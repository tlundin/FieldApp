package com.teraim.fieldapp.dynamic.blocks;

import android.os.AsyncTask;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.AsyncResumeExecutorI;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Alphanumeric_Sorter;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_IndexOrder_Sorter;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Instance_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_List_UpdateOnSaveEvent;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_TimeOrder_Sorter;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_OnlyWithValue_Filter;
import com.teraim.fieldapp.log.LogRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class BlockCreateListEntriesFromFieldList extends DisplayFieldBlock {
    private static final String TAG = "BlockCreateListEntriesFromFieldList";



    private static final Map <String,List<List<String>>> cacheMap=new HashMap <String,List<List<String>>>();
    private final String id;
    private final String type;
    private final String containerId;
    private final String selectionPattern;
    private final String selectionField;
    private final String variatorColumn;


    private transient WF_Static_List myList = null;
    private transient List<AddVariableToEveryListEntryBlock> associatedVariablesList;
    private transient List<AddFilter>associatedFiltersList;

    public BlockCreateListEntriesFromFieldList(String id,String namn, String type,
                                               String containerId, String selectionPattern, String selectionField,String variatorColumn,
                                               String textColor, String bgColor,String verticalFormat,String verticalMargin) {
        super(textColor,bgColor,verticalFormat,verticalMargin);

        this.blockId=id;
        this.id = namn;
        this.type = type;
        this.containerId = containerId;
        this.selectionPattern = selectionPattern;
        this.selectionField = selectionField;
        this.variatorColumn=variatorColumn;
    }

    public void create(WF_Context myContext) {
        //prefetch values from db.
        o = LogRepository.getInstance();
        associatedFiltersList=null;
        associatedVariablesList=null;

        Container myContainer = myContext.getContainer(containerId);
        if (myContainer != null) {

            boolean isVisible = true;
            if (type.equals("selected_values_list")) {
                o.addText("This is a selected values type list. Adding Time Order sorter.");
                myList = new WF_List_UpdateOnSaveEvent(id, myContext, isVisible, this);
                myList.addSorter(new WF_TimeOrder_Sorter());
                o.addText("Adding Filter Type: only instantiated");
                myList.addFilter(new WF_OnlyWithValue_Filter(id));
            } else {
                if (type.equals("selection_list")) {
                    o.addText("This is a selection list. Adding Alphanumeric sorter.");
                    myList = new WF_List_UpdateOnSaveEvent(id, myContext, isVisible, this);
                    myList.addSorter(new WF_Alphanumeric_Sorter());
                } else if (type.equals("instance_list")) {
                    o.addText("instance selection list. Time sorter.");
                    myList = new WF_Instance_List(id, myContext, variatorColumn, isVisible, this);
                    myList.addSorter(new WF_IndexOrder_Sorter());
                } else {
                    //TODO: Find other solution
                    myList = new WF_List_UpdateOnSaveEvent(id, myContext, isVisible, this);
                    myList.addSorter(new WF_Alphanumeric_Sorter());
                }
            }

            if (myList != null) {
                myContainer.add(myList);
                myContext.addList(myList);
                //Return true if instance list. Otherwise false (true = list is ready. False=async creation ongoing)

            }
        } else {
            o.addText("");
            o.addCriticalText("Failed to add list entries block with id " + blockId + " - missing container " + containerId);
        }

    }
    public void generate(final WF_Context myContext, final AsyncResumeExecutorI mom) {

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();
                List<List<String>> rows = cacheMap.get(blockId);
                Log.d(TAG, "selectionField: "+selectionField+" selectionPattern: "+selectionPattern+" rows: " + (rows==null?"null":rows.size()));

                if (rows == null) {

                    rows = al.getTable().getRowsContaining(selectionField, selectionPattern);
                    Log.d(TAG, " rows: " + rows.size());

                    if (associatedFiltersList!=null) {
                        for (AddFilter f : associatedFiltersList) {
                            rows = getRowsContaining(al, rows, f.getSelectionField(), f.getSelectionPattern());
                            Log.d(TAG, "filtered rows size: " + rows.size());
                        }
                    }
                    cacheMap.put(blockId, rows);

                }
                if (rows.size() == 0) {
                    Log.e("vortex", "Selectionfield: " + selectionField + " selectionPattern: " + selectionPattern + " returns zero rows! List cannot be created");
                    o.addText("");
                    o.addCriticalText("Selectionfield: " + selectionField + " selectionPattern: " + selectionPattern + " returns zero rows! List cannot be created");
                    al.getTable().printTable();
                } else {
                    myList.setRows(rows);
                    Log.d(TAG,"try to reuse");

                }
                createVariables(myContext);
                if (mom !=null)
                    mom.continueExecution("draw");
            }
        });

    }




    private List<List<String>> getRowsContaining(VariableConfiguration al,List<List<String>> rows, String columnName, String pattern) {
        String colValue;
        List<List<String>> ret = new ArrayList<List<String>>();
        for (List<String> row: rows) {
            colValue=al.getColumn(columnName, row);
            if (colValue!=null && (colValue.equals(pattern)||colValue.matches(pattern))) {
                ret.add(row);
            }
        }
        return ret;
    }





    public void associateVariableBlock(AddVariableToEveryListEntryBlock bl) {
        if (associatedVariablesList==null)
            associatedVariablesList = new ArrayList<>();
        associatedVariablesList.add(bl);
    }

    public void associateFilterBlock(AddFilter bl) {
        if (associatedFiltersList==null)
            associatedFiltersList=new ArrayList<>();
        associatedFiltersList.add(bl);

    }

    public String getListId() {
        return id;
    }

    public void createVariables(WF_Context myContext) {
        for (AddVariableToEveryListEntryBlock bl:associatedVariablesList){
            bl.create(myContext);
        }
    }
}

