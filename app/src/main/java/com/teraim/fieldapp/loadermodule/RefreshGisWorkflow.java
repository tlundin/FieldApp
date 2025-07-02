package com.teraim.fieldapp.loadermodule;

import android.content.Context;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.loadermodule.configurations.GISListConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.GisObjectConfiguration;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.PersistenceHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import okhttp3.internal.Util;

/**
 * A specific workflow designed only to refresh the GIS database modules.
 * This workflow has only one stage.
 */
public class RefreshGisWorkflow implements Workflow_I {

    private final Context context;
    private final PersistenceHelper globalPh;
    private final PersistenceHelper ph;
    private final LogRepository debugConsole;
    private final GlobalState gs;

    public RefreshGisWorkflow(Context context, GlobalState gs) {
        this.context = context;
        this.gs = gs;
        this.globalPh = gs.getGlobalPreferences();
        this.ph = gs.getPreferences();
        this.debugConsole = gs.getLogger();
    }

    @Override
    public LoadJob getInitialJob() {
        // This is the only job in this workflow.
        // It retrieves the modules needed for the GIS objects.

        // First, we need to get the list of GIS types, which may require
        // modules that are already loaded in the registry.
        ModuleRegistry registry = gs.getModuleRegistry();
        if (registry == null) {
            Log.e("Workflow", "Cannot refresh GIS modules: ModuleRegistry is not available in GlobalState.");
            return null;
        }

        GISListConfiguration gisListModule = (GISListConfiguration) registry.getModule(GISListConfiguration.NAME);
        if (gisListModule == null) {
            Log.e("Workflow", "Cannot refresh GIS modules: GISListConfiguration is not loaded.");
            LogRepository.getInstance().addCriticalText("Cannot refresh GIS modules: The gis list ('content' file) is missing from device storage.");
            return null;
        }

        String bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
        String url = globalPh.get(PersistenceHelper.SERVER_URL) + bundleName.toLowerCase(Locale.ROOT) + "/";
        String gisPath = url + Constants.GIS_CONFIG_WEB_FOLDER + "/";

        Table t = gs.getVariableConfiguration().getTable();

        List<ConfigurationModule> databaseModules = createDBModules(context, gisPath, bundleName, t, debugConsole, registry);

        if (databaseModules.isEmpty()) {
            Log.d("Workflow", "No GIS modules to refresh.");
            LogRepository.getInstance().addColorText("Found no GIS modules to refresh.", ContextCompat.getColor(context, R.color.purple));
            return null; // Nothing to refresh.
        }
        LogRepository.getInstance().addColorText("Refresh started for GIS modules:", ContextCompat.getColor(context, R.color.purple));
        for (ConfigurationModule module : databaseModules) {
            LogRepository.getInstance().addColorText(" - " + module.getLabel(), ContextCompat.getColor(context, R.color.purple));
        }
        return new LoadJob(LoadStage.PROVYTOR, databaseModules);
    }

    @Override
    public LoadJob getNextJob(ModuleRegistry registry) {
        // This workflow only has one stage, so this method always returns null.
        return null;
    }
    private final Set<String> provyteTyper = GlobalState.getInstance().getProvYtaTypes();
    private List<ConfigurationModule> createDBModules(Context context, String gisFolder, String bundleName, Table t, LogRepository debugConsole,ModuleRegistry registry) {
        List<ConfigurationModule> modules = new ArrayList<>();
            for (String provyta : provyteTyper) {
                    Log.d("Workflow", "Adding Module " + provyta);
                    modules.add(new GisObjectConfiguration(context, globalPh, ph, gisFolder, provyta, debugConsole, gs.getDb(), t));
            }

        return modules;
    }
}
