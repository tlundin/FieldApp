package com.teraim.fieldapp.loadermodule;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.loadermodule.configurations.GISListConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.GisObjectConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.GroupsConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.SpinnerConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.VariablesConfiguration;
import com.teraim.fieldapp.loadermodule.configurations.WorkFlowBundleConfiguration;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class GisDatabaseWorkflow implements Workflow_I {

    // Dependencies needed for the workflow
    private final Context context;
    private final PersistenceHelper globalPh;
    private final PersistenceHelper ph;
    private final LoggerI debugConsole;
    private final String bundleName, url, gisPath;

    // A pre-filtered list of modules to download, can be null.
    private final List<ConfigurationModule> modulesToDownload;

    private int stageCount = 0;

    /**
     * Private "master" constructor that handles all field initializations.
     * Public constructors will call this one.
     */

    private GisDatabaseWorkflow(Context context, PersistenceHelper globalPh, PersistenceHelper ph, List<ConfigurationModule> modulesToDownload,boolean _dummy) {
        this.context = context;
        this.globalPh = globalPh;
        this.ph = ph;
        this.debugConsole = Start.singleton.getLogger();
        this.bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
        this.url = globalPh.get(PersistenceHelper.SERVER_URL) + bundleName.toLowerCase(Locale.ROOT) + "/";
        this.gisPath = url + Constants.GIS_CONFIG_WEB_FOLDER + "/";
        this.modulesToDownload = modulesToDownload;
    }

    /**
     * Default public constructor. Creates a workflow that will generate a full list of initial modules.
     * It calls the private constructor, passing null for the pre-filtered list.
     */
    public GisDatabaseWorkflow(Context context, PersistenceHelper globalPh, PersistenceHelper ph) {
        this(context, globalPh, ph, null, false);
    }

    /**
     * Overloaded public constructor. Creates a workflow that will only operate on a provided list of modules.
     * It calls the private constructor, passing the provided list.
     */
    public GisDatabaseWorkflow(Context context, PersistenceHelper globalPh, PersistenceHelper ph, List<ConfigurationModule> modulesToDownload) {
        this(context, globalPh, ph, modulesToDownload, false);
    }




    @Override
    public LoadJob getInitialJob() {
        // If a pre-filtered list was provided, use it directly for the first job.
        if (modulesToDownload != null) {
            Log.d("Workflow", "Executing workflow with a pre-filtered list of " + modulesToDownload.size() + " modules.");
            stageCount = 1; // Start at stage 1
            return new LoadJob(LoadStage.FILES, modulesToDownload);
        }

        // Otherwise, generate the full list of initial modules as before.
        Log.d("Workflow", "Executing workflow with the full list of initial modules.");
        List<ConfigurationModule> initialModules = new LinkedList<>();
        String cachePath = context.getFilesDir() + "/" + bundleName.toLowerCase(Locale.ROOT) + "/cache/";

        initialModules.add(new WorkFlowBundleConfiguration(context, cachePath, globalPh, ph, url, bundleName, debugConsole));
        initialModules.add(new SpinnerConfiguration(context, globalPh, ph, url, debugConsole));
        initialModules.add(new GroupsConfiguration(context, globalPh, ph, url, bundleName, debugConsole));
        initialModules.add(new VariablesConfiguration(context, globalPh, ph, url, debugConsole));
        initialModules.add(new GISListConfiguration(context, globalPh, ph, gisPath));

        stageCount = 1;
        return new LoadJob(LoadStage.FILES, initialModules);
    }

    @Override
    public LoadJob getNextJob(ModuleRegistry registry) {
        if (stageCount == 1) {
            stageCount = 2; // Advance the stage to prevent re-running this block

            ConfigurationModule varConfModule = registry.getModule(VariablesConfiguration.NAME);
            GISListConfiguration gisListModule = (GISListConfiguration) registry.getModule(GISListConfiguration.NAME);

            if (varConfModule == null || gisListModule == null) {
                Log.e("Workflow", "A required module dependency was missing. Aborting workflow.");
                return null;
            }

            Table t = (Table) varConfModule.getEssence();
            List<String> gisList = gisListModule.getGisTypes();
            List<ConfigurationModule> databaseModules = createDBModules(context, gisPath, bundleName, t, gisList, debugConsole);

            if (databaseModules.isEmpty()) {
                return null; // No more jobs to run, workflow is complete.
            }

            return new LoadJob(LoadStage.DATABASES, databaseModules);
        }
        // After stage 2, there are no more jobs.
        return null;
    }

    private List<ConfigurationModule> createDBModules(Context context, String gisFolder, String bundleName, Table t, List<String> gisTypes, LoggerI debugConsole) {
        DbHelper myDb = new DbHelper(context.getApplicationContext(), t, globalPh, ph, bundleName);
        List<ConfigurationModule> modules = new ArrayList<>();
        if (gisTypes != null) {
            for (String gisType : gisTypes) {
                Log.d("vortex", "gis type is " + gisType);
                modules.add(new GisObjectConfiguration(context, globalPh, ph, gisFolder, gisType, debugConsole, myDb, t));
            }
        }
        return modules;
    }
}
