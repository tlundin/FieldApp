package com.teraim.fieldapp.ui;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.teraim.fieldapp.dynamic.blocks.CreateGisBlock;
import com.teraim.fieldapp.dynamic.types.Event;
import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadCompletionEvent;
import com.teraim.fieldapp.loadermodule.LoadJob;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadingStatus;
import com.teraim.fieldapp.loadermodule.ModuleLoaderCb;
import com.teraim.fieldapp.loadermodule.ModuleRegistry;
import com.teraim.fieldapp.loadermodule.PhotoMetaI;
import com.teraim.fieldapp.loadermodule.StatefulModuleLoader;
import com.teraim.fieldapp.loadermodule.Workflow_I;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModuleLoaderViewModel extends ViewModel {

    /**
         * A helper class to wrap the final result of a workflow execution.
         */
        public record WorkflowResult(LoadingStatus status, ModuleRegistry registry) {
    }

    // LiveData exposed to the UI layer
    private final MutableLiveData<Event<WorkflowResult>> _finalProcessStatus = new MutableLiveData<>();
    public LiveData<Event<WorkflowResult>> finalProcessStatus = _finalProcessStatus;

    private final MutableLiveData<String> _progressText = new MutableLiveData<>("");
    public final LiveData<String> progressText = _progressText;

    // Executor for one-off background tasks like loading photo metadata.
    private final ExecutorService singleTaskExecutor = Executors.newSingleThreadExecutor();

    private Workflow_I currentWorkflow;

    /**
     * Executes a workflow. This version reverts to a fully parallel model where
     * all modules are dispatched immediately for processing.
     *
     * @param workflow The workflow to execute.
     * @param forceReload If true, all modules will be fetched from the server.
     */
    public void execute(Workflow_I workflow, boolean forceReload) {
        if (_finalProcessStatus.getValue() != null && _finalProcessStatus.getValue().peekContent().status == LoadingStatus.LOADING) {
            Log.w("ModuleLoaderViewModel", "Execution request ignored: a workflow is already running.");
            return;
        }
        _finalProcessStatus.postValue(new Event<>(new WorkflowResult(LoadingStatus.LOADING, null)));

        final ModuleRegistry registry = new ModuleRegistry();
        this.currentWorkflow = workflow;

        // The initial setup work now runs directly on the calling thread (main thread).
        // This may cause a brief "skipped frames" warning but avoids the slowdown and
        // errors from the previous sequential pre-check model.
        LoadJob initialJob = workflow.getInitialJob();
        if (initialJob == null || initialJob.modules.isEmpty()) {
            _finalProcessStatus.setValue(new Event<>(new WorkflowResult(LoadingStatus.SUCCESS,registry)));
            return;
        }

        // Directly call runJob, which will set up the parallel loader.
        runJob(initialJob, forceReload, registry, workflow);
    }

    /**
     * Runs a specific job in the workflow. This method MUST be called on the main thread
     * because it sets up LiveData observers.
     */
    private void runJob(final LoadJob job, final boolean forceReload, final ModuleRegistry registry, final Workflow_I workflow) {
        StatefulModuleLoader loader = new StatefulModuleLoader(job.modules, job.stage, forceReload);

        // These observeForever calls are safe because execute() ensures runJob is on the main thread.
        loader.progressText.observeForever(_progressText::postValue);
        loader.loadingStatus.observeForever(new Observer<>() {
            @Override
            public void onChanged(LoadCompletionEvent completionEvent) {
                loader.loadingStatus.removeObserver(this);
                loader.progressText.removeObserver(_progressText::postValue);

                if (completionEvent.status == LoadingStatus.SUCCESS) {
                    registry.add(job.modules);
                    LoadJob nextJob = workflow.getNextJob(registry);
                    if (nextJob != null && forceReload) {
                        runJob(nextJob, true, registry, workflow);
                    } else {
                        _finalProcessStatus.postValue(new Event<>(new WorkflowResult(LoadingStatus.SUCCESS, registry)));
                    }
                } else {
                    _finalProcessStatus.postValue(new Event<>(new WorkflowResult(LoadingStatus.FAILURE, null)));
                }
            }
        });

        loader.startLoading();
    }

    public LiveData<CreateGisBlock.GisResult> loadPhotoMetadata(ConfigurationModule metadataModule, String cacheFolder, String imageFileName) {
        MutableLiveData<CreateGisBlock.GisResult> resultLiveData = new MutableLiveData<>();
        metadataModule.load(singleTaskExecutor, new ModuleLoaderCb() {
            @Override
            public void onFileLoaded(LoadResult result) {
                if (result.errCode == LoadResult.ErrorCode.frozen) {
                    PhotoMeta pm = ((PhotoMetaI) metadataModule).getPhotoMeta();
                    resultLiveData.postValue(new CreateGisBlock.GisResult(pm, cacheFolder, imageFileName));
                } else {
                    resultLiveData.postValue(new CreateGisBlock.GisResult(new Exception("Failed to load metadata, code: " + result.errCode)));
                }
            }
            @Override
            public void onError(LoadResult result) {
                resultLiveData.postValue(new CreateGisBlock.GisResult(new Exception("Error loading metadata: " + result.errorMessage)));
            }
        }, true);
        return resultLiveData;
    }

    public Workflow_I getCurrentWorkflow() {
        return currentWorkflow;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // The setupExecutor has been removed, so we only need to shut down the other executor.
        singleTaskExecutor.shutdownNow();
    }
}
