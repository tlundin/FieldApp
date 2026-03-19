package com.teraim.fieldapp.loadermodule;

import java.util.List;


// A class representing one job in our loading plan.
public record LoadJob(LoadStage stage, List<ConfigurationModule> modules) {
}
