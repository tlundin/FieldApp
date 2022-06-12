package com.teraim.fieldapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.ViewConfiguration;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.templates.LinjePortalTemplate;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnActivityResult;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.log.CriticalOnlyLogger;
import com.teraim.fieldapp.log.DummyLogger;
import com.teraim.fieldapp.log.Logger;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.DrawerMenu;
import com.teraim.fieldapp.ui.LoginConsoleFragment;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;


/**
 * @author Terje
 *
 */
public class Start extends MenuActivity {

    public static boolean alive = false;

    //	private Map<String,List<String>> menuStructure;

    private final AsyncTask<GlobalState, Integer, LoadResult> histT=null;
    public static Start singleton;
    private DrawerMenu mDrawerMenu;

    private androidx.appcompat.app.ActionBarDrawerToggle mDrawerToggle;
    private boolean loading = false;



    // Constants
    // The authority for the sync adapter's content provider
    public static final String AUTHORITY = "com.teraim.fieldapp.provider";
    // An account type, in the form of a domain name
    public static final String ACCOUNT_TYPE = "teraim.com";
    // The account name
    public static final String ACCOUNT = "FieldApp";

    public static final long SYNC_INTERVAL = 60;
    // Instance fields
    // Account mAccount;

    private ContentResolver mResolver;




    /**
     * Program entry point
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Setup handler for uncaught exceptions.
/*
		Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
		{
			@Override
			public void uncaughtException (Thread thread, Throwable e)
			{
				Log.e("vortex","Uncaught Exception detected in thread {"+thread+"} Exce: "+ e);
				//e.printStackTrace();
				handleUncaughtException (thread, e);
			}
		});
*/
        Log.d("nils","in START onCreate");
        singleton = this;
        //This is the frame for all pages, defining the Action bar and Navigation menu.
        setContentView(R.layout.naviframe);
        //This combats an issue on the target panasonic platform having to do with http reading.
        //System.setProperty("http.keepAlive", "false");
        mDrawerMenu = new  DrawerMenu(this);
        mDrawerToggle = mDrawerMenu.getDrawerToggle();
        configureToolBar();



        // Create a Sync account
        // mAccount = CreateSyncAccount(this);

        //Determine if program should start or first reload its configuration.
        if (!loading)
            checkStatics();

        try {
            ViewConfiguration config = ViewConfiguration.get(this);
            @SuppressLint("SoonBlockedPrivateApi") Field menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
            if(menuKeyField != null) {
                menuKeyField.setAccessible(true);
                menuKeyField.setBoolean(config, false);
            }
        } catch (Exception ex) {
            // Ignore
        }
        super.onCreate(savedInstanceState);
    }

    private void configureToolBar() {
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        //getActionBar().setHomeButtonEnabled(true);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setHomeAsUpIndicator(R.drawable.ic_round_menu_24px);
        actionbar.setDisplayHomeAsUpEnabled(true);
    }


    private boolean isUIThread(){
        return Looper.getMainLooper().getThread() == Thread.currentThread();
    }

    private void handleUncaughtException(Thread thread, Throwable e) {

        e.printStackTrace(); // not all Android versions will print the stack trace automatically
        if(isUIThread()) {
            invokeLogActivity();
        }else{  //handle non UI thread throw uncaught exception

            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    invokeLogActivity();
                }
            });
        }


    }


    private void invokeLogActivity(){
        Intent intent = new Intent ();
        if (globalPh!=null) {
            intent.putExtra("program_version", Constants.VORTEX_VERSION);
            intent.putExtra("app_name",globalPh.get(PersistenceHelper.BUNDLE_NAME));
            intent.putExtra("user_name",globalPh.get(PersistenceHelper.USER_ID_KEY));
            intent.putExtra("team_name",globalPh.get(PersistenceHelper.LAG_ID_KEY));
        }
        intent.setAction ("com.teraim.fieldapp.SEND_LOG"); // see step 5.
        intent.setFlags (Intent.FLAG_ACTIVITY_NEW_TASK); // required when starting from Application
        Log.d("vortex","Sending log file. Starting SendLog.");
        startActivity (intent);
        System.exit(1); // kill off the crashed app
    }

    @Override
    protected void onResume() {
        Log.d("nils","In START onResume");
        //Check if program is already up.
        if (!loading)
            checkStatics();
        else
            loading = false;


        super.onResume();

    }

    @Override
    protected void onStart() {
        Log.d("nils","In START onStart");
        if(GlobalState.getInstance()!=null)
            GlobalState.getInstance().onStart();
        super.onStart();
    }


    /**
     *
     */
    private final String[] PERMISSIONS = {
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.VIBRATE,
            android.Manifest.permission.READ_SYNC_SETTINGS,
            android.Manifest.permission.WRITE_SYNC_SETTINGS,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
    };
    private final static int PERMISSION_ALL = 1;

    private static String hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return permission;
                }
            }

        }
        return null;
    }

    private void checkStatics() {
        String permission=null;
        //check and ask permissions to load images.
        if ((permission = Start.hasPermissions(this,PERMISSIONS))!=null) {

            // Permission is not granted

            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{permission},
                    PERMISSION_ALL);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.

        } else {
            // Permission has already been granted

            if (GlobalState.getInstance() == null) {
                loading = true;

                //Create a global logger.


                //Start the login fragment.
                android.app.FragmentManager fm = getFragmentManager();
                for (int i = 0; i < fm.getBackStackEntryCount(); ++i) {
                    fm.popBackStack();
                }
                //	private ArrayList<String> rutItems;
                //	private ArrayList<String> wfItems;
                LoginConsoleFragment loginFragment = new LoginConsoleFragment();
                Log.d("vortex", "LoginFragment on stack!");
                fm.beginTransaction()
                        .replace(R.id.content_frame, loginFragment)
                        .commit();

            } else {
                Log.d("vortex", "Globalstate is not null!");

            }
        }
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d("nils","In oncofigChanged");
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
/*
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				mDrawerLayout.openDrawer(GravityCompat.START);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
*/

    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;

        // Handle other action bar items

        return super.onOptionsItemSelected(item);
    }




    //




    @Override
    public void setTitle(CharSequence title) {

        //getSupportActionBar().setTitle(title);
        if (title != null)
        {
            // font color set to primary_light
            getSupportActionBar( ).setTitle( android.text.Html.fromHtml( "<font color='#d7ccc8'>" + title + "</font>" ) );
        }
    }


    private Fragment emptyFragmentToExecute = null ;

    //execute workflow.
    public void changePage(Workflow wf, String statusVar) {
        if (wf==null) {
            debugLogger.addRow("Workflow not defined for button. Check your project XML");
            Log.e("vortex","no wf in changepage");
            return;
        }
        GlobalState gs = GlobalState.getInstance();
        if (gs == null) {
            Log.e("vortex","Global State is null in change pange. App needs to restart");
            Tools.restart(this);
        }

        if (isFinishing()) {
            Log.d("vortex","This activity is finishing! Cannot continue");
            return;
        }

        String label = wf.getLabel();
        String template = wf.getTemplate();
        if (template!=null && template.equals("GisMapTemplate"))
            template = "DefaultTemplate";

        //Set context.
        Log.d("vortex","CHANGING PAGE TO: xxxxxxxx ["+wf.getName()+"]");
        DB_Context cHash = DB_Context.evaluate(wf.getContext());

        //if Ok err is null.
        if (cHash.isOk()) {
            Log.d("hash","setting global context to "+cHash);
            gs.setDBContext(cHash);

            debugLogger.addRow("Context now [");
            debugLogger.addGreenText(cHash.toString());
            debugLogger.addText("]");
            debugLogger.addText("wf context: "+wf.getContext());

            //gs.setRawHash(r.rawHash);
            //gs.setKeyHash(r.keyHash);
            //No template. This flow does not have a ui. Hand over to Executor.
            Fragment fragmentToExecute;
            Bundle args = new Bundle();
            args.putString("workflow_name", wf.getName());
            args.putString("status_variable", statusVar);


            if (template==null) {
                emptyFragmentToExecute = wf.createFragment("EmptyTemplate");
                emptyFragmentToExecute.setArguments(args);
                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();
                //Log.i("vortex", "Adding fragment");
                //ft.add(R.id.lowerContainer, fragment, "AddedFragment");

                ft.add(emptyFragmentToExecute,"EmptyTemplate");
                Log.i("vortex", "Committing Empty transaction");
                ft.commitAllowingStateLoss();
                Log.i("vortex", "Committed transaction");
            } else {
                fragmentToExecute = wf.createFragment(template);
                fragmentToExecute.setArguments(args);
                changePage(fragmentToExecute,label);
            }
            //show error message.
        } else
            showErrorMsg(cHash);
    }
    public void changePage(Fragment newPage, String label) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft
                .replace(R.id.content_frame, newPage)
                .addToBackStack("TROLL")
                .commit();
        setTitle(label);

        //If previous was an empty fragment, clean it
        if (emptyFragmentToExecute!=null) {
            Log.d("blax","removing empty fragment");
            ft.remove(emptyFragmentToExecute);
            emptyFragmentToExecute=null;
        }
        //mDrawerLayout.closeDrawer(mDrawerList);

    }



    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("vortex","IN ONACTIVITY RESULT ");

        Log.d("vortex","request code "+requestCode+" result code "+resultCode);

        Fragment f = getFragmentManager().findFragmentById(R.id.content_frame);
        if (f instanceof Executor)
            ((Executor) f).getCurrentContext().registerEvent(new WF_Event_OnActivityResult("Start", EventType.onActivityResult));

        super.onActivityResult(requestCode, resultCode, data);
    }




    @Override
    public void onDestroy() {
        if (histT!=null) {
            histT.cancel(true);
        }




        if (GlobalState.getInstance()!=null) {

            //kill tracker
            GlobalState.getInstance().getDb().closeDatabaseBeforeExit();

            GlobalState.destroy();
        }



        super.onDestroy();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            Log.d("vortex","gets here key back");

            if (mPopupWindow!=null) {
                Log.d("vortex", "popup is not null");
                if (mPopupWindow.isShowing()) {

                    Log.d("vortex", "closed popup, exiting");
                    mPopupWindow.dismiss();
                    return true;
                }
            }
            Fragment currentContentFrameFragment = getFragmentManager().findFragmentById(R.id.content_frame);
            if (currentContentFrameFragment instanceof Executor) {

                final WF_Context wfCtx = ((Executor) currentContentFrameFragment).getCurrentContext();
                Log.d("vortex", "current context: " + wfCtx.toString());
                boolean map = false;

                if (wfCtx!=null) {
                    if (wfCtx.getCurrentGis()!=null) {
                        map=true;
                        if (wfCtx.getCurrentGis().wasShowingPopup()) {
                            Log.d("vortex","closed popup, exiting");
                            return true;
                        }
                    }
                    Workflow wf = wfCtx.getWorkflow();
                    Log.d("vortex","gets here wf is "+wf);
                    if (wf!=null) {
                        if (!wf.isBackAllowed()) {
                            new AlertDialog.Builder(this).setTitle(R.string.warning)
                                    .setMessage(R.string.warning_exit)
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {

                                            wfCtx.upOneMapLevel();
                                            Log.d("vortex","mapLayer is now "+wfCtx.getMapLayer());
                                            getFragmentManager().popBackStackImmediate();
                                            //GlobalState.getInstance().setCurrentWorkflowContext(null);
                                        }})
                                    .setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {

                                        }})
                                    .setCancelable(false)
                                    .setIcon(android.R.drawable.ic_dialog_alert)
                                    .show();
                        } else {
                            if (map)
                                wfCtx.upOneMapLevel();

                            Log.d("vortex","back was allowed");
                        }
                    }
                }

                if (currentContentFrameFragment instanceof LinjePortalTemplate) {
                    final LinjePortalTemplate lp = (LinjePortalTemplate)getFragmentManager().findFragmentById(R.id.content_frame);
                    if (lp.isRunning()) {
                        new AlertDialog.Builder(this).setTitle("Linjemätning pågår!")
                                .setMessage("Vill du verkligen gå ut från sidan?")
                                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        getFragmentManager().popBackStackImmediate();
                                    }})
                                .setNegativeButton(R.string.no,new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {

                                    }})
                                .setCancelable(false)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                    }
                }
            }
            setTitle("");

        }

        return super.onKeyDown(keyCode, event);
    }

    private void showErrorMsg(DB_Context context) {

        String dialogText = "Faulty or incomplete context\nError: "+context.toString();
        new AlertDialog.Builder(this)
                .setTitle("Context problem")
                .setMessage(dialogText)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setNeutralButton("Ok",new Dialog.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {


                    }
                } )
                .show();

    }




    public DrawerMenu getDrawerMenu() {
        return mDrawerMenu;
    }




    public LoggerI getLogger() {
        Log.d("vortex","getlogger - debuglogger is null? "+(debugLogger==null));
        if (debugLogger==null) {
            String logLevel = globalPh.get(PersistenceHelper.LOG_LEVEL);
            if (logLevel == null || logLevel.equals(PersistenceHelper.UNDEFINED) ||
                    logLevel.equals("normal")) {
                debugLogger = new Logger(this, "DEBUG");
                Log.d("vortex","logger normal");
            }
            else if (logLevel.equals("off")) {
                debugLogger = new DummyLogger();
                Log.d("vortex","logger off");
            }
            else {
                debugLogger = new CriticalOnlyLogger(this);
                Log.d("vortex","logger critical only");
            }

        }
        return debugLogger;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ALL: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.checkStatics();
                } else {
                    Log.e("vortex","Permission denied: "+ Arrays.toString(permissions));

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }










}
