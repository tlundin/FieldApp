package com.teraim.fieldapp.loadermodule;

import android.graphics.Color;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LogRepository;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A worker class that executes a single load job (a list of modules for a specific stage).
 * It manages its own thread pool and reports its progress and final completion status
 * via LiveData objects.
 */
public class StatefulModuleLoader implements ModuleLoaderCb {

    private final LoadStage stage;
    private final List<ConfigurationModule> modules;
    private final ExecutorService executor;
    private final AtomicInteger modulesInProgress;

    // LiveData for granular, real-time progress text updates.
    public final MutableLiveData<String> progressText = new MutableLiveData<>();

    // LiveData for the FINAL completion event of this specific job.
    // This is the corrected line with the proper generic type.
    public final MutableLiveData<LoadCompletionEvent> loadingStatus = new MutableLiveData<>();
    private final boolean forceReload;

    /**
     * Creates a loader for a specific job.
     *
     * @param modules     The list of ConfigurationModules to load for this job.
     * @param stage       The identifier for this job (e.g., FILES, DATABASES).
     * @param forceReload
     */
    public StatefulModuleLoader(List<ConfigurationModule> modules, LoadStage stage, boolean forceReload) {
        this.stage = stage;
        this.modules = modules;
        this.forceReload = forceReload;
        final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
        Log.d("StatefulModuleLoader","number of threads: "+NUMBER_OF_CORES);
        this.executor = Executors.newFixedThreadPool(Math.max(2, NUMBER_OF_CORES));
        this.modulesInProgress = new AtomicInteger(modules.isEmpty() ? 0 : modules.size());

        // NEW: Post a simple, non-blocking initial message instead.
        // This requires no loops and is extremely fast.
        progressText.postValue("Preparing stage: " + stage + " (" + modules.size() + " modules)...");
    }

    /**
     * Starts the loading process for this job.
     */
    public void startLoading() {
        if (modules.isEmpty()) {
            // If there are no modules, this job is instantly successful.
            LogRepository.getInstance().addColorText("Immediate exit - no modules to load", Color.parseColor("#E6E6FA"));

            loadingStatus.setValue(new LoadCompletionEvent(LoadingStatus.SUCCESS, this.stage));
            return;
        }

        progressText.setValue("Starting stage: " + stage + "...");
        for (ConfigurationModule module : modules) {
            // The loader acts as the callback for each module's async operations.
            LogRepository.getInstance().addColorText("Loading module: " + module.getLabel() + "...", Color.parseColor("#E6E6FA"));
            module.load(executor, this, forceReload);
        }
    }

    @Override
    public void onFileLoaded(LoadResult result) {
        ConfigurationModule module = result.module;
        module.state.postValue(ConfigurationModule.ModuleLoadState.FROZEN);
        LogRepository.getInstance().addColorText("Loading state changed: " + module.getLabel() + "...["+result.errCode+"]", Color.parseColor("#E6E6FA"));

        switch (result.errCode) {
            case loaded:
            case frozen:
            case thawed:
                checkIfAllDone();
                break;
            case reloadDependant:
                Log.d("StatefulModuleLoader", "reloadDependant");
            default:
                System.out.println("Module [" + module.getLabel() + "] finished with unhandled code: " + result.errCode);
                checkIfAllDone();
                break;
        }
    }

    @Override
    public void onError(LoadResult result) {
        ConfigurationModule module = result.module;
        System.err.println("Error for module [" + module.getLabel() + "]: " + result.errCode + " - " + result.errorMessage);
        module.state.postValue(ConfigurationModule.ModuleLoadState.ERROR);
        // A module has failed its lifecycle.
        checkIfAllDone();
    }

    /**
     * Called after each module completes (successfully or not). When all modules are done,
     * it determines the final status of this job and posts the result.
     */
    private void checkIfAllDone() {
        // This is the old, incorrect position for the update
        // updateProgress();
        Log.d("StatefulModuleLoader", "checkIfAllDone"+" modules in progress "+modulesInProgress.get());

        if (modulesInProgress.decrementAndGet() == 0) {
            // --- This block now executes only for the very last module ---

            // 1. First, update the progress text to its FINAL state.
            // This queues the last UI text update on the main thread.
            updateProgress(modulesInProgress);

            // 2. Then, determine the overall result.
            boolean hasErrors = modules.stream()
                    .anyMatch(m -> m.state.getValue() == ConfigurationModule.ModuleLoadState.ERROR);

            // 3. Finally, send the completion signal.
            // This will be queued on the main thread AFTER the final progress update.
            if (hasErrors) {
                loadingStatus.postValue(new LoadCompletionEvent(LoadingStatus.FAILURE, this.stage));
                LogRepository.getInstance().addColorText("Module load failed", Color.parseColor("#E6E6FA"));

            } else {
                loadingStatus.postValue(new LoadCompletionEvent(LoadingStatus.SUCCESS, this.stage));
                LogRepository.getInstance().addColorText("Module load succeeded", Color.parseColor("#E6E6FA"));
            }

            // Clean up this job's resources.
            executor.shutdownNow();
        } else {
            // If we are not done yet, we can still update the progress.
            updateProgress(modulesInProgress);
        }
    }
    /**
     * Generates and posts a summary string for the UI to display progress.
     */
    private void updateProgress(AtomicInteger modulesInProgress) {
        Log.d("StatefulModuleLoader", "updateProgress");
        LogRepository.getInstance().addColorText("Modules in progress ["+modulesInProgress.get()+"]", Color.parseColor("#E6E6FA"));
        StringBuilder sb = new StringBuilder();
        long completed = modules.stream().filter(m ->
                m.state.getValue() != ConfigurationModule.ModuleLoadState.INITIAL &&
                        m.state.getValue() != ConfigurationModule.ModuleLoadState.LOADING).count();

        sb.append("Stage: ").append(stage).append(" (")
                .append(completed).append("/").append(modules.size()).append(")\n\n");

        for (ConfigurationModule module : modules) {
            sb.append(module.getLabel())
                    .append(": ")
                    .append(module.state.getValue())
                    .append("\n");
        }

        progressText.postValue(sb.toString());
    }
}