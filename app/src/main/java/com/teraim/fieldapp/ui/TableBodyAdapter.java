package com.teraim.fieldapp.ui;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
// TextView might not be directly used here anymore if WF_Table_Row_Recycle handles all cell content.
// import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.teraim.fieldapp.R; // Assuming R is in the main app package
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table_Row_Recycle; // Import for the row widget

import java.util.ArrayList;
import java.util.List;

public class TableBodyAdapter extends RecyclerView.Adapter<TableBodyAdapter.RowViewHolder> {

    private final Context context;
    // Changed data type from List<List<String>> to List<WF_Table_Row_Recycle>
    private final List<Listable> tableRowsDataList;
    private List<String> columnHeaders; // Still kept, might be used by PageWithTable or for other logic
    private final ScrollSyncManager scrollSyncManager;

    public interface ScrollSyncManager {
        void syncScroll(HorizontalScrollView source, int scrollX);
        void addScrollListener(HorizontalScrollView scrollView);
        void removeScrollListener(HorizontalScrollView scrollView);
    }

    // Updated constructor to accept List<WF_Table_Row_Recycle>
    public TableBodyAdapter(Context context, List<Listable> rowsData, List<String> columnHeaders, ScrollSyncManager scrollSyncManager) {
        this.context = context;
        this.tableRowsDataList = rowsData; // Use the new list type
        this.columnHeaders = columnHeaders != null ? columnHeaders : new ArrayList<>();
        this.scrollSyncManager = scrollSyncManager;
    }

    public void setColumnHeaders(List<String> columnHeaders) {
        this.columnHeaders = columnHeaders != null ? columnHeaders : new ArrayList<>();
        // notifyDataSetChanged(); // Potentially notify if header changes affect row rendering significantly
    }

    @NonNull
    @Override
    public RowViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // This inflates the outer shell for each row (the HorizontalScrollView)
        View view = LayoutInflater.from(context).inflate(R.layout.item_table_row, parent, false);
        return new RowViewHolder(view, scrollSyncManager);
    }

    @Override
    public void onBindViewHolder(@NonNull RowViewHolder holder, int position) {
        WF_Table_Row_Recycle rowWidgetItem =(WF_Table_Row_Recycle)tableRowsDataList.get(position);
        // The bind method in ViewHolder now takes the WF_Table_Row_Recycle object
        holder.bind(rowWidgetItem);
    }

    @Override
    public void onViewRecycled(@NonNull RowViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycle(); // Important for removing scroll listeners and cleaning up
    }


    @Override
    public int getItemCount() {
        return tableRowsDataList.size();
    }

    public static class RowViewHolder extends RecyclerView.ViewHolder {
        public HorizontalScrollView rowScrollView;
        LinearLayout cellContainer; // This is the LinearLayout inside item_table_row.xml
        ScrollSyncManager scrollSyncManager;

        public RowViewHolder(@NonNull View itemView, ScrollSyncManager syncManager) {
            super(itemView);
            rowScrollView = itemView.findViewById(R.id.row_scroll_view);
            cellContainer = itemView.findViewById(R.id.row_cell_container_linear_layout);
            this.scrollSyncManager = syncManager;
        }

        // Updated bind method to take WF_Table_Row_Recycle
        public void bind(WF_Table_Row_Recycle rowWidgetItem) {
            cellContainer.removeAllViews(); // Clear previous content from the LinearLayout container

            // Get the pre-configured View from the WF_Table_Row_Recycle widget.
            // This view was inflated from R.layout.table_row in PageWithTable.
            // Assuming WF_Table_Row_Recycle inherits or implements getWidget() from WF_Widget.
            View actualRowContentView = rowWidgetItem.getWidget();

            if (actualRowContentView != null) {
                // If the view is already part of another hierarchy (e.g., from a previous bind),
                // it needs to be removed from its parent before being added to a new one.
                // RecyclerView handles view recycling, so this should generally be okay if views
                // are properly detached/attached, but an explicit check can be safer.
                if (actualRowContentView.getParent() instanceof ViewGroup) {
                    ((ViewGroup) actualRowContentView.getParent()).removeView(actualRowContentView);
                }

                // Add the WF_Table_Row_Recycle's fully formed content view into our cellContainer.
                // The R.layout.table_row (actualRowContentView) should be designed to fill the width appropriately.
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, // Or WRAP_CONTENT if R.layout.table_row handles its width
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                cellContainer.addView(actualRowContentView, params);
            } else {
                Log.e("TableBodyAdapter", "WF_Table_Row_Recycle.getWidget() returned null for item at position " + getAdapterPosition());
            }

            // Reset scroll position for the HorizontalScrollView of this row
            rowScrollView.scrollTo(0,0);
            // Add scroll listener for synchronization
            scrollSyncManager.addScrollListener(rowScrollView);
        }

        public void recycle() {
            // Remove listener to prevent memory leaks and issues with recycled views
            scrollSyncManager.removeScrollListener(rowScrollView);
            // Clear the container if necessary, though RecyclerView should handle detachment
            cellContainer.removeAllViews();
        }
    }
}
