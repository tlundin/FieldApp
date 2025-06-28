package com.teraim.fieldapp.ui;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.viewmodels.LogViewModel;

public class LogDialogFragment extends DialogFragment {

    private LogViewModel logViewModel;
    private TextView logTextView;
    private ScrollView scrollView;

    // Best practice: Use a static newInstance method to create the fragment
    public static LogDialogFragment newInstance() {
        return new LogDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Get the Activity-scoped ViewModel. This ensures we get the same instance
        // as the hosting Activity and any other fragments.
        logViewModel = new ViewModelProvider(requireActivity()).get(LogViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.dialog_log, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find all the views from the inflated layout
        logTextView = view.findViewById(R.id.log_text_view);
        scrollView = view.findViewById(R.id.log_scroll_view);
        AppCompatImageButton closeButton = view.findViewById(R.id.button_close);
        Button clearButton = view.findViewById(R.id.button_clear);
        Button scrollDownButton = view.findViewById(R.id.button_scroll_down);
        Button backupDbButton = view.findViewById(R.id.button_backup_db);
        Button crashAppButton = view.findViewById(R.id.button_crash_app);

        // --- THE MAGIC: OBSERVE THE VIEWMODEL ---
        // No more setOutputView() or draw()! The UI updates automatically.
        logViewModel.getLogContent().observe(getViewLifecycleOwner(), charSequence -> {
            logTextView.setText(charSequence);
            // Optional: Automatically scroll down when new content is added
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });

        // --- SETUP BUTTON LISTENERS ---
        closeButton.setOnClickListener(v -> dismiss());

        // The "Clear" button now calls a method on the ViewModel
        clearButton.setOnClickListener(v -> logViewModel.clearLog());

        scrollDownButton.setOnClickListener(v -> scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN)));

        // For actions outside the ViewModel's scope, we delegate back to the Activity.
        // This keeps the DialogFragment decoupled from things like BackupManager.
        backupDbButton.setOnClickListener(v -> {
            if (getActivity() instanceof LogDialogListener) {
                ((LogDialogListener) getActivity()).onBackupDatabaseClicked();
            }
        });

        crashAppButton.setOnClickListener(v -> {
            if (getActivity() instanceof LogDialogListener) {
                ((LogDialogListener) getActivity()).onCrashAppClicked();
            }
        });
    }

    // This makes the dialog full-screen
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = ViewGroup.LayoutParams.MATCH_PARENT;
            int height = ViewGroup.LayoutParams.MATCH_PARENT;
            Window window = dialog.getWindow();
            if (window != null) {
                window.setLayout(width, height);
            }
        }
    }

    // --- INTERFACE FOR DELEGATION ---
    // The activity will implement this interface to handle button clicks.
    public interface LogDialogListener {
        void onBackupDatabaseClicked();
        void onCrashAppClicked();
    }
}