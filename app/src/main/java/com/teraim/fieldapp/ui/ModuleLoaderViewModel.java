package com.teraim.fieldapp.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.teraim.fieldapp.loadermodule.LoadCompletionEvent;
import com.teraim.fieldapp.loadermodule.LoadJob;
import com.teraim.fieldapp.loadermodule.LoadingStatus;
import com.teraim.fieldapp.loadermodule.ModuleRegistry;
import com.teraim.fieldapp.loadermodule.StatefulModuleLoader;
import com.teraim.fieldapp.loadermodule.Workflow_I;


public class ModuleLoaderViewModel extends ViewModel {

    // These are exposed to the Fragment and represent the OVERALL plan's status.
    private final MutableLiveData<LoadingStatus> _finalProcessStatus = new MutableLiveData<>();
    public final LiveData<LoadingStatus> finalProcessStatus = _finalProcessStatus;

    private final MutableLiveData<String> _progressText = new MutableLiveData<>("");
    public final LiveData<String> progressText = _progressText;
    /**
     * The single public entry point. The Fragment provides a plan (a Queue of jobs)
     * and the ViewModel executes it.
     */
    private Workflow_I currentWorkflow;
    private ModuleRegistry registry;

    public void execute(Workflow_I workflow, ModuleRegistry registry) {
        if (_finalProcessStatus.getValue() == LoadingStatus.LOADING) return;
        this.registry=registry;
        this.currentWorkflow = workflow;
        _finalProcessStatus.setValue(LoadingStatus.LOADING);

        LoadJob initialJob = workflow.getInitialJob();
        if (initialJob == null || initialJob.modules.isEmpty()) {
            _finalProcessStatus.setValue(LoadingStatus.SUCCESS);
            return;
        }
        runJob(initialJob);
    }

    private void runJob(LoadJob job) {
        StatefulModuleLoader loader = new StatefulModuleLoader(job.modules, job.stage);

        loader.progressText.observeForever(_progressText::postValue);

        loader.loadingStatus.observeForever(new Observer<LoadCompletionEvent>() {
            @Override
            public void onChanged(LoadCompletionEvent completionEvent) {
                loader.loadingStatus.removeObserver(this);
                loader.progressText.removeObserver(_progressText::postValue);

                if (completionEvent.status == LoadingStatus.SUCCESS) {
                    registry.add(job.modules);
                    LoadJob nextJob = currentWorkflow.getNextJob(registry);

                    if (nextJob != null) {
                        runJob(nextJob); // Execute the next job in the chain
                    } else {
                        // No next job, the workflow is successfully completed.
                        _finalProcessStatus.postValue(LoadingStatus.SUCCESS);
                    }
                } else {
                    // A job failed, so the entire workflow fails.
                    _finalProcessStatus.postValue(LoadingStatus.FAILURE);
                }
            }
        });

        loader.startLoading();
    }
}