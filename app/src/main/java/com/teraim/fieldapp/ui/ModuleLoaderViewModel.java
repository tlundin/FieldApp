package com.teraim.fieldapp.ui;

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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


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

    // Use a single-thread executor for this sequential task.
    private final ExecutorService singleTaskExecutor = Executors.newSingleThreadExecutor();

    /**
     * Loads photo metadata on a background thread and returns a LiveData for the result.
     * @param metadataModule The configuration module for the photo metadata.
     * @param cacheFolder The path to the cache folder.
     * @param imageFileName The file name of the associated image.
     * @return A LiveData object that will receive the GisResult upon completion.
     */

    public LiveData<CreateGisBlock.GisResult> loadPhotoMetadata(ConfigurationModule metadataModule, String cacheFolder, String imageFileName) {

        MutableLiveData<CreateGisBlock.GisResult> resultLiveData = new MutableLiveData<>();

        metadataModule.load(singleTaskExecutor, new ModuleLoaderCb() {
            @Override
            public void onFileLoaded(LoadResult result) {
                if (result.errCode == LoadResult.ErrorCode.frozen) {
                    PhotoMeta pm = ((PhotoMetaI) metadataModule).getPhotoMeta();
                    // On success, post the GisResult object
                    resultLiveData.postValue(new CreateGisBlock.GisResult(pm, cacheFolder, imageFileName));
                } else {
                    // On failure, post a GisResult with an error
                    resultLiveData.postValue(new CreateGisBlock.GisResult(new Exception("Failed to load metadata, code: " + result.errCode)));
                }
            }

            @Override
            public void onError(LoadResult result) {
                resultLiveData.postValue(new CreateGisBlock.GisResult(new Exception("Error loading metadata: " + result.errorMessage)));
            }
        });

        return resultLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Always shut down executors in onCleared
        singleTaskExecutor.shutdownNow();
    }
}