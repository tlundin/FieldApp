package com.teraim.fieldapp.dynamic.templates;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filter;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Cell;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table_Row_Recycle;
import com.teraim.fieldapp.ui.FilterDialogDismissListener;
import com.teraim.fieldapp.ui.FilterDialogFragment;
import com.teraim.fieldapp.ui.TableBodyAdapter;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PageWithTable extends Executor implements TableBodyAdapter.ScrollSyncManager, FilterDialogDismissListener {


    private HorizontalScrollView stickyHeaderScrollView;
    private LinearLayout stickyHeaderLinearLayout;
    private RecyclerView tableRecyclerView;

    private WF_List<Listable> tableRowsDataList = new WF_List() {
        @Override
        public void addSorter(Sorter s) {
            super.addSorter(s);
        }

        @Override
        public void removeSorter(Sorter s) {
            super.removeSorter(s);
        }

        @Override
        public void addFilter(Filter f) {
            super.addFilter(f);
        }

        @Override
        public void removeFilter(Filter f) {
            super.removeFilter(f);
        }
    };
    private TableBodyAdapter tableBodyAdapter;

    private boolean isProgrammaticScroll = false;

    private VariableConfiguration al;
    private GlobalState gs;
    private final Map<String,Set<String>> varIdMap = new HashMap<>();
    private String myVariator;
    private Map<String, Map<String, String>> allInstances;

    private ViewGroup my_root,filterPanel;
    private WF_Context myContext;
    private LayoutInflater inflater;
    private View filterC1o,filterC2o;
    private LinearLayout filterC1,filterC2;
    private int rowNumber = 0;

    private WF_Table_Row_Recycle headerRow;
    public static final String HEADER_ROW_ID = "TableHeader";
    private boolean tableTypeSimple=false;

    private List<ColumnDefinition> columnDefinitions = new ArrayList<>();
    private int currentlyFocusedColumn = -1;

    // Inner class to define column properties
    public static class ColumnDefinition {
        public String label;
        public String key;
        public String type;
        public int width;
        public String backgroundColor;
        public String textColor;
        public float textSizeSp = 16f;
        public boolean isVisible = true;
        public boolean isAggregate = false;

        ColumnDefinition(String label, String key, String type, int width, String backgroundColor, String textColor, boolean isAggregate) {
            this.label = label;
            this.key = key;
            this.type = type;
            this.width = width;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.isAggregate = isAggregate;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        this.inflater = inflater;
        View view = inflater.inflate(R.layout.template_page_with_table, container, false);
        my_root = (ViewGroup) view.findViewById(R.id.myRoot);
        filterPanel = view.findViewById(R.id.filterPanel);
        gs = GlobalState.getInstance();
        if (gs != null) al = gs.getVariableConfiguration();
        else Log.e("PageWithTable", "GlobalState is null in onCreateView!");
        myContext = getCurrentContext();
        if (myContext == null) Log.e("PageWithTable", "myContext is null in onCreateView!");

        if (myContext != null && this.inflater != null) {
            View headerRowView = this.inflater.inflate(R.layout.header_table_row, null);
            headerRow = new WF_Table_Row_Recycle(this, HEADER_ROW_ID, headerRowView, myContext, true);
        } else {
            Log.e("PageWithTable", "Cannot initialize headerRow.");
        }
        if (myContext != null) myContext.addContainers(getContainers());


        //setupToggleListeners();

        stickyHeaderScrollView = view.findViewById(R.id.sticky_header_scroll_view);
        stickyHeaderLinearLayout = view.findViewById(R.id.sticky_header_linear_layout);
        tableRecyclerView = view.findViewById(R.id.table_recycler_view);
        filterC1o = inflater.inflate(R.layout.filter_pop_inner,null);
        filterC2o = inflater.inflate(R.layout.filter_pop_inner,null);
        filterC1 = filterC1o.findViewById(R.id.inner);
        filterC2 = filterC2o.findViewById(R.id.inner);

        setupRecyclerView();
        setupHeaderScrollListener();
        refreshHeaderUI();
        return view;
    }
    private FilterDialogFragment currentFilterDialog = null;
    private void showFilterPopup(View contentLayout, String dialogTag) {
        FragmentManager fm = getChildFragmentManager();
        dismissCurrentFilterDialog();
        int topOffset = filterPanel.getHeight();
        currentFilterDialog = FilterDialogFragment.newInstance(dialogTag, topOffset);
        currentFilterDialog.setPopupContent(contentLayout);
        currentFilterDialog.show(fm, dialogTag);

    }

    private void dismissFilterPopup(String dialogTag) {
        FragmentManager fm = getChildFragmentManager();
        FilterDialogFragment dialog = (FilterDialogFragment) fm.findFragmentByTag(dialogTag);
        if (dialog != null && dialog.isAdded()) {
            dialog.dismissAllowingStateLoss();
            if (dialog == currentFilterDialog) {
                currentFilterDialog = null;
            }
        }
    }

     @Override
    public void onFilterDialogDismissed(String dialogTag) {
     Log.d("vortex","popup dismissed");
     currentFilterDialog = null; // Clear the reference
 }

    private void dismissCurrentFilterDialog() {
        if (currentFilterDialog != null && currentFilterDialog.isAdded()) {
            currentFilterDialog.dismissAllowingStateLoss();
            currentFilterDialog = null;
        }
        // Also ensure toggles are unchecked if a dialog is dismissed externally
        // This might be too aggressive depending on desired UX
        // if (toggleFilterAlphabetic != null) toggleFilterAlphabetic.setChecked(false);
        // if (toggleFilterFamilj != null) toggleFilterFamilj.setChecked(false);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (gs == null) { gs = GlobalState.getInstance(); if (gs != null) al = gs.getVariableConfiguration(); }
        if (al == null && gs != null) al = gs.getVariableConfiguration();
        if (gs == null) Log.e("PageWithTable", "GlobalState is null in onViewCreated!");
        if (al == null) Log.e("PageWithTable", "VariableConfiguration (al) is null in onViewCreated!");

        if (wf != null) { Log.d("PageWithTable","Executing workflow!!"); run(); }

    }

    @Override
    public void onResume() { super.onResume(); Activity activity = getActivity(); if (activity instanceof Start) ((Start) activity).setTopBarVisibility(false); }
    @Override
    public void onPause() { super.onPause(); Activity activity = getActivity(); if (activity instanceof Start) ((Start) activity).setTopBarVisibility(true); }

    private void setupRecyclerView() {
        tableBodyAdapter = new TableBodyAdapter(requireContext(), tableRowsDataList, Collections.emptyList(), this);
        tableRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        tableRecyclerView.setAdapter(tableBodyAdapter);
    }
    private void setupHeaderScrollListener() {
        if (stickyHeaderScrollView != null) {
            stickyHeaderScrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!isProgrammaticScroll) {
                    syncScroll(stickyHeaderScrollView, scrollX);
                }
            });
        }
    }
    private void refreshHeaderUI() {
        if (stickyHeaderLinearLayout == null || headerRow == null || headerRow.getWidget() == null) return;
        stickyHeaderLinearLayout.removeAllViews();
        View headerWidgetView = headerRow.getWidget();
        if (headerWidgetView.getParent() instanceof ViewGroup) {
            ((ViewGroup) headerWidgetView.getParent()).removeView(headerWidgetView);
        }
        stickyHeaderLinearLayout.addView(headerWidgetView);
    }

    public void onColumnHeaderClicked(int columnIndex) {
        Log.d("PageWithTable", "Column header clicked: " + columnIndex + ". Current focused: " + currentlyFocusedColumn);
        if (columnIndex < 0 || columnIndex >= columnDefinitions.size()) {
            Log.e("PageWithTable", "Invalid columnIndex: " + columnIndex);
            return;
        }

        ColumnDefinition clickedColDef = columnDefinitions.get(columnIndex);

        if (currentlyFocusedColumn == columnIndex) {
            for (ColumnDefinition def : columnDefinitions) {
                def.isVisible = true;
            }
            currentlyFocusedColumn = -1;
            Log.d("PageWithTable", "Showing all columns.");
        } else {
            if (clickedColDef.isAggregate) {
                for (int i = 0; i < columnDefinitions.size(); i++) {
                    ColumnDefinition def = columnDefinitions.get(i);
                    def.isVisible = def.isAggregate || (i == columnIndex);
                }
                currentlyFocusedColumn = columnIndex;
                Log.d("PageWithTable", "Focusing aggregate column: " + columnIndex + ", other aggregates also visible.");

            } else {
                for (int i = 0; i < columnDefinitions.size(); i++) {
                    ColumnDefinition def = columnDefinitions.get(i);
                    def.isVisible = def.isAggregate || (i == columnIndex);
                }
                currentlyFocusedColumn = columnIndex;
                Log.d("PageWithTable", "Focusing non-aggregate column: " + columnIndex + ", aggregates also visible.");
            }
        }

        if (headerRow != null) {
            headerRow.updateHeaderAppearance(columnDefinitions, currentlyFocusedColumn);
        }
        refreshColumnVisibilitiesInUI();
    }

    private void refreshColumnVisibilitiesInUI() {
        Log.d("PageWithTable", "Refreshing data row column visibilities. Focused: " + currentlyFocusedColumn);
        for (WF_Table_Row_Recycle dataRow : tableRowsDataList) {
            dataRow.updateCellVisibilities(columnDefinitions);
        }
    }

    private void addColumnInternal(ColumnDefinition colDef, int columnIndex) {
        Log.d("PageWithTable", "addColumnInternal - Header: " + colDef.label + ", Key: " + colDef.key + ", Index: " + columnIndex + ", IsAggregate: " + colDef.isAggregate);
        if (myContext == null || headerRow == null) {
            Log.e("PageWithTable", "Context or HeaderRow null in addColumnInternal.");
            return;
        }

        Map<String, String> colHash = Tools.copyKeyHash(myContext.getKeyHash());
        if (myVariator != null && colDef.key != null) {
            colHash.put(myVariator, colDef.key);
        }

        headerRow.addHeaderCell(colDef.label, colDef.backgroundColor, colDef.textColor, colDef.width, columnIndex, colDef);

        String cellBgColor = colDef.backgroundColor;
        String cellTextColor = colDef.textColor;

        // For "simple" type (checkboxes), do not pass specific colors to use defaults
        if ("simple".equals(colDef.type)) {
            cellBgColor = null;
            cellTextColor = null;
            Log.d("PageWithTable", "Column type is simple for '" + colDef.label + "'. Using default (null) colors for cells.");
        }

        for (WF_Table_Row_Recycle wft : tableRowsDataList) {
            wft.addCell(colDef.label, colDef.key, colHash, colDef.type, colDef.width, cellBgColor, cellTextColor, colDef.isAggregate?WF_Cell.CellType.Aggregate:WF_Cell.CellType.Normal);
        }
    }

    public void addColumns(List<String> labels,
                           List<String> columnKeyL, String type, String widthS,
                           String backgroundColor, String textColor) {
        Log.d("PageWithTable", "addColumns called. BgColor: [" + backgroundColor + "], TextColor: [" + textColor + "]");
        columnDefinitions.clear();
        textColor=null;
        if (headerRow != null) headerRow.clearCells();
        for (WF_Table_Row_Recycle row : tableRowsDataList) row.clearCells();

        if (labels != null && columnKeyL != null && labels.size() == columnKeyL.size()) {
            for (int i = 0; i < labels.size(); i++) {
                String label = labels.get(i) != null ? labels.get(i) : "";
                String key = columnKeyL.get(i);
                int width = 200;
                if (widthS != null && !widthS.isEmpty()) {
                    try {
                        width = Integer.parseInt(widthS.replaceAll("[^0-9]", ""));
                        if (width <= 0) width = 200;
                    } catch (NumberFormatException e) { Log.w("PageWithTable", "Could not parse width: " + widthS); }
                }
                ColumnDefinition colDef = new ColumnDefinition(label, key, type, width, backgroundColor, textColor, false);
                columnDefinitions.add(colDef);
                addColumnInternal(colDef, i);
            }
        } else { Log.w("PageWithTable", "Labels/columnKeyL null or sizes mismatch in addColumns."); }
        if (type!=null && type.equals("simple")) tableTypeSimple=true;

        currentlyFocusedColumn = -1;
        if (headerRow != null) headerRow.updateHeaderAppearance(columnDefinitions, currentlyFocusedColumn);
        refreshColumnVisibilitiesInUI();
        refreshHeaderUI();
    }
    //Keep track of Aggregate column cells.
    public enum AggregateFunction {
        AND, OR, COUNT, SUM, MIN, MAX, aggF, AVG
    }
    private class AggregateColumn implements EventListener {

        AggregateColumn(String label, Expressor.EvalExpr expressionE, String format, AggregateFunction aggregationFunction, boolean isLogical,List<WF_Table_Row_Recycle>rows) {
            myCells=new ArrayList<View>();
            myRows = rows;
            this.expressionE=expressionE;
            this.format=format;
            myContext.registerEventListener(this , Event.EventType.onSave);
            aggF = aggregationFunction;
            this.label=label;
            this.isLogical=isLogical;
        }

        final boolean isLogical;
        final Expressor.EvalExpr expressionE;
        final List<View>myCells;
        final List<WF_Table_Row_Recycle>myRows;
        final String format;
        final AggregateFunction aggF;
        final String label;

        public List<View> getMyCells() {
            return myCells;
        }

        void addCell(View aggView) {
            myCells.add(aggView);
        }



        @Override
        public void onEvent(Event e) {
            if (e.getType()==Event.EventType.onSave) {
                Log.d("vortex","caught onSave in aggregate_column!");
                if (myCells!=null) {
                    //loop over mycells (or over rows...doesnt matter. Equal number)
                    TextView tv=null; CheckBox cb = null;
                    for(int i=0;i<myCells.size();i++) {
                        if (!isLogical)
                            tv = (TextView)myCells.get(i);
                        else
                            cb = (CheckBox)myCells.get(i);
                        WF_Table_Row_Recycle row = myRows.get(i);
                        //if (aggregationFunction.equals(AgAND)
                        Set<Variable> vars;

                        boolean completeResB = true;
                        Integer completeRes = 0;
                        if (aggF== AggregateFunction.MIN || aggF== AggregateFunction.MAX)
                            completeRes=null;

                        boolean done=false;
                        //Aggregate over all cells in a row.
                        for (WF_Cell cell:row.getCells()) {
                            vars=cell.getAssociatedVariables();
                            //Log.d("vortex","cell class: "+cell.getClass().getName()+" cell type "+cell.getType());
                            if (cell.getType().equals(WF_Cell.CellType.Aggregate)) {
                                //Log.d("vortex", "skipping aggregate");
                                continue;
                            }
                            //Evaluate expression with given variables as context.
                            //Log.d("vortex","Cell has these variables: ");
                            //for (Variable v:vars)
                            	//Log.d("vortex",v.getId());
                            if (isLogical) {
                                Boolean result = Expressor.analyzeBooleanExpression(expressionE, vars);

                                switch (aggF) {
                                    case AND:
                                        if (result ==null || !result) {
                                            completeResB=false;
                                            done=true;
                                        }
                                        break;
                                    case OR:
                                        if (result!=null && result) {
                                            completeResB=true;
                                            done= true;
                                        } else
                                            completeResB=false;
                                        break;
                                }
                            }
                            else {
                                String result = Expressor.analyze(expressionE, vars);
                                if (!Tools.isNumeric(result)) {
                                    //Log.e("vortex", "couldnt use " + result + " for " + aggF + ". Not numeric");
                                    continue;
                                }
                                //Numeric result.
                                int res = Integer.parseInt(result);
                                Log.e("vortex", "got numeric "+res);
                                switch (aggF) {

                                    case SUM:
                                    case AVG:
                                        completeRes+=res;
                                        break;
                                    case COUNT:
                                        completeRes++;
                                        break;
                                    case MIN:
                                        if (completeRes==null || completeRes>res)
                                            completeRes=res;
                                        break;
                                    case MAX:
                                        if (completeRes==null || completeRes<res)
                                            completeRes=res;
                                        break;
                                }
                            }
                            if (done) {
                                //Log.d("vortex","I am done..exiting");
                                break;
                            }
                        }
                        //Here we are done for row.
                        if (isLogical) {
                            cb.setChecked(completeResB);
                        } else {
                            if (completeRes==null) {
                                Log.e("vortex","no result..completeRes is null");
                                tv.setText("");
                            } else {
                                if (aggF== AggregateFunction.AVG) {
                                    int size = row.getCells().size();
                                    completeRes=completeRes/size;
                                }
                                tv.setText(completeRes.toString());
                            }
                        }
                    }
                }
            }
        }

        @Override
        public String getName() {
            return "Aggregate_column";
        }

    }

    public void addAggregateColumn(String label, Expressor.EvalExpr expressionE,
                                   String aggregationFunction, String format, String widthStr,
                                   boolean isDisplayed, String backgroundColor, String textColor) {
        Log.d("PageWithTable", "addAggregateColumn - Label: " + label);
        if (label==null || label.isEmpty())
            label = aggregationFunction;
        String colKey = "agg_" + label.replaceAll("\\s+", "_").toLowerCase();
        String type = "aggregate";
        int width = 200;
        if (widthStr != null && !widthStr.isEmpty()) {
            try {
                width = Integer.parseInt(widthStr.replaceAll("[^0-9]", ""));
                if (width <= 0) width = 200;
            } catch (NumberFormatException e) { Log.w("PageWithTable", "Could not parse width for aggregate: " + widthStr); }
        }

        ColumnDefinition colDef = new ColumnDefinition(label, colKey, type, width, backgroundColor, textColor, true);
        colDef.isVisible = isDisplayed;
        columnDefinitions.add(colDef);

        addColumnInternal(colDef, columnDefinitions.size() - 1);

        if (headerRow != null) headerRow.updateHeaderAppearance(columnDefinitions, currentlyFocusedColumn);
        refreshColumnVisibilitiesInUI();
        refreshHeaderUI();

        AggregateFunction aggF;
        try {
            if (aggregationFunction == null) throw new IllegalArgumentException("Aggregation function is null");
            aggF = AggregateFunction.valueOf(aggregationFunction.toUpperCase());
        } catch (Exception e) {
            if (o != null) {o.addRow(""); o.addRedText("Agg func '"+aggregationFunction+"' invalid.");}
            else {Log.e("PageWithTable", "Agg func '"+aggregationFunction+"' invalid and logger 'o' is null.");}
            return;
        }
        boolean isLogical = (aggF== AggregateFunction.AND || aggF == AggregateFunction.OR);
        AggregateColumn aggColInstance = new AggregateColumn(label, expressionE, format, aggF, isLogical,tableRowsDataList);

        for (WF_Table_Row_Recycle wft : tableRowsDataList) {
            View aggCellView;
            if (!isLogical) {
                TextView tv = wft.addAggregateTextCell(backgroundColor,textColor);
                tv.setMinWidth(width);
                aggCellView = tv;
            } else {
                CheckBox cb = wft.addAggregateLogicalCell(backgroundColor,textColor);
                TableRow.LayoutParams params = new TableRow.LayoutParams(width, TableRow.LayoutParams.WRAP_CONTENT);
                params.gravity = android.view.Gravity.CENTER;
                cb.setLayoutParams(params);
                aggCellView = cb;
            }
            aggColInstance.addCell(aggCellView);
            if (!isDisplayed) {
                aggCellView.setVisibility(View.INVISIBLE);
            }
        }
        if (myContext != null) {
            aggColInstance.onEvent(new WF_Event_OnSave("PageWithTable_AggColAdded_" + label));
        }
    }

    public void addRow(List<String> rowData) {
        if (rowData == null) { Log.w("PageWithTable", "addRow null data"); return; }
        if (inflater == null || myContext == null) { Log.e("PageWithTable", "inflater or myContext null in addRow"); return; }

        WF_Table_Row_Recycle rowWidget = new WF_Table_Row_Recycle(this, (rowNumber++) + "", inflater.inflate(R.layout.table_row, null), myContext, true);
        rowWidget.addEntryField(rowData);

        Map<String, String> baseColHash = Tools.copyKeyHash(myContext.getKeyHash());

        // Keep track of how many actual data cells are added to match with WF_Cell list in rowWidget
        int dataCellsAddedToRowWidget = 0;

        // Add data cells
        for (int i = 0; i < columnDefinitions.size(); i++) {
            ColumnDefinition colDef = columnDefinitions.get(i);
            if (colDef.isAggregate) continue; // Skip aggregate columns for regular cell adding

            Map<String, String> colHash = new HashMap<>(baseColHash);
            if (myVariator != null && colDef.key != null) {
                colHash.put(myVariator, colDef.key);
            }

            String cellBgColor = colDef.backgroundColor;
            String cellTextColor = colDef.textColor;
            if ("simple".equals(colDef.type)) { // Use null colors for simple (checkbox) type
                cellBgColor = null;
                cellTextColor = null;
            }

            rowWidget.addCell(colDef.label, colDef.key, colHash, colDef.type, colDef.width, cellBgColor, cellTextColor, colDef.isAggregate? WF_Cell.CellType.Aggregate:WF_Cell.CellType.Normal);

            // Set visibility for the newly added data cellco
            List<WF_Cell> cellsInRowWidget = rowWidget.getCells();
            if (cellsInRowWidget != null && dataCellsAddedToRowWidget < cellsInRowWidget.size()) {
                WF_Cell cell = cellsInRowWidget.get(dataCellsAddedToRowWidget);
                if (cell != null && cell.getWidget() != null) {
                    cell.getWidget().setVisibility(colDef.isVisible ? View.VISIBLE : View.GONE);
                }
            }
            dataCellsAddedToRowWidget++;
        }

        // Add cells for aggregate columns
        if (myContext != null && myContext.getEventListeners(Event.EventType.onSave) != null) {
            for (Object listener : myContext.getEventListeners(Event.EventType.onSave)) {
                if (listener instanceof AggregateColumn) {
                    AggregateColumn aggCol = (AggregateColumn) listener;
                    ColumnDefinition aggColDef = null;
                    for(ColumnDefinition cd : columnDefinitions) {
                        if (cd.isAggregate && cd.label.equals(aggCol.label)) {
                            aggColDef = cd;
                            break;
                        }
                    }

                    View aggCellView;
                    int aggWidth = (aggColDef != null) ? aggColDef.width : 50;
                    String aggBgColor = (aggColDef != null) ? aggColDef.backgroundColor : null;
                    String aggTextColor = (aggColDef != null) ? aggColDef.textColor : null;

                    if (!aggCol.isLogical) {
                        TextView tv = rowWidget.addAggregateTextCell(aggBgColor, aggTextColor);
                        tv.setMinWidth(aggWidth);
                        aggCellView = tv;
                    } else {
                        CheckBox cb = rowWidget.addAggregateLogicalCell(aggBgColor, aggTextColor);
                        TableRow.LayoutParams params = new TableRow.LayoutParams(aggWidth, TableRow.LayoutParams.WRAP_CONTENT);
                        params.gravity = android.view.Gravity.CENTER;
                        cb.setLayoutParams(params);
                        aggCellView = cb;
                    }
                    aggCol.addCell(aggCellView);
                    if (aggColDef != null && aggCellView != null) {
                        aggCellView.setVisibility(aggColDef.isVisible ? View.VISIBLE : View.GONE);
                    }
                }
            }
        }

        tableRowsDataList.add(rowWidget);
        if (tableBodyAdapter != null) {
            tableBodyAdapter.notifyItemInserted(tableRowsDataList.size() - 1);
        }
    }

    public void addRows(List<List<String>> rows,String variatorColumn, String selectionPattern) {
        if (gs == null || al == null) { Log.e("PageWithTable", "gs or al not initialized in addRows!"); return; }
        if (myContext == null) { myContext = getCurrentContext(); if (myContext == null) { Log.e("PageWithTable", "myContext still null in addRows!"); return; } }
        this.myVariator = variatorColumn;
        if (gs.getDb() != null && myContext.getKeyHash() != null) {
            this.allInstances = gs.getDb().preFetchValues(myContext.getKeyHash(), selectionPattern, myVariator);
        } else { this.allInstances = new HashMap<>(); }
        Log.d("nils","in addRows. AllInstances: "+ (allInstances != null ? allInstances.size() : "null"));
        Map<String,List<String>> uRows = new HashMap<>();
        for (List<String> row : rows) {
            String key = al.getEntryLabel(row);
            if (uRows.get(key) == null) uRows.put(key, row);
            Set<String> s = varIdMap.get(key);
            if (s == null) { s = new HashSet<>(); varIdMap.put(key, s); }
            s.add(al.getVarName(row));
        }
        for (String rowKey : uRows.keySet()) { addRow(uRows.get(rowKey)); }
    }

    @Override
    public void syncScroll(HorizontalScrollView source, int scrollX) {
        if (isProgrammaticScroll) return;
        isProgrammaticScroll = true;
        if (stickyHeaderScrollView != null && source != stickyHeaderScrollView) {
            stickyHeaderScrollView.scrollTo(scrollX, 0);
        }
        if (tableRecyclerView != null) {
            for (int i = 0; i < tableRecyclerView.getChildCount(); i++) {
                View child = tableRecyclerView.getChildAt(i);
                RecyclerView.ViewHolder rawViewHolder = tableRecyclerView.getChildViewHolder(child);
                if (rawViewHolder instanceof TableBodyAdapter.RowViewHolder) {
                    TableBodyAdapter.RowViewHolder viewHolder = (TableBodyAdapter.RowViewHolder) rawViewHolder;
                    if (viewHolder.rowScrollView != source) {
                        viewHolder.rowScrollView.scrollTo(scrollX, 0);
                    }
                }
            }
        }
        isProgrammaticScroll = false;
    }

    @Override
    public void addScrollListener(HorizontalScrollView scrollView) {
        if (scrollView != null) {
            scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                if (!isProgrammaticScroll && v == scrollView) {
                    syncScroll(scrollView, scrollX);
                }
            });
        }
    }

    @Override
    public void removeScrollListener(HorizontalScrollView scrollView) {
        if (scrollView != null) {
            scrollView.setOnScrollChangeListener(null);
        }
    }

    private void addDummyData() {
        List<String> initialLabels = new ArrayList<>();
        initialLabels.add("DummyH1");
        initialLabels.add("DummyH2");
        List<String> initialKeys = new ArrayList<>();
        initialKeys.add("key1");
        initialKeys.add("key2");
        addColumns(initialLabels, initialKeys, "text", "220", "#E0E0E0", "#000000");
        addColumns(Collections.singletonList("SimpleCol"), Collections.singletonList("keySimple"), "simple", "100", null, null); // Add a simple column
        addAggregateColumn("AggCol1", null, "SUM", "#,##0", "150", true, "#D1C4E9", "#4A148C");
        for (int i = 1; i <= 3; i++) {
            List<String> row = new ArrayList<>();
            row.add("R" + i + "Val1");
            row.add("R" + i + "Val2");
            row.add("true"); // Value for simple column
            row.add("R" + i + "AggVal");
            addRow(row);
        }
    }

    @Override
    protected List<WF_Container> getContainers() {
        ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
        if (my_root != null) {
            WF_Container root = new WF_Container("root",my_root, null);
            ret.add(root);
            ret.add(new WF_Container("filter_panel",filterPanel,root));
            ret.add(new WF_Container("filter_C1",filterC1,root));
            ret.add(new WF_Container("filter_C2",filterC2,root));
        } else {
            Log.w("PageWithTable", "my_root is null in getContainers.");
        }
        return ret;
    }

    public void setSelectedColumnIndex(int myHeaderIndex) { /* TODO */ }
    public int getSelectedColumnIndex() { return currentlyFocusedColumn; }
    @Override
    public boolean execute(String function, String target) {

        Log.d("cair","Called execute with target "+target);
        if (target.equals("filter_C1")) {
            showFilterPopup(filterC1o,"hubba");
        } else if (target.equals("filter_C2")) {
            showFilterPopup(filterC2o, "bubba");

        }
            return false;
    }

    public void addVariableToEveryCell(String variableSuffix,
                                       boolean displayOut, String format, boolean isVisible,
                                       boolean showHistorical, String initialValue) {
        if (o == null && gs != null) { o = gs.getLogger(); }
        if (o == null) { Log.e("PageWithTable", "'o' (logger/output) is not defined in addVariableToEveryCell."); }

        for (WF_Table_Row_Recycle wft : tableRowsDataList) {
            Set<String> varIds = varIdMap.get(wft.getLabel());
            String varGrId=null;
            if (varIds==null) { Log.e("vortex","No varIds for "+wft.getLabel()); continue; }
            else { for (String varGr:varIds) { if (varGr.endsWith(variableSuffix)) { varGrId = varGr; break; } } }
            if (varGrId==null) { Log.e("vortex","no var for suffix: "+variableSuffix +" for row "+wft.getLabel()); if (o != null) { o.addRow(""); o.addRedText("Could not add var with suffix: "+variableSuffix); } continue; }
            if (tableTypeSimple && gs != null && gs.getVariableConfiguration() != null) {
                List<String> rowDef = gs.getVariableConfiguration().getCompleteVariableDefinition(varGrId);
                if (rowDef != null) {
                    Variable.DataType type = gs.getVariableConfiguration().getnumType(rowDef);
                    if (type != Variable.DataType.bool) {
                        Log.e("vortex", "Non-boolean var in simple column.");
                        if (o != null) o.addRedText("Var [" + variableSuffix+ "] not Boolean.");
                        return;
                    }
                }
            }
            Map<String, String> valueMap = (allInstances != null) ? allInstances.get(varGrId) : null;
            int dataCellIndex = 0; // Index for accessing cells in wft.getCells() which only contains data cells
            for (int colDefIndex = 0; colDefIndex < columnDefinitions.size(); colDefIndex++) {
                ColumnDefinition colDef = columnDefinitions.get(colDefIndex);
                if (colDef.isAggregate) { continue; } // Skip aggregate columns for this variable assignment

                if (wft.getCells() != null && dataCellIndex < wft.getCells().size()) {
                    WF_Cell cell = wft.getCells().get(dataCellIndex);
                    String colKey = colDef.key;
                    String prefetchValue = (valueMap!=null) ? valueMap.get(colKey) : null;
                    cell.addVariable(varGrId, displayOut, format, isVisible, showHistorical,prefetchValue);
                } else {
                    Log.w("PageWithTable", "Data cell index OOB or cells null in addVarToCell. Row: "+wft.getLabel()+", DataCellIdx: "+dataCellIndex+", ColDefIdx: "+colDefIndex);
                    break;
                }
                dataCellIndex++;
            }
        }
    }

    public void setTableName(String name) {
        if (headerRow != null && headerRow.getWidget() != null) {
            TextView headerTV = headerRow.getWidget().findViewById(R.id.headerT);
            if (headerTV != null) {
                headerTV.setText(name);
            }
        }
    }


}
