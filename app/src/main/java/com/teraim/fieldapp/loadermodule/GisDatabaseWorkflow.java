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

    // Dependencies needed for the workflow can be passed in the constructor.
    private final Context context;
    private final PersistenceHelper globalPh;
    private final PersistenceHelper ph;
    private LoggerI debugConsole;
    private String bundleName, url, gisPath;

    private int stageCount = 0;
    public GisDatabaseWorkflow(Context context, PersistenceHelper globalPh, PersistenceHelper ph) {
        this.context = context;
        this.globalPh = globalPh;
        this.ph = ph;
        this.debugConsole = Start.singleton.getLogger();
        this.bundleName = globalPh.get(PersistenceHelper.BUNDLE_NAME);
        this.url = globalPh.get(PersistenceHelper.SERVER_URL)+bundleName.toLowerCase(Locale.ROOT)+"/";
        this.gisPath = url+Constants.GIS_CONFIG_WEB_FOLDER +"/";
    }

    @Override
    public LoadJob getInitialJob() {
        // The first job is to load the modules that the next stage depends on.
        List<ConfigurationModule> initialModules = new LinkedList<>();
        String cachePath = context.getFilesDir()+"/"+bundleName.toLowerCase(Locale.ROOT) + "/cache/";
        LoggerI debugConsole = Start.singleton.getLogger();
        initialModules.add(new WorkFlowBundleConfiguration(context,cachePath,globalPh,ph,url,bundleName,debugConsole));
        initialModules.add(new SpinnerConfiguration(context,globalPh,ph,url,debugConsole));
        initialModules.add(new GroupsConfiguration(context,globalPh,ph,url,bundleName,debugConsole));
        //VariableConfiguration depends on the Groups Configuration.
        initialModules.add(new VariablesConfiguration(context,globalPh,ph,url,debugConsole));
        initialModules.add(new GISListConfiguration(context,globalPh,ph,gisPath));
        stageCount++;
        return new LoadJob(LoadStage.FILES, initialModules);
    }

    @Override
    public LoadJob getNextJob(ModuleRegistry registry) {
        if (stageCount == 1) {
            ConfigurationModule varConfModule = registry.getModule(VariablesConfiguration.NAME);
            GISListConfiguration gisListModule = (GISListConfiguration) registry.getModule(GISListConfiguration.NAME);
            // If a dependency is missing, we can't continue.
            if (varConfModule == null || gisListModule == null) {
                Log.e("Workflow", "A required module dependency was missing. Aborting workflow.");
                return null; // End the workflow
            }

            Table t = (Table) varConfModule.getEssence();
            List<String> gisList = gisListModule.getGisTypes();
            List<ConfigurationModule> databaseModules = createDBModules(context, gisPath, bundleName, t, gisList, debugConsole);

            if (databaseModules.isEmpty()) {
                return null; // No more jobs to run, workflow is complete.
            }

            // Return the second job. After this, getNextJob will be called again.
            // Since there is no third stage, we will return null next time.
            stageCount++;
            return new LoadJob(LoadStage.DATABASES, databaseModules);
        }
        return null;
    }

    private List<ConfigurationModule> createDBModules(Context context, String gisFolder, String bundleName, Table t,List<String> gisTypes, LoggerI debugConsole) {
        DbHelper myDb = new DbHelper(context.getApplicationContext(), t, globalPh, ph, bundleName);
        List<ConfigurationModule> modules = new ArrayList<>();
        for (String gisType:gisTypes) {
            Log.d("vortex","gis type is "+gisType);
            modules.add(new GisObjectConfiguration(context, globalPh, ph, gisFolder, gisType, debugConsole, myDb, t));
        }
        return modules;
    }
}
