package com.teraim.fieldapp.dynamic.templates;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.GisDatabaseWorkflow;
import com.teraim.fieldapp.loadermodule.LoadJob;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.ModuleRegistry;
import com.teraim.fieldapp.loadermodule.Workflow_I;
import com.teraim.fieldapp.loadermodule.configurations.SpinnerConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.VariablesConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.WorkFlowBundleConfiguration;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.ui.ModuleLoaderViewModel;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A modern, combined fragment that serves as the application's main entry point.
 * It handles first-time initialization, displays app information, and manages both
 * the initial configuration load and user-triggered reloads via a ViewModel.
 */
public class StartupFragment extends Executor {

    // UI Elements
    private ViewGroup my_root;
    private ImageView bgImageView, logoImageView;
    private TextView appNameTextView, appVersionTextView, engineVersionTextView;
    private Button loadConfigurationButton;

    // ViewModel for handling background loading tasks
    private ModuleLoaderViewModel viewModel;

    // Helpers and State
    private PersistenceHelper globalPh, ph;
    private String bundleName;
    private float oldAppVersion = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_startup, container, false);

        // Initialize UI components
        my_root = view.findViewById(R.id.myRoot);
        loadConfigurationButton = view.findViewById(R.id.buttonLoadConfig);
        appNameTextView = view.findViewById(R.id.textViewAppValue);
        appVersionTextView = view.findViewById(R.id.textViewVersionValue);
        engineVersionTextView = view.findViewById(R.id.textViewAppVersion);
        bgImageView = view.findViewById(R.id.bgImg);
        logoImageView = view.findViewById(R.id.logo);

        // Initialize helpers
        globalPh = new PersistenceHelper(requireActivity().getApplicationContext().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE));

        // Perform first-time setup if needed
        if (initIfFirstTime()) {
            if (!Connectivity.isConnected(getActivity())) {
                showErrorDialog("A network connection is required the first time you start the app to download configuration files.");
            } else {
                initialize();
            }
        }

        // Load configuration and display initial UI text
        bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME, Constants.DEFAULT_APP);
        ph = new PersistenceHelper(requireActivity().getApplicationContext().getSharedPreferences(bundleName, Context.MODE_PRIVATE));
        oldAppVersion = ph.getF(PersistenceHelper.CURRENT_VERSION_OF_APP);

        appNameTextView.setText(bundleName);
        appVersionTextView.setText(oldAppVersion == -1 ? "[No version]" : String.valueOf(oldAppVersion));
        engineVersionTextView.setText("Vortex Engine " + Constants.VORTEX_VERSION);

        // Load background and logo images asynchronously
        loadStaticImages();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get the shared ViewModel scoped to the Activity
        viewModel = new ViewModelProvider(requireActivity()).get(ModuleLoaderViewModel.class);

        // Set up the listener for the reload button
        loadConfigurationButton.setOnClickListener(v -> showReloadDialog());

        // Set up the observer for the final completion status of the loading process
        // This will now receive a result object containing the ModuleRegistry on success.
        viewModel.finalProcessStatus.observe(getViewLifecycleOwner(), workflowResult -> {
            if (workflowResult == null) return;

            switch (workflowResult.status) {
                case SUCCESS:
                    // ADD THIS CHECK: Only start the application if it has not been started already.
                    // This prevents the observer from re-triggering the start process if the LiveData
                    // re-delivers its "sticky" SUCCESS state when the fragment is recreated.
                    if (GlobalState.getInstance() == null) {
                        Toast.makeText(getContext(), "Configuration loaded successfully!", Toast.LENGTH_SHORT).show();
                        Log.d("StartupFragment", "Load successful. Finalizing setup.");
                        // Pass the completed ModuleRegistry to the startApplication method.
                        startApplication(workflowResult.registry);
                    } else {
                        Log.d("StartupFragment", "SUCCESS event re-delivered, but GlobalState already exists. Ignoring.");
                    }
                    break;
                case FAILURE:
                    Log.d("StartupFragment", "Load failed.");
                    showErrorDialog("An error occurred during loading. Please check your connection and try again.");
                    break;
                case LOADING:
                    // The shared UI indicator in the Activity handles this state.
                    break;
            }
        });
        if (GlobalState.getInstance() !=null && wf!=null) {
			Log.d("gipp", "Executing workflow main in Startup ");
            run();
		}
    }

    @Override
    public void onResume() {
        super.onResume();
        // This is the primary entry point for the automatic load.
        // If GlobalState is not initialized, it means we need to load the configuration.
        if (GlobalState.getInstance() == null) {
            Log.d("StartupFragment", "GlobalState is null. Starting initial configuration load.");
            startLoadingProcess(false); // 'false' means this is not a forced reload
        }
    }

    /**
     * Kicks off the loading process via the ViewModel.
     * @param forceReload If true, all modules will be fetched from the server, ignoring cache.
     */
    private void startLoadingProcess(boolean forceReload) {
        if (forceReload && GlobalState.getInstance() != null) {
            GlobalState.destroy();
        }

        // The fragment's job is now extremely simple:
        // 1. Create the workflow definition.
        Workflow_I myWorkflow = new GisDatabaseWorkflow(getContext(), globalPh, ph);

        // 2. Tell the ViewModel to execute it.
        // The ViewModel now handles all the complex pre-check and loading logic.
        viewModel.execute(myWorkflow, forceReload);
    }

    /**
     * This method is called after a successful configuration load.
     * It initializes the GlobalState and navigates to the main workflow of the app.
     * @param moduleRegistry The completed registry containing all loaded modules.
     */
    private void startApplication(ModuleRegistry moduleRegistry) {
        // This logic is ported from the old LoginConsoleFragment.
        WorkFlowBundleConfiguration wfC = (WorkFlowBundleConfiguration) moduleRegistry.getModule(bundleName);
        if (wfC == null) {
            showErrorDialog("Workflow bundle configuration failed to load. Cannot start application.");
            return;
        }

        List<Workflow> workflows = (List<Workflow>) wfC.getEssence();
        String imgMetaFormat = wfC.getImageMetaFormat();
        Table t = (Table) (moduleRegistry.getModule(VariablesConfiguration.NAME).getEssence());
        SpinnerDefinition sd = (SpinnerDefinition) (moduleRegistry.getModule(SpinnerConfiguration.NAME).getEssence());

        if (t == null) {
            showErrorDialog("Variable configuration (Table) is null. Cannot start application.");
            return;
        }

        DbHelper myDb = new DbHelper(requireActivity().getApplicationContext(), t, globalPh, ph, bundleName);
        gs = GlobalState.createInstance(requireActivity().getApplicationContext(), globalPh, ph, Start.singleton.getLogger(), myDb, workflows, t, sd, "", imgMetaFormat);

        if (gs.getBackupManager().timeToBackup()) {
            gs.getBackupManager().backUp();
        }

        float loadedAppVersion = ph.getF(PersistenceHelper.NEW_APP_VERSION);
        ph.put(PersistenceHelper.CURRENT_VERSION_OF_APP, loadedAppVersion);
        gs.setDrawerMenu(Start.singleton.getDrawerMenu());
        gs.setModuleRegistry(moduleRegistry);
        Start.singleton.getDrawerMenu().closeDrawer();
        Start.singleton.getDrawerMenu().clear();
        Workflow wf = gs.getWorkflow("Main");
        //Redraws the same fragment but now with a global state.
        Start.singleton.changePage(wf, null);
    }


    private void showReloadDialog() {
        if (!Connectivity.isConnected(getContext())) {
            showErrorDialog("No connection - cannot reload configuration. Please try again when you are connected.");
        } else {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.ladda_styrfiler)
                    .setMessage(R.string.ladda_descr)
                    .setPositiveButton("Ok", (dialog, which) -> {
                        Log.d("StartupFragment", "User triggered a force reload.");
                        if (GlobalState.getInstance() != null) {
                            GlobalState.getInstance().getDrawerMenu().clear();
                            GlobalState.destroyInstance();
                        }
                        startLoadingProcess(true);
                    })
                    .setNegativeButton("Cancel", null)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }
    }

    private void showErrorDialog(String message) {
        if (getActivity() == null) return;
        new AlertDialog.Builder(getActivity())
                .setTitle("Error")
                .setMessage(message)
                .setPositiveButton("Ok", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void loadStaticImages() {
        String serverURL = globalPh.get(PersistenceHelper.SERVER_URL);
        String appBaseUrl = serverURL + bundleName.toLowerCase(Locale.ROOT) + "/";
        final String cacheFolder = requireContext().getFilesDir() + "/" + bundleName.toLowerCase(Locale.ROOT) + "/cache/";

        // Use a full anonymous class to implement the multi-method interface
        Tools.onLoadCacheImage(appBaseUrl, "bg_image.jpg", cacheFolder, new Tools.WebLoaderCb() {
            @Override
            public void loaded(Boolean result) {
                if (result) {
                    Bitmap bm = BitmapFactory.decodeFile(cacheFolder + "bg_image.jpg");
                    if (bm != null) bgImageView.setImageBitmap(bm);
                }
            }

            @Override
            public void progress(int bytesRead) {
                // You can leave this empty if you don't need to show download progress
            }
        });

        // Use a full anonymous class here as well
        Tools.onLoadCacheImage(appBaseUrl, "logo.png", cacheFolder, new Tools.WebLoaderCb() {
            @Override
            public void loaded(Boolean result) {
                if (result) {
                    Bitmap bm = BitmapFactory.decodeFile(cacheFolder + "logo.png");
                    if (bm != null) logoImageView.setImageBitmap(bm);
                }
            }

            @Override
            public void progress(int bytesRead) {
                // Leave empty
            }
        });
    }

    private boolean initIfFirstTime() {
        boolean first = globalPh.get(PersistenceHelper.FIRST_TIME_KEY, PersistenceHelper.UNDEFINED).equals(PersistenceHelper.UNDEFINED);
        if (first) {
            Log.d("StartupFragment", "First time execution detected. Initializing.");
            return true;
        }
        return false;
    }

    private void initialize() {
        File[] externalStorageVolumes = ContextCompat.getExternalFilesDirs(requireContext(), null);
        File primaryExternalStorage = externalStorageVolumes[0];

        // Create required application folders
        new File(primaryExternalStorage.getAbsolutePath() + "/pics/").mkdirs();
        new File(primaryExternalStorage.getAbsolutePath() + "/old_pics/").mkdirs();
        new File(primaryExternalStorage.getAbsolutePath() + "/export/").mkdirs();
        new File(requireContext().getFilesDir() + "/" + globalPh.get(PersistenceHelper.BUNDLE_NAME).toLowerCase(Locale.ROOT) + "/cache/").mkdirs();

        // Set default global preferences
        globalPh.put(PersistenceHelper.BUNDLE_NAME, Constants.DEFAULT_APP);
        globalPh.put(PersistenceHelper.VERSION_CONTROL, "Major");
        globalPh.put(PersistenceHelper.SYNC_METHOD, "NONE");
        globalPh.put(PersistenceHelper.LOG_LEVEL, "critical");
        globalPh.put(PersistenceHelper.SERVER_URL, Constants.DEFAULT_SERVER_URI);
        globalPh.put(PersistenceHelper.EXPORT_SERVER_URL, Constants.DEFAULT_EXPORT_SERVER);

        // Mark initialization as complete
        globalPh.put(PersistenceHelper.FIRST_TIME_KEY, "Initialized");
        globalPh.put(PersistenceHelper.TIME_OF_FIRST_USE, System.currentTimeMillis());
    }

    //endregion

    //region Executor Overrides

    @Override
    public boolean execute(String function, String target) {
        // This can be expanded if the startup page needs to execute functions.
        return true;
    }

    @Override
    protected List<WF_Container> getContainers() {
        ArrayList<WF_Container> ret = new ArrayList<>();
        ret.add(new WF_Container("root", my_root, null));
        return ret;
    }

    //endregion
}
