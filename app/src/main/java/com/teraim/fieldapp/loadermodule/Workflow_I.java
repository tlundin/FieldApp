package com.teraim.fieldapp.loadermodule;

public interface Workflow_I {
    /**
     * Gets the first job to be executed in the workflow.
     * @return The initial LoadJob.
     */
    LoadJob getInitialJob();

    /**
     * Gets the next job in the workflow based on the results of the previous stage.
     * @param previousJobRegistry The registry containing the successfully loaded modules from the last job.
     * @return The next LoadJob to execute, or null if the workflow is complete.
     */
    LoadJob getNextJob(ModuleRegistry previousJobRegistry);
}