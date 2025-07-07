package com.teraim.fieldapp.dynamic.templates;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.util.TypedValue; // Needed for TypedValue.applyDimension

import com.google.android.flexbox.FlexboxLayout; // Import FlexboxLayout
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.templates.OnFilterSelectedListener;

import java.util.ArrayList;
import java.util.List;

// Assume your hosting Fragment/Activity implements this interface
// public interface OnFilterSelectedListener {
//     void onFiltersApplied(List<String> newTopFilters, List<String> newAvailableFilters);
// }

public class FilterSelectionDialogFragment extends DialogFragment {

    private static final String ARG_TOP_FILTERS = "top_filters";
    private static final String ARG_AVAILABLE_FILTERS = "available_filters";

    private List<String> currentTopFilters;
    private List<String> currentAvailableFilters;

    // Change LinearLayout to FlexboxLayout
    private FlexboxLayout topFiltersContainer;
    private FlexboxLayout availableFiltersContainer;
    private Button okButton;

    // Listener to send results back to the calling Fragment/Activity
    private OnFilterSelectedListener selectionListener;

    public static FilterSelectionDialogFragment newInstance(
            ArrayList<String> topFilters, ArrayList<String> availableFilters) {
        FilterSelectionDialogFragment fragment = new FilterSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_TOP_FILTERS, topFilters);
        args.putStringArrayList(ARG_AVAILABLE_FILTERS, availableFilters);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentTopFilters = getArguments().getStringArrayList(ARG_TOP_FILTERS);
            currentAvailableFilters = getArguments().getStringArrayList(ARG_AVAILABLE_FILTERS);
        } else {
            currentTopFilters = new ArrayList<>();
            currentAvailableFilters = new ArrayList<>();
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Ensure the host implements the callback interface
        if (getTargetFragment() instanceof OnFilterSelectedListener) {
            selectionListener = (OnFilterSelectedListener) getTargetFragment();
        } else if (context instanceof OnFilterSelectedListener) {
            selectionListener = (OnFilterSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFilterSelectedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_filter_selection, null);

        // Change LinearLayout to FlexboxLayout for casting
        topFiltersContainer = view.findViewById(R.id.dialog_top_filters_container);
        availableFiltersContainer = view.findViewById(R.id.dialog_available_filters_container);
        okButton = view.findViewById(R.id.dialog_ok_button);

        populateContainers(); // Initial population

        okButton.setOnClickListener(v -> {
            if (selectionListener != null) {
                selectionListener.onFiltersApplied(currentTopFilters, currentAvailableFilters);
            }
            dismiss(); // Close the dialog
        });

        builder.setView(view);
        return builder.create();
    }

    // Helper method to create clickable filter "buttons" within the dialog
    private TextView createDialogFilterLabel(Context context, String label) {
        TextView textView = new TextView(context);
        textView.setText(label);
        textView.setBackgroundResource(R.drawable.dialog_filter_label_background); // Custom background
        textView.setTextColor(context.getResources().getColor(android.R.color.black));
        textView.setPadding(16, 8, 16, 8);
        // Use FlexboxLayout.LayoutParams for children of FlexboxLayout
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        params.setMargins(margin, margin, margin, margin);
        textView.setLayoutParams(params);
        return textView;
    }

    // Populate the two FlexboxLayouts with the current filter labels
    private void populateContainers() {
        topFiltersContainer.removeAllViews();
        availableFiltersContainer.removeAllViews();

        for (String label : new ArrayList<>(currentTopFilters)) { // Iterate over a copy to avoid ConcurrentModificationException
            TextView textView = createDialogFilterLabel(getContext(), label);
            textView.setOnClickListener(v -> {
                // Move from top to available
                currentTopFilters.remove(label);
                currentAvailableFilters.add(label);
                populateContainers(); // Refresh the dialog UI
            });
            topFiltersContainer.addView(textView);
        }

        for (String label : new ArrayList<>(currentAvailableFilters)) { // Iterate over a copy
            TextView textView = createDialogFilterLabel(getContext(), label);
            textView.setOnClickListener(v -> {
                // Move from available to top
                currentAvailableFilters.remove(label);
                currentTopFilters.add(label);
                populateContainers(); // Refresh the dialog UI
            });
            availableFiltersContainer.addView(textView);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        selectionListener = null; // Clear listener to prevent memory leaks
    }
}
