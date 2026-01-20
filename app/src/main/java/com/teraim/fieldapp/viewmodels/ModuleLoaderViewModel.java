package com.teraim.fieldapp.viewmodels;

import android.graphics.Color;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.teraim.fieldapp.dynamic.blocks.CreateGisBlock;
import com.teraim.fieldapp.dynamic.types.Event;
import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
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
import com.teraim.fieldapp.log.LogRepository;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModuleLoaderViewModel extends ViewModel {
    private static final String TAG = "ModuleLoaderViewModel";


    /**
     * A helper class to wrap the final result of a workflow execution.
     * The record provides immutable data.
     */
    public record WorkflowResult(LoadingStatus status, ModuleRegistry registry, WF_Context myContext) {}

    // --- 1. LIVE DATA FOR PERSISTENT STATE ---
    // Observed by UI that needs to reflect the current state (e.g., progress bars).
    // Multiple observers can safely listen to this.
    private final MutableLiveData<WorkflowResult> _workflowState = new MutableLiveData<>();
    public final LiveData<WorkflowResult> workflowState = _workflowState;

    // --- 2. LIVE DATA FOR ONE-TIME SUCCESS EVENT ---
    // Observed only by components that need to perform a single, non-repeatable action on success.
    private final MutableLiveData<Event<WorkflowResult>> _onSuccessEvent = new MutableLiveData<>();
    public final LiveData<Event<WorkflowResult>> onSuccessEvent = _onSuccessEvent;

    // --- LiveData for progress text remains the same ---
    private final MutableLiveData<String> _progressText = new MutableLiveData<>("");
    public final LiveData<String> progressText = _progressText;

    private final ExecutorService singleTaskExecutor = Executors.newSingleThreadExecutor();
    private Workflow_I currentWorkflow;
    private WF_Context myContext;
    public void execute(Workflow_I workflow, boolean forceReload, WF_Context myContext) {
        // Check the STATE LiveData for a running process
        this.myContext=myContext;
        if (_workflowState.getValue() != null && _workflowState.getValue().status() == LoadingStatus.LOADING) {
            Log.w("ModuleLoaderViewModel", "Execution request ignored: a workflow is already running.");
            LogRepository.getInstance().addColorText("Execution request ignored: a workflow is already running.", Color.parseColor("#E6E6FA"));
            return;
        }
        // Post the initial loading status to the STATE LiveData
        _workflowState.postValue(new WorkflowResult(LoadingStatus.LOADING, null,myContext));

        final ModuleRegistry registry = new ModuleRegistry();
        this.currentWorkflow = workflow;

        LoadJob initialJob = workflow.getInitialJob();
        if (initialJob == null || initialJob.modules.isEmpty()) {
            // Handle early success: post to both STATE and EVENT
            LogRepository.getInstance().addColorText("Load ended: no initial job", Color.parseColor("#E6E6FA"));
            WorkflowResult successResult = new WorkflowResult(LoadingStatus.SUCCESS, registry,myContext);
            _workflowState.setValue(successResult);
            _onSuccessEvent.setValue(new Event<>(successResult));
            return;
        }

        runJob(initialJob, forceReload, registry, workflow);
    }

    private void runJob(final LoadJob job, final boolean forceReload, final ModuleRegistry registry, final Workflow_I workflow) {
        StatefulModuleLoader loader = new StatefulModuleLoader(job.modules, job.stage, forceReload);

        // IMPORTANT NOTE on observeForever:
        // This pattern is risky. If the removeObserver call is ever missed due to an error,
        // it will cause a memory leak. The code below correctly removes it, but a safer,
        // more modern approach would involve LiveData Transformations or Kotlin Flows.
        loader.progressText.observeForever(_progressText::postValue);
        loader.loadingStatus.observeForever(new Observer<>() {
            @Override
            public void onChanged(LoadCompletionEvent completionEvent) {

                if (completionEvent.status == LoadingStatus.SUCCESS) {
                    registry.add(job.modules);
                    LogRepository.getInstance().addColorText("ModuleLoader succeeded. Loaded " + job.modules.size() + " modules.", Color.parseColor("#E6E6FA"));
                    LoadJob nextJob = workflow.getNextJob(registry);
                    if (nextJob != null && forceReload) {
                        LogRepository.getInstance().addColorText("ModuleLoader: continue - next job has " + nextJob.modules.size() + " modules", Color.parseColor("#E6E6FA"));
                        runJob(nextJob, true, registry, workflow);
                    } else {
                        // This is the FINAL success. Post to both STATE and EVENT streams.
                        LogRepository.getInstance().addColorText("ViewModel has finished loading.", Color.parseColor("#E6E6FA"));
                        Log.d(TAG, "Final SUCCESS. Posting state and event.");
                        WorkflowResult successResult = new WorkflowResult(LoadingStatus.SUCCESS, registry,myContext);
                        _workflowState.postValue(successResult);
                        _onSuccessEvent.postValue(new Event<>(successResult));
                        // Cleanup is crucial when using observeForever
                        loader.loadingStatus.removeObserver(this);
                        loader.progressText.removeObserver(_progressText::postValue);
                    }
                } else {
                    // On failure, we only update the STATE.
                    Log.d(TAG, "FAILURE. Posting state.");
                    LogRepository.getInstance().addCriticalText("ModuleLoader failed.");
                    _workflowState.postValue(new WorkflowResult(LoadingStatus.FAILURE, null,null));
                    // Cleanup is crucial when using observeForever
                    loader.loadingStatus.removeObserver(this);
                    loader.progressText.removeObserver(_progressText::postValue);
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
                resultLiveData.postValue(new CreateGisBlock.GisResult(new Exception("Error loading metadata " + result.errorMessage)));
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
