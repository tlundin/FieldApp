package com.teraim.fieldapp.loadermodule;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Locale;
import java.util.concurrent.ExecutorService;


/**
 * A corrected base class for Configuration Modules.
 * This version uses lazy initialization to prevent blocking the main UI thread and
 * ensures the loading process correctly handles all outcomes.
 */
public abstract class ConfigurationModule {

	// Enums
	public enum FileFormat { json, xml, csv, ini, jgw, txt }
	public enum ModuleLoadState { INITIAL, LOADING, FROZEN, ERROR }

	// LiveData for state tracking, initialized with a default value to be thread-safe.
	public final MutableLiveData<ModuleLoadState> state = new MutableLiveData<>(ModuleLoadState.INITIAL);

	// Module properties
	public final FileFormat fileFormat;
	public final String fileName;
	public final String printedLabel;
	private final String fullPath;
	protected final String baseBundlePath;

	// Lazily initialized fields to prevent I/O in the constructor
	protected String frozenPath;

	protected float newVersion;
	protected Object essence;
	protected int freezeSteps = -1;
	protected boolean isDatabaseModule = false;
	protected boolean hasSimpleVersion = true;
	private boolean IamLoaded = false;
	private boolean isThawing = false;
	private boolean notFound = false;
	private String rawData;
	private Integer linesOfRawData;

	protected boolean isBundle = false;
	// Helpers and context
	private final Context context;
	protected final PersistenceHelper globalPh;
	protected final PersistenceHelper ph;


	protected ConfigurationModule(Context context, PersistenceHelper gPh, PersistenceHelper ph, FileFormat fileFormat, String urlOrPath, String fileName, String moduleName) {
		// --- LEAN CONSTRUCTOR ---
		// The constructor should only assign values and perform no I/O.
		this.context = context;
		this.globalPh = gPh;
		this.ph = ph;
		this.fileFormat = fileFormat;
		this.fileName = fileName;
		this.printedLabel = moduleName;
		this.baseBundlePath = urlOrPath;
		this.fullPath = urlOrPath + fileName + "." + fileFormat.name();
		initLazyFields();
	}

	/**
	 * The single, robust loading method. It handles both cache and network paths.
	 * @param executor The executor service to run tasks on.
	 * @param cb The callback to report completion or failure.
	 * @param forceReload If true, the cache check is skipped.
	 */
	public void load(ExecutorService executor, final ModuleLoaderCb cb, boolean forceReload) {
		state.postValue(ModuleLoadState.LOADING);

		executor.submit(() -> {
			// 1. Check for a cached version first, unless forced to reload.
			if (!forceReload) {
				if (thawSynchronously().errCode == ErrorCode.thawed) {
					Log.d("ConfigModule", "Module [" + getLabel() + "] successfully thawed from cache. Skipping network.");
					// FIX: Always invoke the callback on success to notify the loader.
					cb.onFileLoaded(new LoadResult(this, ErrorCode.thawed));
					return; // Stop execution here.
				}
			}
			// 2. If no valid cache, or if forced, proceed with network download.
			Log.d("ConfigModule", "Module [" + getLabel() + "] will be fetched from network. ForceReload=" + forceReload);
			// Assuming DataLoader performs the synchronous network request.
			LoadResult networkResult = DataLoader.loadAndParseAndFreeze(this);
			if (networkResult.errCode == ErrorCode.frozen ) {
				cb.onFileLoaded(networkResult);
			} else {
				cb.onError(networkResult);
			}
		});
	}

	/**
	 * Initializes fields that require I/O, ensuring it happens on a background thread.
	 */
	private void initLazyFields() {
		if (frozenPath == null) {
			String bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
			frozenPath = context.getFilesDir() + "/" + bundleName.toLowerCase(Locale.ROOT) + "/cache/" + fileName;
		}
	}

	public LoadResult thawSynchronously() {
		Type essenceType = getEssenceType();
		Log.d("ConfigModule", "getEssenceType() returned " + essenceType + " for " + this.getClass().getSimpleName() + "");
		if (essenceType == null) {
			Log.e("ConfigModule", "getEssenceType() returned null for " + this.getClass().getSimpleName());
			return new LoadResult(this, ErrorCode.thawFailed);
		}

		Object result = Tools.readObjectFromFileAsJson(this.frozenPath, essenceType);

		if (result != null) {
			setEssence(result);
			return new LoadResult(this, ErrorCode.thawed);
		} else {
			setFrozenVersion(-1);
			return new LoadResult(this, ErrorCode.thawFailed);
		}
	}

	public void freeze(int counter) {
		setEssence();
		if (essence != null) {
			Tools.writeObjectToFileAsJson(essence, frozenPath);
        }
	}

	// Abstract methods to be implemented by subclasses
	protected abstract Type getEssenceType();
	public abstract float getFrozenVersion();
	protected abstract void setFrozenVersion(float version);
	protected abstract void setEssence();
	public abstract boolean isRequired();

	// Getters and Setters
	public String getURL() { return fullPath; }
	public String getLabel() { return printedLabel; }
	public void setNewVersion(float version) { this.newVersion = version; }
	public void setEssence(Object result) { this.essence = result; }
	public Object getEssence() { return essence; }
	public boolean frozenFileExists() {
		initLazyFields();
		return new File(frozenPath).isFile() && (getFrozenVersion()!=-1);
	}
	public String getFileName() { return fileName; }
	public String getRawData() { return rawData; }
	public void setRawData(String data, Integer tot) {
		rawData = data;
		this.linesOfRawData = tot;
	}
	protected Integer getNumberOfLinesInRawData() { return linesOfRawData; }
	public boolean isLoaded() { return IamLoaded||notFound; }
	public void setLoaded(boolean loadStatus) {
		IamLoaded=loadStatus;
		setThawActive(false);
	}
	public boolean isThawing() { return isThawing; }
	public void setThawActive(boolean t) { isThawing=t; }
	public boolean isMissing() { return notFound; }
	public void setNotFound() {
		isThawing=false;
		notFound=true;
	}
	public void deleteFrozen() {
		new File(this.frozenPath).delete();
	}
}
