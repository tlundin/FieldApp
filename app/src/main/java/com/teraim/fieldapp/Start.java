package com.teraim.fieldapp;

import static com.teraim.fieldapp.loadermodule.LoadingStatus.FAILURE;
import static com.teraim.fieldapp.loadermodule.LoadingStatus.LOADING;
import static com.teraim.fieldapp.loadermodule.LoadingStatus.SUCCESS;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import androidx.fragment.app.Fragment;
import android.app.FragmentManager;
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
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.templates.StartupFragment;
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
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.ui.ModuleLoaderViewModel;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

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

    private ModuleLoaderViewModel viewModel;
    private FrameLayout progressIndicatorContainer;
    private TextView progressIndicatorText;


    /**
     * Program entry point
     *
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // <<-- MOVE THIS TO THE TOP

        // Setup handler for uncaught exceptions.
/*        Thread.setDefaultUncaughtExceptionHandler (new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException (Thread thread, Throwable e)
            {
                Log.e("vortex","Uncaught Exception detected in thread {"+thread+"} Exce: "+ e);
                handleUncaughtException (thread, e);
            }
        });*/

        Log.d("nils","in START onCreate");

        singleton = this;

        //This is the frame for all pages, defining the Action bar and Navigation menu.
        setContentView(R.layout.naviframe);

        // 1. Get references to the ViewModel and the new UI elements
        viewModel = new ViewModelProvider(this).get(ModuleLoaderViewModel.class);
        progressIndicatorContainer = findViewById(R.id.progress_indicator_container);
        progressIndicatorText = findViewById(R.id.progress_text);

        // 2. Set up the observers
        setupObservers();
        //This combats an issue on the target panasonic platform having to do with http reading.
        //System.setProperty("http.keepAlive", "false");
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar == null) {
            Log.e("START_ACTIVITY_ERROR", "findViewById(R.id.toolbar) returned NULL. This is the root cause of the crash. Check for build cache issues or ID typos.");
        }
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        assert actionbar != null;
        actionbar.setHomeAsUpIndicator(R.drawable.ic_round_menu_24px);
        actionbar.setDisplayHomeAsUpEnabled(true);
        mDrawerMenu = new  DrawerMenu(this,toolbar);
        mDrawerToggle = mDrawerMenu.getDrawerToggle();

        // Create a Sync account - REMOVED 2025
        // mAccount = CreateSyncAccount(this);

        //Determine if program should start or first reload its configuration.
        // Now that super.onCreate() has been called, FragmentManager will be ready.
        if (!loading) {
            // Consider passing savedInstanceState to checkStatics if you want to
            // avoid re-adding fragments on configuration changes.
            // For now, this direct call should work after moving super.onCreate().
            checkStatics();
        }


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
        // super.onCreate(savedInstanceState); // Already called at the top
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
        super.onStart();
    }


    /**
     *
     */
    private final String[] PERMISSIONS = {
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.INTERNET,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_NETWORK_STATE,
            android.Manifest.permission.VIBRATE,
            android.Manifest.permission.READ_SYNC_SETTINGS,
            android.Manifest.permission.WRITE_SYNC_SETTINGS
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
                //Start the login fragment.
                androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
                StartupFragment startupFragment = new StartupFragment();
                //Don't add loginfragment to backstack.
                fm.beginTransaction()
                        .replace(R.id.content_frame, startupFragment)
                        .addToBackStack("login")
                        .commit();

            } else {
                Log.d("vortex", "No need to run initial load - globalstate exists");

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

    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;

        // Handle other action bar items

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setTitle(CharSequence title) {

        //getSupportActionBar().setTitle(title);
        if (title != null)
        {
            // font color set to primary_light
            getSupportActionBar( ).setTitle( android.text.Html.fromHtml( "<font color='#d7ccc8'>" + title + "</font>" ) );
        }
    }

    //execute workflow.
    public void changePage(Workflow wf, String statusVar) {
        if (wf==null) {
            debugLogger.addRow("Workflow not defined for button. Check your project XML");
            Log.e("vortex","no wf in changepage");
            return;
        }
        GlobalState gs = GlobalState.getInstance();
        if (gs == null) {
            Log.e("vortex","Global State is null in change page. App needs to restart");
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
        Log.d("gipp","CHANGING PAGE TO: xxxxxxxx ["+wf.getName()+"] with template "+wf.getTemplate());
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
                template = "StartupFragment";
                label = (GlobalState.getInstance()!=null)?"Start":"Startup...";
                androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.popBackStack();
            }


            fragmentToExecute = wf.createFragment(template);
            fragmentToExecute.setArguments(args);

            changePage(fragmentToExecute,label);

            //show error message.
        } else
            showErrorMsg(cHash);
    }
    public void changePage(Fragment newPage, String label) {
        androidx.fragment.app.FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction ft = fragmentManager.beginTransaction();

        ft
                .replace(R.id.content_frame, newPage)
                .addToBackStack(label)
                .commit();
        setTitle(label);


    }



    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("vortex","IN ONACTIVITY RESULT ");
        Log.d("vortex","request code "+requestCode+" result code "+resultCode);
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentById(R.id.content_frame);
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
            //Log.d("vortex","gets here key back");

            if (mPopupWindow!=null) {
                //Log.d("vortex", "popup is not null");
                if (mPopupWindow.isShowing()) {

                    //Log.d("vortex", "closed popup, exiting");
                    mPopupWindow.dismiss();
                    return true;
                }
            }
            if (getDrawerMenu().isDrawerOpen()) {
                getDrawerMenu().closeDrawer();
                return true;
            }
            androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
            Fragment currentContentFrameFragment = fm.findFragmentById(R.id.content_frame);
            int x = fm.getBackStackEntryCount();
            Log.d("gipp", "backstack count: " + x);
            if (currentContentFrameFragment == null) {
                Log.d("gipp", "current fragment is null");
                return false;
            } else
                Log.d("gipp", "current content fragment: "+currentContentFrameFragment.getClass().getName());
            if (currentContentFrameFragment.getClass().getName().equals("com.teraim.fieldapp.dynamic.templates.StartupFragment")) {
                String dialogText = getString(R.string.exit_app);
                new AlertDialog.Builder(this)
                        .setTitle("Exit")
                        .setMessage(dialogText)
                        .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ok,new Dialog.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                finishAndRemoveTask();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new Dialog.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        })

                        .show();
            }


            if (currentContentFrameFragment instanceof Executor) {
                final WF_Context wfCtx = ((Executor) currentContentFrameFragment).getCurrentContext();
                Log.d("gipp", "current context: " + wfCtx);
                wfCtx.printD();
                boolean map = false;

                if (wfCtx != null) {
                    if (wfCtx.getCurrentGis() != null) {
                        map = true;
                        if (wfCtx.getCurrentGis().wasShowingPopup()) {
                            //Log.d("gipp", "closed popup, exiting");
                            return true;
                        }
                    }
                    Workflow wf = wfCtx.getWorkflow();
                    //Log.d("gipp", "gets here wf is " + wf.getLabel());
                    if (wf != null) {
                        if (map) {
                            //Log.d("gipp", "gets here too");
                            wfCtx.upOneMapLevel();
                        }
                    }
                    setTitle("");
                }


            }
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
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
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

    public void setTopBarVisibility(boolean isVisible) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (isVisible) {
                actionBar.show();
            } else {
                actionBar.hide();
            }
        } else {
            Log.w("StartActivity", "ActionBar not found, cannot set visibility.");
        }
    }

    private void setupObservers() {
        viewModel.finalProcessStatus.observe(this, workflowResult -> { // Renamed parameter for clarity
            if (workflowResult == null) return;
            switch (workflowResult.status) {
                case LOADING:
                    Log.d("heppola","LOADING");
                    progressIndicatorContainer.setVisibility(View.VISIBLE);
                    break;
                case SUCCESS:
                    Log.d("heppola","SUCCESS");
                    // Fall-through to also hide on success
                case FAILURE:
                    // Hide the indicator when the process is complete
                    progressIndicatorContainer.setVisibility(View.GONE);
                    break;
            }
        });
        // This observer updates the TEXT of the indicator
        viewModel.progressText.observe(this, text -> {
            if (text != null && !text.isEmpty()) {
                // We'll just show the first line of the detailed progress for a clean look
                String firstLine = text.split("\n")[0];
                progressIndicatorText.setText(firstLine);
            }
        });
    }
    /*
    @Override
    public void onBackStackChanged() {
        String TAG = "gipp";
        FragmentManager fm = getFragmentManager();
        Fragment currentFragment = fm.findFragmentById(R.id.content_frame); // Use your container ID
        if (currentFragment != null) {
            Log.d("gipp", "Current fragment is: " + currentFragment.getClass().getSimpleName());
        } else
            Log.d("gipp", "Current fragment is null");

        // 2. Get Back Stack Info
        int backStackCount = fm.getBackStackEntryCount();
        Log.i(TAG, "Back Stack Count: " + backStackCount);

        if (backStackCount > 0) {
            // Top entry (represents the transaction that led TO the current state)
            FragmentManager.BackStackEntry topEntry = fm.getBackStackEntryAt(backStackCount - 1);
            Log.i(TAG, "Top BackStack Entry (will be popped): Name='" + topEntry.getName() + "', ID=" + topEntry.getId());

            if (backStackCount > 1) {
                // Entry below the top (often represents the state TO BE restored after back press)
                FragmentManager.BackStackEntry previousEntry = fm.getBackStackEntryAt(backStackCount - 2);
                Log.i(TAG, "Previous BackStack Entry (likely visible after back): Name='" + previousEntry.getName() + "', ID=" + previousEntry.getId());
            } else {
                Log.i(TAG, "Previous BackStack Entry: None (stack size is 1)");
            }
        } else {
            Log.i(TAG, "Top BackStack Entry: None (stack is empty)");
        }
        Log.d(TAG,"--------------------------"); // Separator

    }
    */

}
