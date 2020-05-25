package com.teraim.fieldapp.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.loadermodule.Configuration;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.ModuleLoader;
import com.teraim.fieldapp.loadermodule.ModuleLoader.ModuleLoaderListener;
import com.teraim.fieldapp.loadermodule.configurations.SpinnerConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.VariablesConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.WorkFlowBundleConfiguration;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.log.PlainLogger;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class LoginConsoleFragment extends Fragment implements ModuleLoaderListener {

    private LoggerI loginConsole,debugConsole;
	private PersistenceHelper globalPh,ph;
	private ModuleLoader myLoader=null,myDBLoader=null;
	private String bundleName;
	private Configuration myModules;
	private DbHelper myDb;
	private TextView appTxt;
	private float oldV = -1;
	private final static String InitialBundleName = "Vortex";


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_login_console,
				container, false);
		TextView versionTxt;
		Log.e("vortex","oncreatevieww!");
        TextView log = view.findViewById(R.id.logger);
		versionTxt = view.findViewById(R.id.versionTxt);

		final ImageView logo = view.findViewById(R.id.logo);
		final ImageView bg = view.findViewById(R.id.bgImg);
		appTxt = view.findViewById(R.id.appTxt);

		//Typeface type=Typeface.createFromAsset(getActivity().getAssets(),
		//		"clacon.ttf");
		//log.setTypeface(type);
		log.setMovementMethod(new ScrollingMovementMethod());
		versionTxt.setText("Field Pad ver. "+Constants.VORTEX_VERSION);

		//Create global state


		globalPh = new PersistenceHelper(getActivity().getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE));

		debugConsole = Start.singleton.getLogger();

		//Send a signal that init starts

		//First time vortex runs? Then create global folders.
		if (this.initIfFirstTime()) {
			if (!Connectivity.isConnected(getActivity())) {
				showErrorMsg("You need a network connection first time you start the program. Vortex requires configuration files to run.");
				return view;
			} else {
				this.initialize();
			}
		}
		//First time this application runs? Then create config folder.
		if (!new File(Constants.VORTEX_ROOT_DIR+bundleName).isDirectory()) {
			Log.d("vortex","First time execution!");
			debugConsole.addRow("");
			debugConsole.addPurpleText("First time execution of App "+bundleName);
		} //else
		//	Log.d("vortex","This application has been executed before.");

		//TODO: Move this code into above in next release.
		File folder = new File(Constants.VORTEX_ROOT_DIR+bundleName);
		folder = new File(Constants.VORTEX_ROOT_DIR+bundleName+"/config");
		folder = new File(Constants.VORTEX_ROOT_DIR+bundleName+"/cache");

		//write down version..quickly! :)
		globalPh.put(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM, Constants.VORTEX_VERSION);

		bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
		if (bundleName == null || bundleName.length()==0)
			bundleName = InitialBundleName;

		ph = new PersistenceHelper(getActivity().getApplicationContext().getSharedPreferences(globalPh.get(PersistenceHelper.BUNDLE_NAME), Context.MODE_PRIVATE));
		oldV= ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);

		appTxt.setText(bundleName+" "+(oldV==-1?"":oldV));
		String p_serverURL = globalPh.get(PersistenceHelper.SERVER_URL);
		String checked_URL = Tools.server(p_serverURL);
		if (!checked_URL.equals(p_serverURL))
		    globalPh.put(PersistenceHelper.SERVER_URL,checked_URL);
		String appBaseUrl = checked_URL+bundleName.toLowerCase()+"/";
		final String appRootFolderPath = Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/";
		loginConsole = new PlainLogger(getActivity(),"INITIAL");
		loginConsole.setOutputView(log);

		Tools.onLoadCacheImage(appBaseUrl,"bg_image.jpg", appRootFolderPath+"cache/", new Tools.WebLoaderCb() {
			@Override
			public void loaded(Boolean result) {
				if (result) {
					Bitmap bm = BitmapFactory.decodeFile(appRootFolderPath+"cache/bg_image.jpg", new BitmapFactory.Options());
					if (bm!=null)
						bg.setImageBitmap(bm);
				}
			}

			@Override
			public void progress(int bytesRead) {
			}
		});
		Tools.onLoadCacheImage(appBaseUrl,"logo.png", appRootFolderPath+"cache/", new Tools.WebLoaderCb() {
			@Override
			public void loaded(Boolean result) {
				if (result) {
					Bitmap bm = BitmapFactory.decodeFile(appRootFolderPath+"cache/logo.png", new BitmapFactory.Options());
					if (bm!=null)
						logo.setImageBitmap(bm);
				}
			}
			@Override
			public void progress(int bytesRead) {
			}
		});

		//Tools.preCacheImage(bgUrl,"logo.png",appRootFolderPath+"cache/",loginConsole);

		//new DownloadImageTask((ImageView) view.findViewById(R.id.bgImg))
		//		.execute(bgUrl.toLowerCase());



		//create module descriptors for all known configuration files.
		//Log.d("vortex","Creating Configuration and ModuleLoader");

		//Check if configuration should be loaded from server or from file system.

		if (globalPh.getB("local_config"))
			myModules = new Configuration(Constants.getCurrentlyKnownModules(ConfigurationModule.Source.file,globalPh,ph,null,bundleName,debugConsole));
		else
			myModules = new Configuration(Constants.getCurrentlyKnownModules(ConfigurationModule.Source.internet,globalPh,ph,globalPh.get(PersistenceHelper.SERVER_URL),bundleName,debugConsole));
		String loaderId = "moduleLoader";
		boolean allFrozen = ph.getB(PersistenceHelper.ALL_MODULES_FROZEN+loaderId);
		myLoader = new ModuleLoader(loaderId,myModules,loginConsole,globalPh,allFrozen,debugConsole,this,getActivity());

		if (Constants.FreeVersion && expired())
			showErrorMsg("The license has expired. The App still works, but you will not be able to export any data.");

		return view;
	}





	private boolean expired() {
		long takenIntoUseTime = globalPh.getL(PersistenceHelper.TIME_OF_FIRST_USE);
		long currentTime = System.currentTimeMillis();
		long diff = currentTime - takenIntoUseTime;
		return (diff > Constants.MS_MONTH);

	}





	@Override
	public void onResume() {
		super.onResume();
		Log.e("vortex","onresume!");

		if (GlobalState.getInstance() == null ) {
			if (!myLoader.isActive()) {
				Intent intent = new Intent();
				intent.setAction(MenuActivity.INITSTARTS);
				LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);
				Log.d("vortex", "Loading In Memory Modules");
				myLoader.loadModules(null,false);
				loginConsole.draw();
				Log.d("vortex", "loginConsole object " + loginConsole);
			} else
				Log.e("vortex","Loader is active...discarding onResume");
		}
	}




	@Override
	public void onStop() {
		Log.e("vortex","onstop!");
		if (myLoader!=null)
			myLoader.stop();
		if (myDBLoader!=null)
			myDBLoader.stop();
		super.onStop();
	}


	/******************************
	 * First time? If so, create subfolders.
	 */
	private boolean initIfFirstTime() {
		//If testFile doesnt exist it will be created and found next time.
		Log.d("vortex","Checking if this is first time use of Vortex...");
		boolean first = (globalPh.get(PersistenceHelper.FIRST_TIME_KEY).equals(PersistenceHelper.UNDEFINED));


		if (first) {
			Log.d("vortex","Yes..executing  first time init");
			return true;
		}
		else {
			Log.d("vortex","..Not first time");
			return false;
		}

	}

	private void initialize() {

		//create data folder. This will also create the ROOT folder for the Strand app.
		File folder = new File(Constants.PIC_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create pic root folder");
		folder = new File(Constants.OLD_PIC_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create old pic root folder");
		folder = new File(Constants.EXPORT_FILES_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create export folder");

		//Set defaults if none.
		if (globalPh.get(PersistenceHelper.BUNDLE_NAME).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.BUNDLE_NAME, InitialBundleName);
		if (globalPh.get(PersistenceHelper.VERSION_CONTROL).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.VERSION_CONTROL, "Major");
		if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.SYNC_METHOD, "NONE");
		//if (globalPh.get(PersistenceHelper.USER_ID_KEY).equals(PersistenceHelper.UNDEFINED))
		//	globalPh.put(PersistenceHelper.USER_ID_KEY, "");//getRandomName());
		//if (globalPh.get(PersistenceHelper.LAG_ID_KEY).equals(PersistenceHelper.UNDEFINED))
		//	globalPh.put(PersistenceHelper.LAG_ID_KEY, "");
		if (globalPh.get(PersistenceHelper.LOG_LEVEL).equals(PersistenceHelper.UNDEFINED))
			globalPh.put(PersistenceHelper.LOG_LEVEL, "critical");



		folder = new File(Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/"+Constants.CACHE_ROOT_DIR);
		if(!folder.mkdirs())
			Log.e("NILS","Failed to create gis image folder");

		globalPh.put(PersistenceHelper.FIRST_TIME_KEY,"Initialized");
		long millis = System.currentTimeMillis();
		//date = Constants.getTimeStamp();
		globalPh.put(PersistenceHelper.TIME_OF_FIRST_USE,millis);
	}


	private String getRandomName() {

		List<String> start= Arrays.asList("Anna","Eva","Fiona","Berta");
		List<String> end= Arrays.asList("stina","getrud","lena","eulalia");
		Collections.shuffle(start);
		Collections.shuffle(end);
		return start.get(0)+end.get(0)+"_"+(new Random()).nextInt(500);
	}





	private void showErrorMsg(String error) {
		new AlertDialog.Builder(getActivity())
				.setTitle("Error message")
				.setMessage(error)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setCancelable(false)
				.setNeutralButton("Ok", (dialog, which) -> {

				})
				.show();
	}


	private CharSequence logTxt="";

	@Override
	public void loadSuccess(String loaderId, final boolean majorVersionChange, CharSequence logText,boolean socketBroken) {
		Log.d("vortex","Arrives to loadsucc with ID: "+loaderId);
		ph.put(PersistenceHelper.ALL_MODULES_FROZEN+loaderId,true);
		Log.d("baza","logtxt incoming: "+logText.toString());
		this.logTxt = TextUtils.concat(this.logTxt,logText);
		Log.d("baza","logtxt now "+this.logTxt.toString());
		//If load successful, create database and import data into it. 
		if (loaderId.equals("moduleLoader")) {
			//Create or update database from Table object.
			ConfigurationModule m = myModules.getModule(VariablesConfiguration.NAME);

			if (m!=null) {
				final String _loaderId = "dbLoader";
				boolean allFrozen = ph.getB(PersistenceHelper.ALL_MODULES_FROZEN+_loaderId);
				if (!allFrozen && socketBroken) {
					//alert.
					//break
					loginConsole.clear();
					loginConsole.addRow("Network Error - load aborted");
					loginConsole.draw();
					return;
				}

				Table t = (Table) m.getEssence();
				myDb = new DbHelper(getActivity().getApplicationContext(), t, globalPh, ph, bundleName);
                boolean majorVersionControl = "major".equals(globalPh.get(PersistenceHelper.VERSION_CONTROL));
				if (socketBroken && allFrozen || (majorVersionControl && allFrozen && !majorVersionChange)) {
					//no need to load.
					Log.d("baloo","no need to load...socket broken or no majorchange and allfrozen");
					loadSuccess(_loaderId, majorVersionChange, "\ndb modules unchanged",socketBroken);
				} else {
					//Load configuration files asynchronously.
					Constants.getDBImportModules(globalPh, ph, globalPh.get(PersistenceHelper.SERVER_URL), bundleName, debugConsole, myDb, t, new AsyncLoadDoneCb() {
						public void onLoadSuccesful(List<ConfigurationModule> modules) {
							Configuration dbModules = new Configuration(modules);
							if (modules != null) {
								myDBLoader = new ModuleLoader(_loaderId, dbModules, loginConsole, globalPh, allFrozen, debugConsole, LoginConsoleFragment.this, getActivity());
								LoginConsoleFragment.this.logTxt = TextUtils.concat(LoginConsoleFragment.this.logTxt, "\nDefaults & GIS modules");
								myDBLoader.loadModules(majorVersionChange, socketBroken);
							} else
								Log.e("vortex", "null returned from getDBImportModules");
						}
					});
				}
				//Configuration dbModules = new Configuration(Constants.getDBImportModules(globalPh, ph, server(), bundleName, debugConsole, myDb,t));
				//Import historical data to database. 


			}
		} else {
			//Program is ready to run.
			//Create the global state from all module objects. 
			//Context applicationContext, PersistenceHelper globalPh,
			//PersistenceHelper ph, LoggerI debugConsole, DbHelper myDb,
			//Map<String, Workflow> workflows,Table t,SpinnerDefinition sd


			WorkFlowBundleConfiguration wfC = ((WorkFlowBundleConfiguration)myModules.getModule(bundleName));
			@SuppressWarnings("unchecked") List<Workflow> workflows = (List<Workflow>)wfC.getEssence();
			String imgMetaFormat = wfC.getImageMetaFormat();
			Table t = (Table)(myModules.getModule(VariablesConfiguration.NAME).getEssence());
			SpinnerDefinition sd = (SpinnerDefinition)(myModules.getModule(SpinnerConfiguration.NAME).getEssence());
			if (t==null) {
				Log.e("vortex","table null - load fail");
				return;
			}
			if (getActivity()!=null) {
				final GlobalState gs =
						GlobalState.createInstance(getActivity().getApplicationContext(),globalPh,ph,debugConsole,myDb, workflows, t,sd, this.logTxt,imgMetaFormat);

				//check if backup required.
				if (gs.getBackupManager().timeToBackup()) {
					loginConsole.addRow("Backing up data");
					gs.getBackupManager().backUp();
				}
				if(isAdded()) {
					loginConsole.clear();
					loginConsole.addRow(getString(R.string.done_loading));
					loginConsole.draw();
				}

				start(gs);

			} else {
				Log.e("vortex","No activity.");
			}
		}





	}



	private void start(GlobalState gs) {
		Start.alive = true;
		//Update app version if new
		//if (majorVersionChange) {
		float loadedAppVersion = ph.getF(PersistenceHelper.NEW_APP_VERSION);
		Log.d("antrax","GS VALUE AT START: "+ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP));
		Log.d("vortex", "updating App version to " + loadedAppVersion);
		ph.put(PersistenceHelper.CURRENT_VERSION_OF_APP, loadedAppVersion);
		//				}
		//drawermenu
		gs.setDrawerMenu(Start.singleton.getDrawerMenu());


		//Change to main.
		//execute main workflow if it exists.
		Workflow wf = gs.getWorkflow("Main");
		if (wf == null) {
			String[] x = gs.getWorkflowNames();
			debugConsole.addRow("");
			debugConsole.addRedText("workflow main not found. These are available:");
			for (String n : x)
				debugConsole.addRow(n);
		}
		if (wf != null) {
			Start.singleton.getDrawerMenu().closeDrawer();
			Start.singleton.getDrawerMenu().clear();
			gs.sendEvent(MenuActivity.INITDONE);
			float newV = ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
			if (newV == -1)
				appTxt.setText(bundleName + " [no version]");
			else {
				if (newV > oldV)
					appTxt.setText(bundleName + " --New Version! [" + newV + "]");
				else
					appTxt.setText(bundleName + " " + newV);
			}
			Start.singleton.changePage(wf, null);
			gs.setModules(myModules);

			GlobalState.getInstance().onStart();
		} else {
			if (isAdded()) {
				loginConsole.addRow("");
				loginConsole.addRedText("Found no workflow 'Main'. Exiting..");
			}
		}


	}
private static class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
	final ImageView bmImage;

	public DownloadImageTask(ImageView bmImage) {
		this.bmImage = bmImage;
	}

	protected Bitmap doInBackground(String... urls) {
		String urldisplay = urls[0];
		Bitmap mIcon11 = null;
		try {
			InputStream in = new java.net.URL(urldisplay).openStream();
			mIcon11 = BitmapFactory.decodeStream(in);
		} catch (Exception e) {

		}
		return mIcon11;
	}

	protected void onPostExecute(Bitmap result) {
		Log.d("vortex","setting image!!");
		if (result!=null)
			bmImage.setImageBitmap(result);
	}
}



	@Override
	public void loadFail(String loaderId) {
		Log.d("vortex","loadFail!");
        ph.put(PersistenceHelper.ALL_MODULES_FROZEN+loaderId,false);
		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(MenuActivity.INITFAILED));
	}




}
