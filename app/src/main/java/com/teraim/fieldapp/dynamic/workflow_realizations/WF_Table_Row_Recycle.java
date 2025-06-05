package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
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
	private List<WF_Cell> myColumns; // Data cells for non-aggregate columns
	private final WF_Context myContext;
	private final TextView headerT_RowLabel; // This is the TextView for the row label (first column in data rows) or the top-left cell in the header row
	private final String id;

	// For the main header row, this stores the dynamically added header cell views.
	private List<View> headerCellViews = new ArrayList<>();
	// For data rows, this stores the dynamically added aggregate cell views.
	private List<View> aggregateCellViewsInRow = new ArrayList<>();

	private TableRow actualHeaderCellsContainer; // For header row, this is R.id.header_content_row. For data rows, this is getWidget().

	public WF_Table_Row_Recycle(PageWithTable myWfTable, String id, View v, WF_Context ctx, boolean isVisible) {
		super(id,v,isVisible,ctx);
		myColumns = new ArrayList<>(); // Initialize to prevent nulls
		// aggregateCellViewsInRow = new ArrayList<>(); // Initialized with field
		// headerCellViews = new ArrayList<>(); // Initialized with field

		myContext = ctx;
		headerT_RowLabel = v.findViewById(R.id.headerT); // TextView within the inflated row layout
		this.id=id;
		this.myWfTable = myWfTable;

		if (this.id.equals(PageWithTable.HEADER_ROW_ID)) {
			if (v != null) {
				actualHeaderCellsContainer = v.findViewById(R.id.header_content_row);
				if (actualHeaderCellsContainer == null) {
					Log.e("WF_Table_Row_Recycle", "Critical: TableRow with R.id.header_content_row not found in R.layout.header_table_row.xml for HEADER_ROW_ID");
				}
			}
		} else { // Data Row
			if (v instanceof TableRow) {
				actualHeaderCellsContainer = (TableRow) v;
			} else {
				Log.e("WF_Table_Row_Recycle", "Data row's main view (getWidget()) is not a TableRow for ID: " + id + ". View type: " + (v != null ? v.getClass().getName() : "null"));
				// If getWidget() can be something else, this assignment might be wrong for data rows.
				// However, R.layout.table_row is a TableRow.
			}
		}
	}

	public void addEntryField(List<String> row) {
		myRow=row;
		String label = getLabel();
		if (label==null) label="*null*";
		if (headerT_RowLabel != null) headerT_RowLabel.setText(label);
	}

	public void addHeaderCell(String label, String backgroundColor, String textColor, int width, final int columnIndex, PageWithTable.ColumnDefinition definition) {
		if (myContext == null || myContext.getContext() == null || !this.id.equals(PageWithTable.HEADER_ROW_ID) || actualHeaderCellsContainer == null) {
			Log.e("WF_Table_Row_Recycle", "Cannot addHeaderCell. ID: " + this.id + ". Conditions not met."); return;
		}
		View headerC = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_header, actualHeaderCellsContainer, false);
		TextView headerTV = headerC.findViewById(R.id.headerT);
		if (headerTV == null) { Log.e("WF_Table_Row_Recycle", "headerT in cell_field_header not found."); return; }
		headerTV.setText(label);

		headerC.setTag(R.id.tag_original_bg_color, definition.backgroundColor);
		headerTV.setTag(R.id.tag_original_text_color, definition.textColor);
		headerTV.setTag(R.id.tag_original_text_size_sp, definition.textSizeSp);

		Context ctx = myContext.getContext();
		if (definition.backgroundColor != null) headerC.setBackgroundColor(Tools.getColorResource(ctx, definition.backgroundColor));
		else headerC.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent));
		if (definition.textColor != null) headerTV.setTextColor(Tools.getColorResource(ctx, definition.textColor));
		else headerTV.setTextColor(ContextCompat.getColor(ctx, R.color.default_text_color));
		headerTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, definition.textSizeSp);
		headerTV.setTypeface(null, Typeface.NORMAL);

		TableRow.LayoutParams params;
		if (definition.isAggregate) {
			params = new TableRow.LayoutParams(width > 0 ? width : TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT);
		} else {
			params = new TableRow.LayoutParams(0, TableRow.LayoutParams.WRAP_CONTENT, 1.0f);
		}
		params.gravity = Gravity.CENTER_VERTICAL;
		headerC.setLayoutParams(params);

		actualHeaderCellsContainer.addView(headerC);
		headerCellViews.add(headerC);
		headerC.setOnClickListener(v -> myWfTable.onColumnHeaderClicked(columnIndex));
	}

	public void updateHeaderAppearance(List<PageWithTable.ColumnDefinition> globalColumnDefinitions, int focusedColumnIndex) {
		if (!this.id.equals(PageWithTable.HEADER_ROW_ID)) return;
		if (myContext == null || myContext.getContext() == null || actualHeaderCellsContainer == null || headerCellViews == null) {
			Log.e("WF_Table_Row_Recycle", "Context, container, or headerCellViews null in updateHeaderAppearance.");
			return;
		}
		if (headerCellViews.size() != globalColumnDefinitions.size()) {
			Log.w("WF_Table_Row_Recycle", "Header style update: Mismatch headerCellViews (" + headerCellViews.size() + ") vs globalColumnDefinitions (" + globalColumnDefinitions.size() + ").");
		}

		Context ctx = myContext.getContext();
		for (int i = 0; i < globalColumnDefinitions.size(); i++) {
			if (i >= headerCellViews.size()) { Log.w("WF_Table_Row_Recycle", "Not enough header cell views for definition at index " + i); break; }

			View headerCellView = headerCellViews.get(i);
			TextView headerTV = headerCellView.findViewById(R.id.headerT);
			PageWithTable.ColumnDefinition colDef = globalColumnDefinitions.get(i);

			if (headerTV == null) continue;
			headerCellView.setVisibility(colDef.isVisible ? View.VISIBLE : View.GONE);

			if (colDef.isVisible) {
				if (i == focusedColumnIndex) {
					headerCellView.setBackgroundColor(ContextCompat.getColor(ctx, R.color.selected_header_background));
					headerTV.setTextColor(ContextCompat.getColor(ctx, R.color.selected_header_text_color));
					headerTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, colDef.textSizeSp + 3);
					headerTV.setTypeface(null, Typeface.BOLD);
				} else {
					String originalBgColorStr = (String) headerCellView.getTag(R.id.tag_original_bg_color);
					String originalTextColorStr = (String) headerTV.getTag(R.id.tag_original_text_color);
					Float originalTextSizeSp = (Float) headerTV.getTag(R.id.tag_original_text_size_sp);

					if (originalBgColorStr != null) headerCellView.setBackgroundColor(Tools.getColorResource(ctx, originalBgColorStr));
					else headerCellView.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent));

					if (originalTextColorStr != null) headerTV.setTextColor(Tools.getColorResource(ctx, originalTextColorStr));
					else headerTV.setTextColor(ContextCompat.getColor(ctx, R.color.default_text_color));

					headerTV.setTextSize(TypedValue.COMPLEX_UNIT_SP, (originalTextSizeSp != null) ? originalTextSizeSp : colDef.textSizeSp);
					headerTV.setTypeface(null, Typeface.NORMAL);
				}
				headerCellView.requestLayout();
			}
		}
		actualHeaderCellsContainer.requestLayout();
	}

	/**
	 * Clears all dynamically added cells from this row.
	 * For the main header row, it clears cells from headerCellViews.
	 * For data rows, it clears cells from myColumns (non-aggregate) and aggregateCellViewsInRow.
	 * The statically defined R.id.headerT (row label / top-left header cell) is not removed.
	 */
	public void clearCells() {
		TableRow targetRowForDynamicCells = actualHeaderCellsContainer;
		if (targetRowForDynamicCells == null && getWidget() instanceof TableRow) { // Fallback for data rows if actualHeaderCellsContainer wasn't set properly
			targetRowForDynamicCells = (TableRow) getWidget();
		}

		if (targetRowForDynamicCells == null) {
			Log.e("WF_Table_Row_Recycle", "Target TableRow for clearCells is null. ID: " + this.id);
			// Clear lists anyway to prevent lingering data if views are gone
			if (myColumns != null) myColumns.clear();
			if (headerCellViews != null) headerCellViews.clear();
			if (aggregateCellViewsInRow != null) aggregateCellViewsInRow.clear();
			return;
		}

		// Clear data cells (myColumns) - relevant for data rows
		if (myColumns != null) {
			for (int i = myColumns.size() - 1; i >= 0; i--) {
				WF_Cell cell = myColumns.get(i);
				if (cell != null && cell.getWidget() != null) {
					targetRowForDynamicCells.removeView(cell.getWidget());
				}
			}
			myColumns.clear();
		}

		// Clear aggregate data cells (aggregateCellViewsInRow) - relevant for data rows
		if (aggregateCellViewsInRow != null) {
			for (View aggCellView : aggregateCellViewsInRow) {
				if (aggCellView != null) {
					targetRowForDynamicCells.removeView(aggCellView);
				}
			}
			aggregateCellViewsInRow.clear();
		}

		// Clear header cells (headerCellViews) - relevant ONLY for the main headerRow
		if (this.id.equals(PageWithTable.HEADER_ROW_ID)) {
			if (headerCellViews != null) {
				for (View headerCellView : headerCellViews) {
					if (headerCellView != null) {
						targetRowForDynamicCells.removeView(headerCellView); // targetRowForDynamicCells is actualHeaderCellsContainer for header
					}
				}
				headerCellViews.clear();
			}
		}
	}

	/**
	 * Updates the visibility of data cells (both normal and aggregate) in a data row.
	 * This method should NOT be called on the main header row.
	 *
	 * @param globalColumnDefinitions List of all column definitions for the table.
	 */
	public void updateCellVisibilities(List<PageWithTable.ColumnDefinition> globalColumnDefinitions) {
		if (this.id.equals(PageWithTable.HEADER_ROW_ID)) {
			// Header visibility is handled by updateHeaderAppearance
			Log.w("WF_Table_Row_Recycle", "updateCellVisibilities called on header row. This should be handled by updateHeaderAppearance.");
			return;
		}
		if (globalColumnDefinitions == null) {
			Log.w("WF_Table_Row_Recycle", "DataRow: Cannot update cell visibilities: globalColumnDefinitions is null. Row ID: " + this.id);
			return;
		}

		TableRow targetRow = (getWidget() instanceof TableRow) ? (TableRow) getWidget() : actualHeaderCellsContainer;
		int currentDataCellIndex = 0;    // Tracks index in myColumns (non-aggregate cells)
		int currentAggCellIndex = 0;     // Tracks index in aggregateCellViewsInRow

		for (int globalDefIndex = 0; globalDefIndex < globalColumnDefinitions.size(); globalDefIndex++) {
			PageWithTable.ColumnDefinition colDef = globalColumnDefinitions.get(globalDefIndex);
			View cellViewToUpdate = null;

			if (colDef.isAggregate) {
				if (aggregateCellViewsInRow != null && currentAggCellIndex < aggregateCellViewsInRow.size()) {
					cellViewToUpdate = aggregateCellViewsInRow.get(currentAggCellIndex);
					currentAggCellIndex++;
				} else {
					Log.w("WF_Table_Row_Recycle", "Row " + getLabel() + ": Not enough aggregate cell views for colDef '" + colDef.label + "' (global index " + globalDefIndex + ")");
				}
			} else { // Non-aggregate column
				if (myColumns != null && currentDataCellIndex < myColumns.size()) {
					WF_Cell wfCell = myColumns.get(currentDataCellIndex);
					if (wfCell != null) {
						cellViewToUpdate = wfCell.getWidget();
					}
					currentDataCellIndex++;
				} else {
					Log.w("WF_Table_Row_Recycle", "Row " + getLabel() + ": Not enough data cell (myColumns) views for colDef '" + colDef.label + "' (global index " + globalDefIndex + ")");
				}
			}

			if (cellViewToUpdate != null) {
				cellViewToUpdate.setVisibility(colDef.isVisible ? View.VISIBLE : View.GONE);
			}
		}

		if (targetRow != null) targetRow.requestLayout();
		else if (getWidget() != null) getWidget().requestLayout();
	}

	public void addCell(String colHeader, String colKey, Map<String,String> columnKeyHash, String type, int passedWidth, String backgroundColor, String textColor, WF_Cell.CellType cellType) {
		if (cellType== WF_Cell.CellType.Aggregate && !this.id.equals(PageWithTable.HEADER_ROW_ID)) {
			return; // Aggregate data cells are added via addAggregateTextCell/LogicalCell
		}
		if (myColumns==null) myColumns = new ArrayList<>();
		WF_Cell widget;
		if (al == null) { Log.e("WF_Table_Row_Recycle", "VariableConfiguration 'al' is null in addCell."); return; }
		TableRow targetRow = (getWidget() instanceof TableRow) ? (TableRow) getWidget() : actualHeaderCellsContainer;
		if (targetRow == null) { Log.e("WF_Table_Row_Recycle", "Target TableRow for adding cell is null. Row ID: " + id); return; }

		Context ctx = myContext.getContext();
		boolean isAggregateCellDefinition = false; // Check if the definition implies it's for an aggregate column header
		for(PageWithTable.ColumnDefinition cd: myWfTable.getColumnDefinitions()) { // Assuming myWfTable has a getter for columnDefinitions
			if (cd.key.equals(colKey) && cd.isAggregate) {
				isAggregateCellDefinition = true;
				break;
			}
		}


		if ("simple".equals(type)) {
			widget = new WF_Simple_Cell_Widget(columnKeyHash, getLabel(), al.getDescription(myRow), myContext, this.getId() + colKey, true, cellType);
			View widgetView = widget.getWidget();
			if (widgetView instanceof CheckBox) {
				if (textColor != null) ((CheckBox) widgetView).setTextColor(Tools.getColorResource(ctx, textColor));
				if (backgroundColor != null) widgetView.setBackgroundColor(Tools.getColorResource(ctx,backgroundColor));
			}
		} else {
			widget = new WF_Cell_Widget(columnKeyHash, getLabel(), al.getDescription(myRow), myContext, this.getId() + colKey, true, cellType);
			View widgetView = widget.getWidget();
			if (widgetView != null) {
				if (backgroundColor != null) widgetView.setBackgroundColor(Tools.getColorResource(ctx,backgroundColor));
				else widgetView.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent));
			}
		}

		View actualWidgetView = widget.getWidget();
		if (actualWidgetView != null) {
			TableRow.LayoutParams params;
			if (isAggregateCellDefinition) { // Check if the column key corresponds to an aggregate column definition
				params = new TableRow.LayoutParams(passedWidth > 0 ? passedWidth : TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.MATCH_PARENT);
			} else {
				params = new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT, 1.0f);
			}
			params.gravity = Gravity.CENTER_VERTICAL;
			actualWidgetView.setLayoutParams(params);
			myColumns.add(widget);
			targetRow.addView(actualWidgetView);
		} else {
			Log.e("WF_Table_Row_Recycle", "Widget's view is null in addCell for colKey: "+colKey);
		}
	}

	@Override public String getSortableField(String columnId) { if (al == null) return ""; return al.getTable().getElement(columnId, myRow); }
	@Override public long getTimeStamp() { return 0; }
	@Override public boolean hasValue() { if (myColumns==null || myColumns.isEmpty()) return false; for (WF_Cell w:myColumns) if (w.hasValue()) return true; return false; }
	@Override public void refresh() { if (myColumns==null) return ; for (WF_Cell w:myColumns) w.refresh(); }
	@Override public Set<Variable> getAssociatedVariables() { if (myColumns!=null && !myColumns.isEmpty()) { return myColumns.get(0).getAssociatedVariables(); } return null; }
	@Override public int compareTo(Listable other) { return this.getLabel().compareTo(other.getLabel()); }

	public View addNoClickHeaderCell(String label, String backgroundColor, String textColor) {
		TableRow targetContainer = this.id.equals(PageWithTable.HEADER_ROW_ID) ? actualHeaderCellsContainer : (getWidget() instanceof TableRow ? (TableRow)getWidget() : null);
		if (targetContainer == null) { Log.e("WF_Table_Row_Recycle", "Target container null in addNoClickHeaderCell"); return null; }
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_header, targetContainer, false);
		TextView tv= emptyCell.findViewById(R.id.headerT); tv.setText(label);
		targetContainer.addView(emptyCell); return emptyCell;
	}

	public TextView addAggregateTextCell(String backgroundColor, String textColor) {
		TableRow targetContainer = (getWidget() instanceof TableRow) ? (TableRow)getWidget() : actualHeaderCellsContainer;
		if (targetContainer == null) { Log.e("WF_Table_Row_Recycle", "Target container null in addAggregateTextCell"); return null; }
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_text_aggregate, targetContainer, false);
		View bg = emptyCell.findViewById(R.id.outputContainer); TextView tv= emptyCell.findViewById(R.id.contentT);
		Context ctx = myContext.getContext();
		if (backgroundColor!=null) bg.setBackgroundColor(Tools.getColorResource(ctx,backgroundColor)); else bg.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent));
		if (textColor!=null) tv.setTextColor(Tools.getColorResource(ctx,textColor)); else tv.setTextColor(ContextCompat.getColor(ctx, R.color.default_text_color));
		aggregateCellViewsInRow.add(emptyCell);
		targetContainer.addView(emptyCell); return tv;
	}
	public CheckBox addAggregateLogicalCell(String backgroundColor, String textColor) {
		TableRow targetContainer = (getWidget() instanceof TableRow) ? (TableRow)getWidget() : actualHeaderCellsContainer;
		if (targetContainer == null) { Log.e("WF_Table_Row_Recycle", "Target container null in addAggregateLogicalCell"); return null; }
		View emptyCell = LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_field_logical_aggregate, targetContainer, false);
		CheckBox cb= emptyCell.findViewById(R.id.contentT);
		Context ctx = myContext.getContext();
		if (backgroundColor!=null) emptyCell.setBackgroundColor(Tools.getColorResource(ctx,backgroundColor)); else emptyCell.setBackgroundColor(ContextCompat.getColor(ctx, android.R.color.transparent));
		if (textColor!=null) cb.setTextColor(Tools.getColorResource(ctx,textColor)); else cb.setTextColor(ContextCompat.getColor(ctx, R.color.default_text_color));
		aggregateCellViewsInRow.add(emptyCell);
		targetContainer.addView(emptyCell); return cb;
	}
	public List<WF_Cell> getCells() { return myColumns; }
	@Override
	public String getLabel() { if (al == null) { return "*ConfigErr*"; } return al.getEntryLabel(myRow); }
	@Override public String getKey() { return getLabel(); }
	@Override public Map<String, String> getKeyChain() { return null; }

	public boolean hasAnyCheckedNonAggregateSimpleCell(List<PageWithTable.ColumnDefinition> globalColumnDefinitions) {
		if (myColumns == null || myColumns.isEmpty() || globalColumnDefinitions == null) return false;
		if (al == null) { Log.e("WF_Table_Row_Recycle", "VariableConfiguration 'al' is null in hasAnyCheckedNonAggregateSimpleCell"); return false; }

		int dataCellIndex = 0;
		for (int colDefIndex = 0; colDefIndex < globalColumnDefinitions.size(); colDefIndex++) {
			PageWithTable.ColumnDefinition colDef = globalColumnDefinitions.get(colDefIndex);
			if (colDef.isAggregate) continue;

			if (dataCellIndex < myColumns.size()) {
				if ("simple".equals(colDef.type)) {
					WF_Cell cell = myColumns.get(dataCellIndex);
					Set<Variable> vars = cell.getAssociatedVariables();
					if (vars != null) {
						for (Variable var : vars) {
							if (var != null && var.getType() == Variable.DataType.bool) {
								String value = var.getValue();
								if (value != null && (value.equalsIgnoreCase("true") || value.equals("1"))) {
									return true;
								}
							}
						}
					}
				}
				dataCellIndex++;
			} else {
				break;
			}
		}
		return false;
	}
}
