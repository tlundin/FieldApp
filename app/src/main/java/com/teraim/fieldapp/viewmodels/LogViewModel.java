package com.teraim.fieldapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.log.LogRepository;

public class LogViewModel extends ViewModel {

    private final LogRepository logRepository;

    public LogViewModel() {
        // Get the singleton instance of our data repository
        this.logRepository = LogRepository.getInstance();
    }

    // Expose the LiveData from the repository to the UI
    public LiveData<CharSequence> getLogContent() {
        return logRepository.getLogContent();
    }

    // Expose the new LiveData from the repository to the UI
    public LiveData<Boolean> getHasNewCriticalEvent() {
        return logRepository.getHasNewCriticalEvent();
    }

    // Expose the "consume" method
    public void consumeNewCriticalEventFlag() {
        logRepository.consumeNewCriticalEventFlag();
    }

    // You can also add methods here to trigger actions on the repository if needed
    public void clearLog() {
        logRepository.clear();
    }

    /** Appends a snapshot of VariableCache variables in the current DB_Context to the log. */
    public void printVariablesToLog() {
        GlobalState gs = GlobalState.getInstance();
        if (gs == null) {
            logRepository.addText("Print variables: GlobalState is not available.");
            return;
        }
        logRepository.addText(gs.getVariableCache().buildVariableDiagnosticText());
    }
}