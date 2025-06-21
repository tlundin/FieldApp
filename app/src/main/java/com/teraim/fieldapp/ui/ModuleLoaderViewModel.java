package com.teraim.fieldapp.ui;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.teraim.fieldapp.dynamic.blocks.CreateGisBlock;
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

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModuleLoaderViewModel extends ViewModel {

    /**
     * A helper class to wrap the final result of a workflow execution.
     */
    public static class WorkflowResult {
        public final LoadingStatus status;
        public final ModuleRegistry registry;

        public WorkflowResult(LoadingStatus status, ModuleRegistry registry) {
            this.status = status;
            this.registry = registry;
        }
    }

    // LiveData exposed to the UI layer
    private final MutableLiveData<WorkflowResult> _finalProcessStatus = new MutableLiveData<>();
    public final LiveData<WorkflowResult> finalProcessStatus = _finalProcessStatus;

    private final MutableLiveData<String> _progressText = new MutableLiveData<>("");
    public final LiveData<String> progressText = _progressText;

    // Executors for background tasks
    private final ExecutorService setupExecutor = Executors.newSingleThreadExecutor();
    private final ExecutorService singleTaskExecutor = Executors.newSingleThreadExecutor();

    // Handler to post tasks back to the main UI thread
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Field to hold the currently executing workflow
    private Workflow_I currentWorkflow;

    /**
     * Public getter for the currently executing workflow.
     * Allows observers to check which workflow has completed.
     */
    public Workflow_I getCurrentWorkflow() {
        return currentWorkflow;
    }

    /**
     * Executes a workflow. Handles pre-checking for cached modules and orchestrates
     * the entire loading process off the main thread.
     *
     * @param workflow The workflow to execute.
     * @param forceReload If true, all modules will be fetched from the server.
     */
    public void execute(Workflow_I workflow, boolean forceReload) {
        if (_finalProcessStatus.getValue() != null && _finalProcessStatus.getValue().status == LoadingStatus.LOADING) {
            Log.w("ModuleLoaderViewModel", "Execution request ignored: a workflow is already running.");
            return;
        }

        // Set the current workflow so observers can identify it
        this.currentWorkflow = workflow;

        _finalProcessStatus.postValue(new WorkflowResult(LoadingStatus.LOADING, null));

        setupExecutor.execute(() -> {
            final ModuleRegistry registry = new ModuleRegistry();
            LoadJob initialJob = workflow.getInitialJob();

            if (initialJob == null || initialJob.modules.isEmpty()) {
                mainHandler.post(() -> _finalProcessStatus.postValue(new WorkflowResult(LoadingStatus.SUCCESS, registry)));
                return;
            }

            final ArrayList<ConfigurationModule> modulesToDownload = new ArrayList<>();

            if (!forceReload) {
                for (ConfigurationModule module : initialJob.modules) {
                    if (module.thawSynchronously().errCode == LoadResult.ErrorCode.thawed) {
                        registry.add(module);
                    } else {
                        modulesToDownload.add(module);
                    }
                }
            } else {
                modulesToDownload.addAll(initialJob.modules);
            }

            mainHandler.post(() -> {
                if (modulesToDownload.isEmpty()) {
                    Log.d("ViewModel", "All modules were successfully loaded from cache. Workflow complete.");
                    _finalProcessStatus.postValue(new WorkflowResult(LoadingStatus.SUCCESS, registry));
                } else {
                    Log.d("ViewModel", "Starting network download for " + modulesToDownload.size() + " modules.");
                    LoadJob downloadJob = new LoadJob(initialJob.stage, modulesToDownload);
                    runJob(downloadJob, true, registry, workflow);
                }
            });
        });
    }

    /**
     * Runs a specific job in the workflow. This method MUST be called on the main thread.
     */
    private void runJob(final LoadJob job, final boolean forceReload, final ModuleRegistry registry, final Workflow_I workflow) {
        StatefulModuleLoader loader = new StatefulModuleLoader(job.modules, job.stage, forceReload);

        loader.progressText.observeForever(_progressText::postValue);
        loader.loadingStatus.observeForever(new Observer<LoadCompletionEvent>() {
            @Override
            public void onChanged(LoadCompletionEvent completionEvent) {
                loader.loadingStatus.removeObserver(this);
                loader.progressText.removeObserver(_progressText::postValue);

                if (completionEvent.status == LoadingStatus.SUCCESS) {
                    registry.add(job.modules);
                    LoadJob nextJob = workflow.getNextJob(registry);
                    if (nextJob != null) {
                        runJob(nextJob, true, registry, workflow);
                    } else {
                        _finalProcessStatus.postValue(new WorkflowResult(LoadingStatus.SUCCESS, registry));
                    }
                } else {
                    _finalProcessStatus.postValue(new WorkflowResult(LoadingStatus.FAILURE, null));
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

    @Override
    protected void onCleared() {
        super.onCleared();
        setupExecutor.shutdownNow();
        singleTaskExecutor.shutdownNow();
    }
}
