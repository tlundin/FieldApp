package com.teraim.fieldapp.viewmodels;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import android.util.Log;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.log.LogRepository;

public class LogViewModel extends ViewModel {
    private static final String TAG = "LogViewModel";

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
        Log.d(TAG, "printVariablesToLog clicked");
        GlobalState gs = GlobalState.getInstance();
        if (gs == null) {
            String msg = "Print variables: GlobalState is not available.";
            logRepository.addTextToBothLogs(msg);
            return;
        }
        String diagnosticText = gs.getVariableCache().buildVariableDiagnosticText();
        // Avoid spamming LogCat with the full dump; preview it.
        String preview = diagnosticText;
        if (preview != null && preview.length() > 350) {
            preview = preview.substring(0, 350) + "...";
        }
        Log.d(TAG, "Variable dump preview:\n" + (preview == null ? "(null)" : preview));
        logRepository.addTextToBothLogs(diagnosticText);
    }
}