package com.teraim.fieldapp.loadermodule;

import java.util.List;


// A class representing one job in our loading plan.
public class LoadJob {
    public final LoadStage stage;
    public final List<ConfigurationModule> modules;

    public LoadJob(LoadStage stage, List<ConfigurationModule> modules) {
        this.stage = stage;
        this.modules = modules;
    }
}
