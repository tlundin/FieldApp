//package com.teraim.fieldapp.ui;
//
//import android.app.AlertDialog;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.os.AsyncTask;
//import android.os.Bundle;
//
//import androidx.core.content.ContextCompat;
//import androidx.lifecycle.ViewModelProvider;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;
//import android.text.TextUtils;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import com.teraim.fieldapp.GlobalState;
//import com.teraim.fieldapp.R;
//import com.teraim.fieldapp.Start;
//import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
//import com.teraim.fieldapp.dynamic.types.Table;
//import com.teraim.fieldapp.dynamic.types.Workflow;
//import com.teraim.fieldapp.loadermodule.Configuration;
//import com.teraim.fieldapp.loadermodule.ConfigurationModule;
//import com.teraim.fieldapp.loadermodule.GisDatabaseWorkflow;
//import com.teraim.fieldapp.loadermodule.ModuleLoader;
//import com.teraim.fieldapp.loadermodule.ModuleLoader.ModuleLoaderListener;
//import com.teraim.fieldapp.loadermodule.Workflow_I;
//import com.teraim.fieldapp.loadermodule.ModuleRegistry;
//import com.teraim.fieldapp.loadermodule.configurations.SpinnerConfiguration;
//import com.teraim.fieldapp.loadermodule.configurations.VariablesConfiguration;
//import com.teraim.fieldapp.loadermodule.configurations.WorkFlowBundleConfiguration;
//import com.teraim.fieldapp.log.LoggerI;
//import com.teraim.fieldapp.non_generics.Constants;
//import com.teraim.fieldapp.utils.Connectivity;
//import com.teraim.fieldapp.utils.DbHelper;
//import com.teraim.fieldapp.utils.PersistenceHelper;
//import com.teraim.fieldapp.utils.Tools;
//
//import java.io.File;
//import java.io.InputStream;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//import java.util.Locale;
//import java.util.Random;
//
//
//public class LoginConsoleFragment extends Fragment implements ModuleLoaderListener {
//
//    private LoggerI loginConsole,debugConsole;
//	private PersistenceHelper globalPh,ph;
//	private ModuleLoader myLoader=null,myDBLoader=null;
//	private String bundleName;
//	private DbHelper myDb;
//	private TextView appTxt;
//	private float oldV = -1;
//	private final static String InitialBundleName = Constants.DEFAULT_APP;
//	private ModuleLoaderViewModel viewModel;
//
//	private ModuleRegistry moduleRegistry;
//
//	private boolean loadGisModules=true;
//
//	@Override
//	public View onCreateView(LayoutInflater inflater, ViewGroup container,
//							 Bundle savedInstanceState) {
//		View view = inflater.inflate(R.layout.fragment_login_console,
//				container, false);
//		TextView versionTxt;
//		Log.e("vortex","OnCreateView!");
//        TextView log = view.findViewById(R.id.logger);
//		versionTxt = view.findViewById(R.id.versionTxt);
//		appTxt = view.findViewById(R.id.appTxt);
//		moduleRegistry = new ModuleRegistry();
//
//		//Typeface type=Typeface.createFromAsset(getActivity().getAssets(),
//		//		"clacon.ttf");
//		//log.setTypeface(type);
//		//log.setMovementMethod(new ScrollingMovementMethod());
//		versionTxt.setText("Field Pad version "+Constants.VORTEX_VERSION);
//
//		//Create global state
//
//
//		globalPh = new PersistenceHelper(getActivity().getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE));
//
//		debugConsole = Start.singleton.getLogger();
//
//		//Send a signal that init starts
//		//First time this application runs? Then create config folder.
//		if (this.initIfFirstTime()) {
//			if (!Connectivity.isConnected(getActivity())) {
//				showErrorMsg("You need a network connection first time you start the program. Vortex requires configuration files to run.");
//				return view;
//			} else {
//				this.initialize();
//			}
//		}
//		bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
//		if (bundleName == null || bundleName.length()==0)
//			bundleName = InitialBundleName;
//
//		new File(getContext().getFilesDir()+"/"+bundleName.toLowerCase(Locale.ROOT));
//		new File(getContext().getFilesDir()+"/"+bundleName.toLowerCase(Locale.ROOT)+"/config");
//		new File(getContext().getFilesDir()+"/"+bundleName.toLowerCase(Locale.ROOT)+"/cache");
//		globalPh.put(PersistenceHelper.CURRENT_VERSION_OF_PROGRAM, Constants.VORTEX_VERSION);
//		ph = new PersistenceHelper(getActivity().getApplicationContext().getSharedPreferences(globalPh.get(PersistenceHelper.BUNDLE_NAME), Context.MODE_PRIVATE));
//		oldV= ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
//		appTxt.setText(bundleName+" "+(oldV==-1?"":oldV));
//		String p_serverURL = globalPh.get(PersistenceHelper.SERVER_URL);
//		String checked_URL = Tools.server(p_serverURL);
//		if (!checked_URL.equals(p_serverURL))
//		    globalPh.put(PersistenceHelper.SERVER_URL,checked_URL);
//		String appBaseUrl = checked_URL+bundleName.toLowerCase(Locale.ROOT)+"/";
//		final String appRootFolderPath = getContext().getFilesDir()+"/"+bundleName.toLowerCase(Locale.ROOT)+"/";
//		//loginConsole = new PlainLogger(getActivity(),"INITIAL");
//		//loginConsole.setOutputView(log);
//		//String server = globalPh.get(PersistenceHelper.SERVER_URL);
//		//myModules = new Configuration(Constants.getCurrentlyKnownModules(getContext(),globalPh,ph,server,bundleName,debugConsole));
//		//If all modules are already in memory, thaw them by default. Also create a button to allow the user to load new configuration.
//		//myLoader = new ModuleLoader("moduleLoader", myModules, loginConsole, globalPh, ph.getB(PersistenceHelper.ALL_MODULES_FROZEN + "moduleLoader"), debugConsole, LoginConsoleFragment.this, getActivity());
//
//		viewModel = new ViewModelProvider(requireActivity()).get(ModuleLoaderViewModel.class);
//		return view;
//	}
//
//
//
//	@Override
//	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//		super.onViewCreated(view, savedInstanceState);
//
//		// --- Observer for the final completion status of the entire plan ---
//		// This reacts only when the whole plan is loading, succeeds, or fails.
//		viewModel.finalProcessStatus.observe(getViewLifecycleOwner(), status -> {
//			if (status == null) return;
//			// Use a switch to react to the final state
//			switch (status) {
//				case LOADING:
//					// The process has started, disable buttons and show a progress indicator.
//					// e.g., progressBar.setVisibility(View.VISIBLE);
//					break;
//				case SUCCESS:
//					// The entire plan is done and was successful!
//					Toast.makeText(getContext(), "All modules loaded successfully!", Toast.LENGTH_SHORT).show();
//					Log.d(TAG,"load successful");
//					start();
//					break;
//
//				case FAILURE:
//					// The plan is done but at least one job failed.
//					// Re-enable UI so the user can try again.
//					Log.d(TAG,"load failed");
//					Toast.makeText(getContext(), "An error occurred during loading. Please try again.", Toast.LENGTH_LONG).show();
//					// e.g., showErrorDialog();
//					break;
//			}
//		});
//	}
//
//
//
//	@Override
//	public void onResume() {
//		super.onResume();
//		Log.e("gipp","loginfragment onresume!");
//
//		if (GlobalState.getInstance() == null ) {
//			Intent intent = new Intent();
//			intent.setAction(MenuActivity.INITSTARTS);
//			LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);
//			startLoadingProcess();
//			Log.d(TAG, "Loading In Memory Modules");
//		}
//	}
//
//	@Override
//	public void onStop() {
//		Log.e("vortex","onstop!");
//		if (myLoader!=null)
//			myLoader.stop();
//		if (myDBLoader!=null)
//			myDBLoader.stop();
//		super.onStop();
//	}
//
//
//	/******************************
//	 * First time? If so, create subfolders.
//	 */
//	private boolean initIfFirstTime() {
//		globalPh.put(PersistenceHelper.SYNC_METHOD, "NONE");
//		//If testFile doesnt exist it will be created and found next time.
//		Log.d(TAG,"Checking if this is first time use of Vortex...");
//		boolean first = (globalPh.get(PersistenceHelper.FIRST_TIME_KEY).equals(PersistenceHelper.UNDEFINED));
//		if (first) {
//			Log.d(TAG,"Yes..executing  first time init");
//			debugConsole.addRow("");
//			debugConsole.addPurpleText("First time execution");
//			return true;
//		}
//		else {
//			Log.d(TAG,"..Not first time");
//			return false;
//		}
//
//	}
//
//	private void initialize() {
//		File[] externalStorageVolumes =
//				ContextCompat.getExternalFilesDirs(getContext(), null);
//		File primaryExternalStorage = externalStorageVolumes[0];
//		//create data folder. This will also create the ROOT folder for the Strand app.
//		File folder = new File(primaryExternalStorage.getAbsolutePath() + "/pics/");
//		if(!folder.mkdirs())
//			Log.e("NILS","Failed to create pic root folder");
//		folder = new File(primaryExternalStorage.getAbsolutePath() + "/old_pics/");
//		if(!folder.mkdirs())
//			Log.e("NILS","Failed to create old pic root folder");
//		folder = new File(primaryExternalStorage.getAbsolutePath() + "/export/");
//		if(!folder.mkdirs())
//			Log.e("NILS","Failed to create export folder");
//
//		//Set defaults if none.
//		if (globalPh.get(PersistenceHelper.BUNDLE_NAME).equals(PersistenceHelper.UNDEFINED))
//			globalPh.put(PersistenceHelper.BUNDLE_NAME, InitialBundleName);
//		if (globalPh.get(PersistenceHelper.VERSION_CONTROL).equals(PersistenceHelper.UNDEFINED))
//			globalPh.put(PersistenceHelper.VERSION_CONTROL, "Major");
//		if (globalPh.get(PersistenceHelper.SYNC_METHOD).equals(PersistenceHelper.UNDEFINED))
//			globalPh.put(PersistenceHelper.SYNC_METHOD, "NONE");
//		if (globalPh.get(PersistenceHelper.LOG_LEVEL).equals(PersistenceHelper.UNDEFINED))
//			globalPh.put(PersistenceHelper.LOG_LEVEL, "critical");
//		if (globalPh.get(PersistenceHelper.SERVER_URL).equals(PersistenceHelper.UNDEFINED))
//			globalPh.put(PersistenceHelper.SERVER_URL, Constants.DEFAULT_SERVER_URI);
//		if (globalPh.get(PersistenceHelper.EXPORT_SERVER_URL).equals(PersistenceHelper.UNDEFINED))
//			globalPh.put(PersistenceHelper.EXPORT_SERVER_URL, Constants.DEFAULT_EXPORT_SERVER);
//
//		folder = new File(getContext().getFilesDir()+"/"+globalPh.get(PersistenceHelper.BUNDLE_NAME).toLowerCase(Locale.ROOT)+"/cache/");
//		if(!folder.mkdirs())
//			Log.e("NILS","Failed to create cache folder");
//		globalPh.put(PersistenceHelper.FIRST_TIME_KEY,"Initialized");
//		long millis = System.currentTimeMillis();
//		//date = Constants.getTimeStamp();
//		globalPh.put(PersistenceHelper.TIME_OF_FIRST_USE,millis);
//	}
//
//
//	private String getRandomName() {
//
//		List<String> start= Arrays.asList("Anna","Eva","Fiona","Berta");
//		List<String> end= Arrays.asList("stina","getrud","lena","eulalia");
//		Collections.shuffle(start);
//		Collections.shuffle(end);
//		return start.get(0)+end.get(0)+"_"+(new Random()).nextInt(500);
//	}
//
//	void startLoadingProcess() {
//		// Create the specific workflow needed for this screen.
//		Workflow_I myWorkflow = new GisDatabaseWorkflow(getContext(), globalPh, ph);
//
//		// Tell the ViewModel to execute it. The fragment doesn't need to know the details.
//		viewModel.execute(myWorkflow,moduleRegistry,false);
//	}
//
//
//
//	private void showErrorMsg(String error) {
//		new AlertDialog.Builder(getActivity())
//				.setTitle("Error message")
//				.setMessage(error)
//				.setIcon(android.R.drawable.ic_dialog_alert)
//				.setCancelable(false)
//				.setNeutralButton("Ok", (dialog, which) -> {
//
//				})
//				.show();
//	}
//
//
//	private CharSequence logTxt="";
//
//	@Override
//	public void loadSuccess(String loaderId, final boolean majorVersionChange, CharSequence logText,boolean socketBroken) {
//		Log.d(TAG,"Arrives to loadsucc with ID: "+loaderId);
//		ph.put(PersistenceHelper.ALL_MODULES_FROZEN+loaderId,true);
//		Log.d(TAG,"logtxt incoming: "+logText.toString());
//		this.logTxt = logText; // TextUtils.concat(this.logTxt,logText);
//		Log.d(TAG,"logtxt now "+this.logTxt.toString());
//		//If load successful, create database and import data into it.
//		if (loaderId.equals("moduleLoader")) {
//			//Create or update database from Table object.
//			ConfigurationModule m = moduleRegistry.getModule(VariablesConfiguration.NAME);
//
//			if (m!=null) {
//				final String _loaderId = "dbLoader";
//				boolean allFrozen = ph.getB(PersistenceHelper.ALL_MODULES_FROZEN+_loaderId);
//				if (!allFrozen && socketBroken) {
//					//alert.
//					//break
//					loginConsole.clear();
//					loginConsole.addRow("Network Error - load aborted");
//					loginConsole.draw();
//					return;
//				}
//
//				Table t = (Table) m.getEssence();
//				myDb = new DbHelper(getActivity().getApplicationContext(), t, globalPh, ph, bundleName);
//                boolean majorVersionControl = "Major".equals(globalPh.get(PersistenceHelper.VERSION_CONTROL));
//				if (socketBroken && allFrozen || (majorVersionControl && allFrozen && !majorVersionChange)) {
//					//no need to load.
//					Log.d(TAG,"no need to load...socket broken or no majorchange and allfrozen");
//					loadSuccess(_loaderId, majorVersionChange, "\ndb modules unchanged",socketBroken);
//				} else {
//					//Load configuration files asynchronously.
//					Constants.getDBImportModules(getContext(), globalPh, ph, globalPh.get(PersistenceHelper.SERVER_URL), bundleName, debugConsole, myDb, t, new AsyncLoadDoneCb() {
//						public void onLoadSuccesful(List<ConfigurationModule> modules) {
//							Configuration dbModules = new Configuration(modules);
//							if (modules != null) {
//								myDBLoader = new ModuleLoader(_loaderId, dbModules, loginConsole, globalPh, allFrozen, debugConsole, LoginConsoleFragment.this, getActivity());
//								LoginConsoleFragment.this.logTxt = TextUtils.concat(LoginConsoleFragment.this.logTxt, "\nDefaults & GIS modules");
//								myDBLoader.loadModules(majorVersionChange, socketBroken);
//							} else
//								Log.e("vortex", "null returned from getDBImportModules");
//						}
//					});
//
//
//				}
//				//Configuration dbModules = new Configuration(Constants.getDBImportModules(globalPh, ph, server(), bundleName, debugConsole, myDb,t));
//				//Import historical data to database.
//
//
//			}
//		} else {
//			//Program is ready to run.
//			//Create the global state from all module objects.
//			//Context applicationContext, PersistenceHelper globalPh,
//			//PersistenceHelper ph, LoggerI debugConsole, DbHelper myDb,
//			//Map<String, Workflow> workflows,Table t,SpinnerDefinition sd
//
//
//			WorkFlowBundleConfiguration wfC = ((WorkFlowBundleConfiguration)moduleRegistry.getModule(bundleName));
//			@SuppressWarnings("unchecked") List<Workflow> workflows = (List<Workflow>)wfC.getEssence();
//			String imgMetaFormat = wfC.getImageMetaFormat();
//			Table t = (Table)(moduleRegistry.getModule(VariablesConfiguration.NAME).getEssence());
//			SpinnerDefinition sd = (SpinnerDefinition)(moduleRegistry.getModule(SpinnerConfiguration.NAME).getEssence());
//			if (t==null) {
//				Log.e("vortex","table null - load fail");
//				return;
//			}
//			if (getActivity()!=null) {
//				final GlobalState gs =
//						GlobalState.createInstance(getActivity().getApplicationContext(),globalPh,ph,debugConsole,myDb, workflows, t,sd, this.logTxt,imgMetaFormat);
//				//SharedPreferences sp = GlobalState.getInstance().getPreferences().getPreferences();
//				//sp.edit().putStringSet(PersistenceHelper.EXPORTED_IMAGES_KEY, new HashSet<>()).commit();
//				//check if backup required.
//				if (gs.getBackupManager().timeToBackup()) {
//					loginConsole.addRow("Backing up data");
//					gs.getBackupManager().backUp();
//				}
//				if(isAdded()) {
//					loginConsole.clear();
//					loginConsole.addRow(getString(R.string.done_loading));
//					loginConsole.draw();
//				}
//				//Log.d(TAG,debugConsole.getLogText().toString());
//				start();
//
//			} else {
//				Log.e("vortex","No activity.");
//			}
//		}
//
//
//
//
//
//	}
//
//
//
//	private void start() {
//		WorkFlowBundleConfiguration wfC = ((WorkFlowBundleConfiguration)moduleRegistry.getModule(bundleName));
//		@SuppressWarnings("unchecked") List<Workflow> workflows = (List<Workflow>)wfC.getEssence();
//		String imgMetaFormat = wfC.getImageMetaFormat();
//		Table t = (Table)(moduleRegistry.getModule(VariablesConfiguration.NAME).getEssence());
//		myDb = new DbHelper(getActivity().getApplicationContext(), t, globalPh, ph, bundleName);
//		SpinnerDefinition sd = (SpinnerDefinition)(moduleRegistry.getModule(SpinnerConfiguration.NAME).getEssence());
//		if (t==null) {
//			Log.e("vortex","table null - load fail");
//			return;
//		}
//			final GlobalState gs =
//					GlobalState.createInstance(getActivity().getApplicationContext(),globalPh,ph,debugConsole,myDb, workflows, t,sd, this.logTxt,imgMetaFormat);
//			//SharedPreferences sp = GlobalState.getInstance().getPreferences().getPreferences();
//			//sp.edit().putStringSet(PersistenceHelper.EXPORTED_IMAGES_KEY, new HashSet<>()).commit();
//			//check if backup required.
//			if (gs.getBackupManager().timeToBackup()) {
//				//loginConsole.addRow("Backing up data");
//				gs.getBackupManager().backUp();
//			}
////			if(isAdded()) {
////				loginConsole.clear();
////				loginConsole.addRow(getString(R.string.done_loading));
////				loginConsole.draw();
////			}
//			//Log.d(TAG,debugConsole.getLogText().toString());
//
//		Start.alive = true;
//		//Update app version if new
//		//if (majorVersionChange) {
//		float loadedAppVersion = ph.getF(PersistenceHelper.NEW_APP_VERSION);
//		Log.d(TAG,"GS VALUE AT START: "+ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP));
//		Log.d(TAG, "updating App version to " + loadedAppVersion);
//		ph.put(PersistenceHelper.CURRENT_VERSION_OF_APP, loadedAppVersion);
//		//				}
//		//drawermenu
//		gs.setDrawerMenu(Start.singleton.getDrawerMenu());
//
//
//		//Change to main.
//		//execute main workflow if it exists.
//		Workflow wf = gs.getWorkflow("Main");
//		if (wf == null) {
//			String[] x = gs.getWorkflowNames();
//			debugConsole.addRow("");
//			debugConsole.addRedText("workflow main not found. These are available:");
//			for (String n : x)
//				debugConsole.addRow(n);
//		}
//		if (wf != null) {
//			Start.singleton.getDrawerMenu().closeDrawer();
//			Start.singleton.getDrawerMenu().clear();
//			gs.sendEvent(MenuActivity.INITDONE);
//			float newV = ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);
//			if (newV == -1)
//				appTxt.setText(bundleName + " [no version]");
//			else {
//				if (newV > oldV)
//					appTxt.setText(bundleName + " --New Version! [" + newV + "]");
//				else
//					appTxt.setText(bundleName + " " + newV);
//			}
//			Start.singleton.changePage(wf, null);
//			GlobalState.getInstance().onStart();
//		} else {
//			if (isAdded()) {
//				Log.d(TAG,"Main workflow not found.");
//				//loginConsole.addRow("");
//				//loginConsole.addRedText("Found no workflow 'Main'. Exiting..");
//			}
//		}
//		myLoader=null;
//	}
//
//
//
//
//	@Override
//	public void loadFail(String loaderId) {
//		Log.d(TAG,"loadFail!");
//        ph.put(PersistenceHelper.ALL_MODULES_FROZEN+loaderId,false);
//		LocalBroadcastManager.getInstance(getActivity()).sendBroadcast(new Intent(MenuActivity.INITFAILED));
//	}
//
//
//
//
//}
