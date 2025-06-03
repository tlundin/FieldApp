package com.teraim.fieldapp.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView; // Added for error view

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat; // Added for color
import androidx.fragment.app.DialogFragment;

import com.teraim.fieldapp.R;

public class FilterDialogFragment extends DialogFragment {

    private static final String ARG_DIALOG_TAG = "dialog_tag";
    private static final String ARG_TOP_OFFSET = "top_offset"; // New argument for top offset

    private View popupContentLayout;
    private int topOffset = 0; // To store the offset

    public static FilterDialogFragment newInstance(String dialogTag, int topOffset) {
        FilterDialogFragment fragment = new FilterDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DIALOG_TAG, dialogTag);
        args.putInt(ARG_TOP_OFFSET, topOffset); // Store the top offset
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Call this method before showing the dialog to set the content view.
     * @param contentLayout The LinearLayout containing the filter options.
     */
    public void setPopupContent(View contentLayout) {
        this.popupContentLayout = contentLayout;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            topOffset = getArguments().getInt(ARG_TOP_OFFSET, 0);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (popupContentLayout == null) {
            TextView errorView = new TextView(requireContext());
            errorView.setText("Filter content not available.");
            errorView.setPadding(32,32,32,32);
            return errorView;
        }

        if (popupContentLayout.getParent() instanceof ViewGroup) {
            ((ViewGroup) popupContentLayout.getParent()).removeView(popupContentLayout);
        }

        FrameLayout rootContainer = new FrameLayout(requireContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, // Dialog width will be set in onStart
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        rootContainer.addView(popupContentLayout, params);
        // Set a background for the dialog fragment's view area if the passed layout doesn't have one
        // Consider using a theme attribute for better light/dark theme support
        rootContainer.setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white));

        return rootContainer;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            Window window = dialog.getWindow();
            if (window != null) {
                WindowManager.LayoutParams params = window.getAttributes();
                params.gravity = Gravity.TOP | Gravity.END; // Position to top-right

                // Apply the top offset
                params.y = topOffset;

                params.width = getResources().getDimensionPixelSize(R.dimen.filter_dialog_width);
                params.height = WindowManager.LayoutParams.MATCH_PARENT;

                window.setAttributes(params);
                window.setWindowAnimations(R.style.RightSlideAnimation);
            }
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        assert getParentFragment() != null;
        ((FilterDialogDismissListener) getParentFragment()).onFilterDialogDismissed(getTag());

    }
}
