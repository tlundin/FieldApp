package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
// import android.widget.LinearLayout; // Not directly used as a field in this version
import android.widget.TableRow;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.templates.PageWithTable; // Required for ColumnDefinition and HEADER_ROW_ID
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WF_Table_Row_Recycle extends WF_Widget implements Listable,Comparable<Listable> {
	private final PageWithTable myWfTable;
	private List<String> myRow;
	private List<WF_Cell> myColumns; // Data cells
	private final WF_Context myContext;
	private final TextView headerT_RowLabel; // This is the TextView for the row label (first column in data rows) or the top-left cell in the header row
	private final String id;

	private List<View> headerCellViews = new ArrayList<>(); // For the main header row's dynamic cells

	// New: Reference to the actual TableRow that holds header cells if this is the headerRow
	private TableRow actualHeaderCellsContainer;

	public WF_Table_Row_Recycle(PageWithTable myWfTable, String id, View v, WF_Context ctx, boolean isVisible) {
		super(id,v,isVisible,ctx); // v is the inflated R.layout.table_row or R.layout.header_table_row
		myColumns = new ArrayList<>();
		myContext = ctx;
		headerT_RowLabel = v.findViewById(R.id.headerT); // TextView within the inflated row layout
		this.id=id;
		this.myWfTable = myWfTable;

		if (this.id.equals(PageWithTable.HEADER_ROW_ID)) {
			// If this is the main header row, its getWidget() is the LinearLayout from header_table_row.xml
			// We need to find the TableRow within it that will actually hold the header cells.
			if (v != null) {
				actualHeaderCellsContainer = v.findViewById(R.id.header_content_row);
				if (actualHeaderCellsContainer == null) {
					Log.e("WF_Table_Row_Recycle", "Critical: TableRow with R.id.header_content_row not found in R.layout.header_table_row.xml for HEADER_ROW_ID");
				}
			}
		} else {
			// For data rows, getWidget() is already the TableRow from R.layout.table_row.xml
			if (v instanceof TableRow) {
				actualHeaderCellsContainer = (TableRow) v;
			} else {
				// If v is not a TableRow (e.g. if R.layout.table_row was changed), this could be an issue.
				// However, getWidget() from super class WF_Widget returns 'v', so it should be the TableRow.
				Log.e("WF_Table_Row_Recycle", "Data row's main view is not a TableRow for ID: " + id + ". View type: " + (v != null ? v.getClass().getName() : "null"));
			}
		}
	}

	public void addEntryField(List<String> row) {
		myRow=row;
		String label = getLabel();
		if (label==null) {
			Log.e("vortex","label null for row with id "+id);
			label="*null*";
		}
		if (headerT_RowLabel != null) { // This is the static TextView in R.layout.table_row or R.layout.header_table_row
			headerT_RowLabel.setText(label);
		} else {
			Log.d("WF_Table_Row_Recycle", "headerT_RowLabel TextView is null or not applicable for row id " + id);
		}
	}

	public void addHeaderCell(String label, String backgroundColor, String textColor, int width, final int columnIndex, PageWithTable.ColumnDefinition definition) {
		if (myContext == null || myContext.getContext() == null) {
			Log.e("WF_Table_Row_Recycle", "Context is null in addHeaderCell.");
			return;
		}
		if (!this.id.equals(PageWithTable.HEADER_ROW_ID) || actualHeaderCellsContainer == null) {
			Log.e("WF_Table_Row_Recycle", "addHeaderCell called on a non-header row or actualHeaderCellsContainer is null. ID: " + this.id);
			return;
		}

		View headerC = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_header, actualHeaderCellsContainer, false);
		TextView headerTV = headerC.findViewById(R.id.headerT);
		if (headerTV == null) {
			Log.e("WF_Table_Row_Recycle", "TextView R.id.headerT not found in R.layout.cell_field_header.");
			return;
		}
		headerTV.setText(label);


		// Apply initial/default colors and text size from the definition
		Context ctx = myContext.getContext();

		headerC.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent)); // Default

		headerTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, definition.textSizeSp);
		headerTV.setTypeface(null, Typeface.NORMAL);

		TableRow.LayoutParams params = new TableRow.LayoutParams(width, TableRow.LayoutParams.WRAP_CONTENT);
		headerC.setLayoutParams(params);

		actualHeaderCellsContainer.addView(headerC); // Add to the specific TableRow for headers
		headerCellViews.add(headerC);

		headerC.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				myWfTable.onColumnHeaderClicked(columnIndex);
			}
		});
	}

	public void updateHeaderAppearance(List<PageWithTable.ColumnDefinition> globalColumnDefinitions, int focusedColumnIndex) {
		if (!this.id.equals(PageWithTable.HEADER_ROW_ID)) {
			// This method is specifically for the headerRow instance.
			return;
		}
		if (myContext == null || myContext.getContext() == null || actualHeaderCellsContainer == null) {
			Log.e("WF_Table_Row_Recycle", "Context or actualHeaderCellsContainer is null in updateHeaderAppearance.");
			return;
		}

		if (headerCellViews.size() != globalColumnDefinitions.size()) {
			Log.w("WF_Table_Row_Recycle", "Header style update: Mismatch headerCellViews (" + headerCellViews.size() + ") vs globalColumnDefinitions (" + globalColumnDefinitions.size() + "). Styles might be incorrect if columns were added/removed without full header rebuild.");
		}

		Context ctx = myContext.getContext();
		for (int i = 0; i < headerCellViews.size(); i++) {
			if (i >= globalColumnDefinitions.size()) {
				Log.w("WF_Table_Row_Recycle", "Ran out of column definitions while updating header appearance for cell index " + i);
				break; // Safety break if lists are out of sync
			}

			View headerCellView = headerCellViews.get(i);
			TextView headerTV = headerCellView.findViewById(R.id.headerT); // Assuming R.id.headerT is the TextView in cell_field_header
			PageWithTable.ColumnDefinition colDef = globalColumnDefinitions.get(i);

			if (headerTV == null) {
				Log.e("WF_Table_Row_Recycle", "headerTV is null for header cell at index " + i);
				continue;
			}

			headerCellView.setVisibility(colDef.isVisible ? View.VISIBLE : View.GONE);

			if (colDef.isVisible) {
				if (i == focusedColumnIndex) {
					Log.d("PageWithTable", "Applying FOCUSED style to header TextView: " + colDef.label);
					int selectedBgColor = ContextCompat.getColor(ctx, R.color.selected_header_background);
					int selectedTextColorVal = ContextCompat.getColor(ctx, R.color.selected_header_text_color);

					// Apply background to the TextView itself
					headerTV.setBackgroundColor(selectedBgColor);
					headerTV.setTextColor(selectedTextColorVal);
					headerTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, colDef.textSizeSp);
					//headerTV.setTypeface(null, Typeface.BOLD);
					headerCellView.requestLayout();
					Log.d("PageWithTable", "Focused - Applied to TextView - BG: #" + Integer.toHexString(selectedBgColor) + ", Text: #" + Integer.toHexString(selectedTextColorVal));

					// Optional: If you still want the LinearLayout container to have a different background when focused
					// headerCellView.setBackgroundColor(some_other_color_for_container_focus);

				} else {
					Log.d("PageWithTable", "Applying NORMAL style to header TextView: " + colDef.label);
					// String originalContainerBgColorStr = (String) headerCellView.getTag(R.id.tag_original_bg_color); // For LinearLayout
					// Restore TextView's original background (the shape drawable)
					headerTV.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent));

					// Restore TextView's text color
					headerTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, colDef.textSizeSp);
					headerTV.setTypeface(null, Typeface.NORMAL);
					headerCellView.requestLayout();

				}
			}
		}
		if (actualHeaderCellsContainer != null) {
			actualHeaderCellsContainer.requestLayout();
		}
	}

	public void clearCells() {
		TableRow targetRowForCells = null;

		if (this.id.equals(PageWithTable.HEADER_ROW_ID)) {
			targetRowForCells = actualHeaderCellsContainer; // This is the R.id.header_content_row
			if (targetRowForCells != null && headerCellViews != null) {
				for (View headerCellView : headerCellViews) {
					if (headerCellView != null) {
						targetRowForCells.removeView(headerCellView);
					}
				}
				headerCellViews.clear();
			}
		} else { // For data rows
			if (getWidget() instanceof TableRow) {
				targetRowForCells = (TableRow) getWidget();
			} else if (actualHeaderCellsContainer != null) { // Fallback if getWidget isn't TableRow but actualHeaderCellsContainer is
				targetRowForCells = actualHeaderCellsContainer;
			}
		}

		// Clear data cells (myColumns) if targetRowForCells is valid
		if (targetRowForCells != null && myColumns != null) {
			for (int i = myColumns.size() - 1; i >= 0; i--) {
				WF_Cell cell = myColumns.get(i);
				if (cell != null && cell.getWidget() != null) {
					targetRowForCells.removeView(cell.getWidget());
				}
			}
			myColumns.clear();
		} else if (myColumns != null) {
			// Fallback if no TableRow found, just clear the list
			myColumns.clear();
		}
		// The static R.id.headerT (row label or top-left header cell) is part of the initially inflated layout
		// and is not in myColumns or headerCellViews, so it won't be removed by this logic.
	}

	public void updateCellVisibilities(List<PageWithTable.ColumnDefinition> globalColumnDefinitions) {
		if (this.id.equals(PageWithTable.HEADER_ROW_ID)) {
			// Header visibility is handled by updateHeaderAppearance
			return;
		}
		if (myColumns == null || globalColumnDefinitions == null) {
			Log.w("WF_Table_Row_Recycle", "DataRow: Cannot update cell visibilities: myColumns or globalColumnDefinitions is null.");
			return;
		}

		TableRow targetRow = null;
		if (getWidget() instanceof TableRow) {
			targetRow = (TableRow) getWidget();
		} else if (actualHeaderCellsContainer != null) { // Should be set for data rows too
			targetRow = actualHeaderCellsContainer;
		}

		if (myColumns.size() != globalColumnDefinitions.size()) {
			Log.w("WF_Table_Row_Recycle", "DataRow: Mismatch myColumns ("+myColumns.size()+") vs globalColumnDefinitions ("+globalColumnDefinitions.size()+") for row ID "+this.id+". Visibility update might be incomplete.");
		}

		for (int i = 0; i < myColumns.size(); i++) {
			WF_Cell cellWidget = myColumns.get(i);
			if (cellWidget != null && cellWidget.getWidget() != null) {
				if (i < globalColumnDefinitions.size()) {
					PageWithTable.ColumnDefinition colDef = globalColumnDefinitions.get(i);
					cellWidget.getWidget().setVisibility(colDef.isVisible ? View.VISIBLE : View.GONE);
				} else {
					// This row has more cells than defined columns, hide extras
					Log.w("WF_Table_Row_Recycle", "DataRow ID "+this.id+": Hiding extra cell at index " + i);
					cellWidget.getWidget().setVisibility(View.GONE);
				}
			}
		}
		if (targetRow != null) {
			targetRow.requestLayout();
		} else if (getWidget() != null) { // Fallback
			getWidget().requestLayout();
		}
	}

	public void addCell(String colHeader, String colKey, Map<String,String> columnKeyHash, String type, int width, String backgroundColor, String textColor, WF_Cell.CellType cellType) {
		//Log.d("bino","addCell called with celltype "+cellType+" for colkey "+colKey);
		if (cellType== WF_Cell.CellType.Aggregate) {
			Log.d("bino","don't add cell for agg");
			return;
		}
		if (myColumns==null) myColumns = new ArrayList<>();
		WF_Cell widget;

		if (al == null) {
			Log.e("WF_Table_Row_Recycle", "VariableConfiguration 'al' is null in addCell for row id "+id+", colKey "+colKey);
			return;
		}

		TableRow targetRow = null; // Determine the correct TableRow to add cells to
		if (this.id.equals(PageWithTable.HEADER_ROW_ID)) {
			targetRow = actualHeaderCellsContainer; // For header row, add to its specific cell container
		} else {
			if (getWidget() instanceof TableRow) { // For data rows, getWidget() is the TableRow
				targetRow = (TableRow) getWidget();
			} else if (actualHeaderCellsContainer != null) { // Fallback for data rows
				targetRow = actualHeaderCellsContainer;
			}
		}
		if (targetRow == null) {
			Log.e("WF_Table_Row_Recycle", "Target TableRow for adding cell is null. Row ID: " + id + ", colKey: " + colKey);
			return;
		}


		if ("simple".equals(type)) {
			widget = new WF_Simple_Cell_Widget(columnKeyHash, getLabel(), al.getDescription(myRow),
					myContext, this.getId() + colKey, true, cellType);
			View widgetView = widget.getWidget();
			if (widgetView instanceof CheckBox) {
				CheckBox cb = (CheckBox) widgetView;
				Context ctx = myContext.getContext();
				widgetView.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent));
			}
		} else {
			widget = new WF_Cell_Widget(columnKeyHash, getLabel(), al.getDescription(myRow),
					myContext, this.getId() + colKey, true,cellType);
			View widgetView = widget.getWidget();
			if (widgetView != null) {
				Context ctx = myContext.getContext();
				widgetView.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent));
				// TODO: If WF_Cell_Widget has an internal TextView, apply textColor to it here.
			}
		}

		View actualWidgetView = widget.getWidget();
		if (actualWidgetView != null) {
			TableRow.LayoutParams params;
			if (width != -1) {
				params = new TableRow.LayoutParams(width, TableRow.LayoutParams.MATCH_PARENT);
			} else {
				params = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);
			}
			params.gravity = Gravity.CENTER_VERTICAL;
			actualWidgetView.setLayoutParams(params);

			myColumns.add(widget);
			targetRow.addView(actualWidgetView);
		} else {
			Log.e("WF_Table_Row_Recycle", "Widget's view is null in addCell for colKey: "+colKey);
		}
	}

	// --- Other existing methods (getSortableField, getTimeStamp, etc.) ---
	@Override
	public String getSortableField(String columnId) { if (al == null) return ""; return al.getTable().getElement(columnId, myRow); }
	@Override
	public long getTimeStamp() { return 0; }
	@Override
	public boolean hasValue() { if (myColumns==null || myColumns.isEmpty()) return false; for (WF_Cell w:myColumns) if (w.hasValue()) return true; return false; }
	@Override
	public void refresh() { if (myColumns==null) return ; for (WF_Cell w:myColumns) w.refresh(); }
	@Override
	public Set<Variable> getAssociatedVariables() { if (myColumns!=null && !myColumns.isEmpty()) { return myColumns.get(0).getAssociatedVariables(); } return null; }
	@Override
	public int compareTo(Listable other) { return this.getLabel().compareTo(other.getLabel()); }

	public View addNoClickHeaderCell(String label, String backgroundColor, String textColor) {
		//Log.d("bino","addNoClickHeader called");
		TableRow targetContainer = this.id.equals(PageWithTable.HEADER_ROW_ID) ? actualHeaderCellsContainer : (getWidget() instanceof TableRow ? (TableRow)getWidget() : null);
		if (targetContainer == null) { Log.e("WF_Table_Row_Recycle", "Target container null in addNoClickHeaderCell"); return null; }
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_header, targetContainer, false);
		TextView tv= emptyCell.findViewById(R.id.headerT); tv.setText(label);
		targetContainer.addView(emptyCell); return emptyCell;
	}
	public TextView addAggregateTextCell(String backgroundColor, String textColor) {
		//Log.d("bino","addAggregateTextCell called");
		TableRow targetContainer = this.id.equals(PageWithTable.HEADER_ROW_ID) ? actualHeaderCellsContainer : (getWidget() instanceof TableRow ? (TableRow)getWidget() : null);
		if (targetContainer == null) { Log.e("WF_Table_Row_Recycle", "Target container null in addAggregateTextCell"); return null; }
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_text_aggregate, targetContainer, false);
		View bg = emptyCell.findViewById(R.id.outputContainer); TextView tv= emptyCell.findViewById(R.id.contentT);
		Context ctx = myContext.getContext();
		if (backgroundColor!=null) bg.setBackgroundColor(Tools.getColorResource(ctx,backgroundColor)); else bg.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent));
		if (textColor!=null) tv.setTextColor(Tools.getColorResource(ctx,textColor)); else tv.setTextColor(ContextCompat.getColor(ctx, R.color.default_text_color));
		targetContainer.addView(emptyCell); return tv;
	}


	public CheckBox addAggregateLogicalCell(String backgroundColor, String textColor) {
		//Log.d("bino","addAggregateLogicalCell called");
		TableRow targetContainer = this.id.equals(PageWithTable.HEADER_ROW_ID) ? actualHeaderCellsContainer : (getWidget() instanceof TableRow ? (TableRow)getWidget() : null);
		if (targetContainer == null) { Log.e("WF_Table_Row_Recycle", "Target container null in addAggregateLogicalCell"); return null; }
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_logical_aggregate, targetContainer, false);
		CheckBox cb= emptyCell.findViewById(R.id.contentT);
		Context ctx = myContext.getContext();
		if (backgroundColor!=null) emptyCell.setBackgroundColor(Tools.getColorResource(ctx,backgroundColor)); else emptyCell.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent));
		if (textColor!=null) cb.setTextColor(Tools.getColorResource(ctx,textColor)); else cb.setTextColor(ContextCompat.getColor(ctx, R.color.default_text_color));
		targetContainer.addView(emptyCell); return cb;
	}
	public List<WF_Cell> getCells() { return myColumns; }
	@Override
	public String getLabel() {
		// Ensure 'al' (VariableConfiguration) is accessible, typically inherited from WF_Widget or set.
		if (al == null) {
			Log.e("WF_Table_Row_Recycle", "VariableConfiguration 'al' is null in getLabel for row id "+id);
			return "*ConfigErr*";
		}
		return al.getEntryLabel(myRow);
	}
	@Override
	public String getKey() { return getLabel(); }
	@Override
	public Map<String, String> getKeyChain() { return null; }
}
