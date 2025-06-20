package com.teraim.fieldapp.loadermodule;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;
import java.util.concurrent.ExecutorService;


//Class that describes the specific load behaviour for a certain type of input data.
public abstract class ConfigurationModule {


	private final Context context;
	public boolean tryingThawAfterFail,tryingWebAfterFail;
    public enum Type {
		json,
		xml,
		csv,
		ini,
		jgw,
		txt
	}
	public enum State {
		INITIAL,
		LOADING,
		FREEZING,
		FROZEN,
		THAWING,
		THAWED,
		NOT_FOUND,
		NO_DATA, ERROR
	}
	public final MutableLiveData<State> state = new MutableLiveData<>(State.INITIAL);


	public final Type type;
	public final String fileName;
	private String rawData;
    private final String printedLabel;
    private  String frozenPath;
	protected float newVersion;
	protected final PersistenceHelper globalPh;
    protected final PersistenceHelper ph;
	private boolean IamLoaded=false;
	protected String versionControl;

	private Integer linesOfRawData;
	protected Object essence;
	protected final String baseBundlePath;
	public boolean isBundle = false;
	private boolean notFound=false;
	private final String fullPath;
	//freezeSteps contains the number of steps required to freeze the object. Should be -1 if not set specifically by specialized classes.
	protected int freezeSteps=-1;
	//tells if this module is stored on disk or db.
	protected boolean isDatabaseModule = false,hasSimpleVersion=true;

	protected ConfigurationModule(Context context, PersistenceHelper gPh, PersistenceHelper ph, Type type, String urlOrPath, String fileName, String moduleName) {
		this.type=type;
		this.fileName=fileName;
		this.globalPh=gPh;
		this.ph=ph;
		this.printedLabel=moduleName;
		this.baseBundlePath=urlOrPath;
		this.fullPath = urlOrPath+fileName+"."+type.name();
		this.context = context;
		this.frozenPath = context.getFilesDir()+"/"+globalPh.get(PersistenceHelper.BUNDLE_NAME).toLowerCase(Locale.ROOT)+"/cache/"+fileName;
		Log.d("balla","full path "+fullPath);
		this.versionControl = globalPh.get(PersistenceHelper.VERSION_CONTROL);
	}


	public boolean frozenFileExists() {
		return new File(frozenPath).isFile() && (getFrozenVersion()!=-1);
	}

	public abstract float getFrozenVersion();

	public String getFullPath() {
		return fullPath;
	}

	public String getURL() {
		return fullPath;
	}
	//Stores version number. Can be different from frozen version during load.
	public void setNewVersion(float version) {
		this.newVersion=version;
		Log.d("vortex","version set to "+version);
	}

	//Freeze version number when load succesful
	public void setLoaded(boolean loadStatus) {
		IamLoaded=loadStatus;
		setThawActive(false);
	}


	private boolean isThawing = false;

	public void setThawActive(boolean t) {
		isThawing=t;
	}
	public boolean isThawing() {
		return isThawing;
	}

	public void setNotFound() {
		isThawing=false;
		notFound=true;
	}

	protected abstract void setFrozenVersion(float version);

	public abstract boolean isRequired();

	public boolean isLoaded() {
		// :)
		return IamLoaded||notFound;
	}
	
	public boolean isMissing() {
		return notFound;
	}

	public void load(ExecutorService executor, ModuleLoaderCb cb, boolean forceReload) {
		state.postValue(State.LOADING);

		executor.submit(() -> {
			// --- This entire block runs on a background thread ---

			// 1. Check if we should skip the download
			if (!forceReload) {
				// Try to load from the local frozen cache first.
				if (thawSynchronously().errCode == ErrorCode.thawed) {
					Log.d("ModuleLoader", "Module [" + getLabel() + "] successfully thawed from cache. Skipping network.");
					// Signal completion using the existing callback.
					cb.onFileLoaded(new LoadResult(this, ErrorCode.thawed));
					// Stop execution here. Do not proceed to network load.
					return;
				}
			}

			// 2. If we are here, it means either:
			//    a) A force reload was requested.
			//    b) There was no frozen file to thaw.
			//    c) The frozen file was corrupt and failed to thaw.
			//    Proceed with the network load.
			Log.d("ModuleLoader", "Module [" + getLabel() + "] will be fetched from network. ForceReload=" + forceReload);
			LoadResult networkResult = DataLoader.loadAndParseAndFreeze(this);

			if (networkResult.errCode == ErrorCode.frozen) {
				cb.onFileLoaded(networkResult);
			} else {
				cb.onError(networkResult);
			}
		});
	}
	public void load(ExecutorService executor, ModuleLoaderCb cb) {
		state.postValue(State.LOADING);
		executor.submit(() -> {

			try {
				versionControl = globalPh.get(PersistenceHelper.VERSION_CONTROL);
				LoadResult result = DataLoader.loadAndParseAndFreeze(this);
				cb.onFileLoaded(result);

			} catch (Exception e) {
				state.postValue(State.ERROR);
				cb.onError(new LoadResult(this,LoadResult.ErrorCode.IOError,e.getMessage()));
			}
		});
	}

	public String getRawData() {
		return rawData;
	}
	public void setRawData(String data, Integer tot) {
		rawData = data;
		this.linesOfRawData = tot;
	}

	protected Integer getNumberOfLinesInRawData() {
		// TODO Auto-generated method stub
		return linesOfRawData;
	}


	public String getFileName() {
		return fileName;
	}

	public String getLabel() {
		return printedLabel;
	}

	//Freeze this configuration. counter is used by some dependants.
	public void freeze(int counter) {
		Log.d("ConfigurationModule","freeze called");
		setEssence();
		if (essence != null) {
			state.postValue(State.FREEZING);
			try {
				Tools.witeObjectToFile(essence, frozenPath);
			} catch (IOException e) {

				GlobalState gs = GlobalState.getInstance();
				if (gs != null) {
					gs.getLogger().addRow("");
					StringWriter sw = new StringWriter();
					PrintWriter pw = new PrintWriter(sw);
					e.printStackTrace(pw);
					gs.getLogger().addRedText(sw.toString());
				}
				e.printStackTrace();
			}
		}
	}





	public LoadResult thawSynchronously() {

		//A database module is by default saved already.
		if (isDatabaseModule)
			return new LoadResult(this,ErrorCode.thawed);
		else {
			this.setThawActive(true);
			Log.d("ConfigurationModule", "Trying to thaw using "+this.frozenPath);
			Object result = Tools.readObjectFromFile(this.frozenPath);
			this.setThawActive(false);
			setEssence(result);
			return new LoadResult(this,result == null?ErrorCode.thawFailed:ErrorCode.thawed);
		}

	}

	public boolean thaw(ModuleLoader caller) {

		//A database module is by default saved already.
		if (isDatabaseModule)
			return true;
        else {
			//Unthaw asynchronously
			this.setThawActive(true);
			Tools.readObjectFromFileAsync(this.frozenPath, this, caller);
			return false;
		}

	}



	public Object getEssence() {
		return essence;
	}

	//Must set essence before freeze.
	protected abstract void setEssence();

    //If thawed, set essence from file.
    public void setEssence(Object result) {
        this.essence=result;
    }


	public void deleteFrozen() {
		new File(this.frozenPath).delete();
	}








}
