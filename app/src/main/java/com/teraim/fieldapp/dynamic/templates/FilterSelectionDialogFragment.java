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
import android.util.TypedValue;
import android.graphics.Color;

import com.google.android.flexbox.FlexboxLayout;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.templates.OnFilterSelectedListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Assume your hosting Fragment/Activity implements this interface
// public interface OnFilterSelectedListener {
//     void onFiltersApplied(List<String> newTopFilters, List<String> newAvailableFilters);
// }

public class FilterSelectionDialogFragment extends DialogFragment {

    private static final String ARG_TOP_FILTERS = "top_filters";
    private static final String ARG_AVAILABLE_FILTERS = "available_filters"; // These are the initial family filters
    private static final String ARG_AVAILABLE_COLUMN_FILTERS = "available_column_filters"; // These are all possible column filters

    private List<String> currentTopFilters; // Filters currently selected to be in the top row
    private List<String> initialAvailableFamilyFilters; // The family filters passed in from PageWithTable
    private List<String> allPossibleColumnFilters; // All possible column filters passed in

    private FlexboxLayout topFiltersContainer;
    private FlexboxLayout availableFiltersContainer;
    private Button okButton;

    private OnFilterSelectedListener selectionListener;

    public static FilterSelectionDialogFragment newInstance(
            ArrayList<String> topFilters, ArrayList<String> availableFamilyFilters, // Renamed for clarity
            ArrayList<String> availableColumnFilters) {
        FilterSelectionDialogFragment fragment = new FilterSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_TOP_FILTERS, topFilters);
        args.putStringArrayList(ARG_AVAILABLE_FILTERS, availableFamilyFilters);
        args.putStringArrayList(ARG_AVAILABLE_COLUMN_FILTERS, availableColumnFilters);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentTopFilters = getArguments().getStringArrayList(ARG_TOP_FILTERS);
            initialAvailableFamilyFilters = getArguments().getStringArrayList(ARG_AVAILABLE_FILTERS);
            allPossibleColumnFilters = getArguments().getStringArrayList(ARG_AVAILABLE_COLUMN_FILTERS);
        } else {
            currentTopFilters = new ArrayList<>();
            initialAvailableFamilyFilters = new ArrayList<>();
            allPossibleColumnFilters = new ArrayList<>();
        }
        // Remove "ALL" from currentTopFilters if it was somehow passed, as it's a fixed button not managed by dialog
        currentTopFilters.remove("ALL");
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
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

        topFiltersContainer = view.findViewById(R.id.dialog_top_filters_container);
        availableFiltersContainer = view.findViewById(R.id.dialog_available_filters_container);
        okButton = view.findViewById(R.id.dialog_ok_button);

        populateContainers();

        okButton.setOnClickListener(v -> {
            if (selectionListener != null) {
                // The dialog only manages the currentTopFilters.
                // The host (PageWithTable) will recalculate its available filters
                // based on its master lists and the returned currentTopFilters.
                selectionListener.onFiltersApplied(currentTopFilters, new ArrayList<>()); // Pass an empty list for available filters, host will re-calculate
            }
            dismiss();
        });

        builder.setView(view);
        return builder.create();
    }

    private TextView createDialogFilterLabel(Context context, String label) {
        TextView textView = new TextView(context);
        textView.setText(label);

        // Determine if it's a column filter based on the master list of column filters
        boolean isColumnFilter = allPossibleColumnFilters.contains(label);

        if (isColumnFilter) {
            textView.setBackgroundResource(R.drawable.dialog_column_filter_label_background);
            textView.setTextColor(context.getResources().getColor(android.R.color.white));
        } else {
            textView.setBackgroundResource(R.drawable.dialog_filter_label_background);
            textView.setTextColor(context.getResources().getColor(android.R.color.black));
        }

        textView.setPadding(16, 8, 16, 8);
        FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(
                FlexboxLayout.LayoutParams.WRAP_CONTENT,
                FlexboxLayout.LayoutParams.WRAP_CONTENT
        );
        int margin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources().getDisplayMetrics());
        params.setMargins(margin, margin, margin, margin);
        textView.setLayoutParams(params);
        return textView;
    }

    private void populateContainers() {
        topFiltersContainer.removeAllViews();
        availableFiltersContainer.removeAllViews();

        // Use a Set for efficient lookup of filters currently in the top row
        Set<String> currentTopFiltersSet = new HashSet<>(currentTopFilters);

        // Populate top filters
        for (String label : new ArrayList<>(currentTopFilters)) { // Iterate over a copy
            if ("ALL".equals(label)) continue; // Do not display "ALL" in the dialog
            TextView textView = createDialogFilterLabel(getContext(), label);
            textView.setOnClickListener(v -> {
                currentTopFilters.remove(label); // Remove from selected top filters
                populateContainers(); // Refresh the dialog UI
            });
            topFiltersContainer.addView(textView);
        }

        // Populate available filters:
        // Combine all potential available filters (column and family)
        // and add only those NOT currently in currentTopFilters.

        List<String> allPotentialAvailableFiltersToDisplay = new ArrayList<>();

        // Add all possible column filters that are NOT currently in top filters
        for (String label : allPossibleColumnFilters) {
            if (!currentTopFiltersSet.contains(label) && !"ALL".equals(label)) {
                allPotentialAvailableFiltersToDisplay.add(label);
            }
        }

        // Add all initial available family filters that are NOT currently in top filters
        for (String label : initialAvailableFamilyFilters) {
            if (!currentTopFiltersSet.contains(label) && !"ALL".equals(label)) {
                allPotentialAvailableFiltersToDisplay.add(label);
            }
        }

        // Use a Set to ensure no duplicates are added to the display list
        Set<String> displayedAvailableFilters = new HashSet<>();

        for (String label : allPotentialAvailableFiltersToDisplay) {
            if (!displayedAvailableFilters.contains(label)) { // Prevent duplicates in the display list
                TextView textView = createDialogFilterLabel(getContext(), label);
                textView.setOnClickListener(v -> {
                    currentTopFilters.add(label); // Add to selected top filters
                    populateContainers(); // Refresh the dialog UI
                });
                availableFiltersContainer.addView(textView);
                displayedAvailableFilters.add(label); // Mark as added to display
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        selectionListener = null; // Clear listener to prevent memory leaks
    }
}
