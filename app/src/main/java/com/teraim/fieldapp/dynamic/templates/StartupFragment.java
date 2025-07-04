package com.teraim.fieldapp.dynamic.templates;

import static androidx.core.content.ContextCompat.getColor;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.StartProvider;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.loadermodule.GisDatabaseWorkflow;
import com.teraim.fieldapp.loadermodule.ModuleRegistry;
import com.teraim.fieldapp.loadermodule.configurations.SpinnerConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.VariablesConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.WorkFlowBundleConfiguration;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.ui.MenuActivity;
import com.teraim.fieldapp.viewmodels.ModuleLoaderViewModel;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;


/**
 * A fragment that serves as the application's main entry point.
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
    private static final String KEY_PROVYTE_TYPES = "provYtaTypes";
    // Helpers and State
    private PersistenceHelper globalPh, ph;
    private String bundleName;
    private float oldAppVersion = -1;
    private boolean loadAllModules = false;
    private Start startInstance;
    private GisDatabaseWorkflow gisDatabaseWorkflowInstance;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof StartProvider) {
            startInstance = ((StartProvider) context).getStartInstance();
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement StartProvider");
        }
    }

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
        appVersionTextView.setText(oldAppVersion == -1 ? "[...]" : String.valueOf(oldAppVersion));
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
        if (GlobalState.getInstance() == null) {
            viewModel.workflowState.observe(getViewLifecycleOwner(), result -> {
                if (result == null) return;
                // This UI just needs to know the current state to show/hide the progress bar.
                // It will correctly re-evaluate on screen rotation.
                boolean startupFailed = false;

                switch (result.status()) {
                    case LOADING:
                        LogRepository.getInstance().addColorText("StartupFragment received workflowstate Loading",getColor(requireContext(),R.color.purple));
                        Log.d("StartupFragment", "Loading....");

                        break;
                    case SUCCESS:
                        LogRepository.getInstance().addColorText("StartupFragment received workflowstate success",getColor(requireContext(),R.color.purple));
                        // Pass the completed ModuleRegistry to the startApplication method.
                        // Corrected: Access registry on the unwrapped result
                        startupFailed = startApplication(result.registry()); // Use .registry() if it's a record
                        if (!startupFailed && loadAllModules) {
                            Set<String> provyteTypes = gisDatabaseWorkflowInstance.getMapObjectsToRefresh();
                            LogRepository.getInstance().addColorText("Setting provyte types: " + provyteTypes.toString(), getColor(requireContext(), R.color.purple));
                            GlobalState.getInstance().setProvYtaTypes(provyteTypes);
                            persistProvYtaTypes(provyteTypes);
                            Log.d("StartupFragment", "ProvYta types set and persisted after full load.");
                        }
                        break;
                    case FAILURE:
                        startupFailed = true;
                        LogRepository.getInstance().addColorText("StartupFragment received workflowstate Failure",getColor(requireContext(),R.color.purple));
                        showErrorDialog("An error occurred during loading. Please check your connection and try again.");
                        break;
                }
                if (startupFailed) {
                    Intent intent = new Intent();
                    intent.setAction(MenuActivity.INITFAILED);
                    LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);
                }
            });
        } else {
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
            Bundle b = this.getArguments();
            if (b != null && b.getBoolean(Constants.RELOAD_DB_MODULES)) {
                Log.d("StartupFragment", "Reload of DB modules triggered by configuration change");
                loadAllModules = true;
            }
            startLoadingProcess(loadAllModules); // 'false' means this is not a forced reload
        } else {
            // If GlobalState is already initialized, ensure provyteTypes are loaded from persistence
            // in case the app was killed and restarted.
            loadProvYtaTypesFromPersistence();
        }
    }

    /**
     * Kicks off the loading process via the ViewModel.
     * @param loadAllModules If true, all modules will be fetched from the server, ignoring cache.
     */
    private void startLoadingProcess(boolean loadAllModules) {
        if (loadAllModules && GlobalState.getInstance() != null) {
            GlobalState.destroy();
        }
        Intent intent = new Intent();
        intent.setAction(MenuActivity.INITSTARTS);
        LocalBroadcastManager.getInstance(this.getActivity()).sendBroadcast(intent);

        // The fragment's job is now extremely simple:
        // 1. Create the workflow definition.
        gisDatabaseWorkflowInstance = new GisDatabaseWorkflow(getContext(), globalPh, ph);

        // 2. Tell the ViewModel to execute it.
        // The ViewModel now handles all the complex pre-check and loading logic.
        viewModel.execute(gisDatabaseWorkflowInstance, loadAllModules);
    }

    /**
     * This method is called after a successful configuration load.
     * It initializes the GlobalState and navigates to the main workflow of the app.
     * @param moduleRegistry The completed registry containing all loaded modules.
     */
    private boolean startApplication(ModuleRegistry moduleRegistry) {
        // This logic is ported from the old LoginConsoleFragment.
        WorkFlowBundleConfiguration wfC = (WorkFlowBundleConfiguration) moduleRegistry.getModule(bundleName);
        if (wfC == null) {
            showErrorDialog("Workflow bundle configuration failed to load. Cannot start application.");
            return true;
        }

        List<Workflow> workflows = wfC.getEssence();
        String imgMetaFormat = wfC.getImageMetaFormat();
        Table t = (Table) moduleRegistry.getModule(VariablesConfiguration.NAME).getEssence();
        SpinnerDefinition sd = (SpinnerDefinition) moduleRegistry.getModule(SpinnerConfiguration.NAME).getEssence();

        if (t == null) {
            showErrorDialog("Variable configuration (Table) is null. Cannot start application.");
            return true;
        }

        DbHelper myDb = new DbHelper(requireActivity().getApplicationContext(), t, globalPh, ph, bundleName);
        gs = GlobalState.createInstance(startInstance, requireActivity().getApplicationContext(), globalPh, ph, myDb, workflows, t, sd, "", imgMetaFormat);

        if (gs.getBackupManager().timeToBackup()) {
            gs.getBackupManager().backUp();
        }

        float loadedAppVersion = ph.getF(PersistenceHelper.NEW_APP_VERSION);
        ph.put(PersistenceHelper.CURRENT_VERSION_OF_APP, loadedAppVersion);
        gs.setDrawerMenu(startInstance.getDrawerMenu());
        gs.setModuleRegistry(moduleRegistry);
        startInstance.getDrawerMenu().closeDrawer();
        startInstance.getDrawerMenu().clear();
        Workflow wf = gs.getWorkflow("Main");
        gs.sendEvent(MenuActivity.INITDONE);
        //Redraws the same fragment but now with a global state.
        startInstance.changePage(wf, null);
        return false;
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
                            // Clear persisted provYtaTypes as they will be re-generated
                            ph.remove(KEY_PROVYTE_TYPES);
                        }
                        Tools.restart(this.getActivity());
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
        LogRepository.getInstance().addText(message);
        LogRepository.getInstance().addCriticalText("***Startup Aborted***");
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
        loadAllModules = true;
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
        LogRepository.getInstance().setLogLevel(LogRepository.LogLevel.CRITICAL);
    }

    /**
     * Persists the set of provyte types to local preferences.
     * @param provYtaTypes The set of provyte types to persist.
     */
    private void persistProvYtaTypes(Set<String> provYtaTypes) {
        if (ph != null && provYtaTypes != null) {
            String provYtaTypesString = String.join(",", provYtaTypes);
            ph.put(KEY_PROVYTE_TYPES, provYtaTypesString);
            Log.d("StartupFragment", "Persisted ProvYta types: " + provYtaTypesString);
        }
    }

    /**
     * Loads the set of provyte types from local preferences and sets them in GlobalState.
     */
    private void loadProvYtaTypesFromPersistence() {
        if (ph != null && GlobalState.getInstance() != null) {
            String provYtaTypesString = ph.get(KEY_PROVYTE_TYPES, "");
            Set<String> loadedProvYtaTypes = new HashSet<>();
            if (!provYtaTypesString.isEmpty()) {
                loadedProvYtaTypes.addAll(Arrays.asList(provYtaTypesString.split(",")));
            }
            GlobalState.getInstance().setProvYtaTypes(loadedProvYtaTypes);
            Log.d("StartupFragment", "Loaded ProvYta types from persistence: " + loadedProvYtaTypes.toString());
        }
    }


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
