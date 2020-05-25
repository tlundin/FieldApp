package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BlockCreateTableEntriesFromFieldList extends Block {

    private String target=null;
    private String selectionField=null;
    private String selectionPattern=null;
    private String variatorColumn=null;
	String typeField=null,keyField = null;
	private static final Map <String,List<List<String>>> cacheMap=new HashMap <String,List<List<String>>>();


	public BlockCreateTableEntriesFromFieldList(String id, String type,String target,
			String selectionField,String selectionPattern,
			String keyField,String labelField,String descriptionField,
			String typeField,String variatorColumn,String uriField
			) {
		super();
        this.selectionField = selectionField;
		this.selectionPattern = selectionPattern;
		this.blockId = id;
        this.variatorColumn = variatorColumn;
		this.target = target;
	}

	public void create(WF_Context myContext) {
		o = GlobalState.getInstance().getLogger();
		WF_Table myTable=null;
			if (target!=null) {
				myTable = myContext.getTable(target);

			}

		if (myTable==null) {
			Log.e("vortex","couldnt find table "+target+" in createTableEntriesFromFieldList, block "+blockId);
			o.addRow("");
			o.addRedText("couldnt find table "+target+" in createTableEntriesFromFieldList, block "+blockId);
			return;
		}
		VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();
			List<List<String>>rows = cacheMap.get(blockId);
			if (rows==null)
				rows  = al.getTable().getRowsContaining(selectionField, selectionPattern);
			if (rows==null||rows.size()==0) {
				Log.e("vortex","Selectionfield: "+selectionField+" selectionPattern: "+selectionPattern+" returns zero rows! List cannot be created");
				o.addRow("");
				o.addRedText("Selectionfield: "+selectionField+" selectionPattern: "+selectionPattern+" returns zero rows! List cannot be created");
			} else {		
				cacheMap.put(blockId, rows);
				Log.d("vortex","Number of rows in CreateEntrieFromList "+rows.size());
				//prefetch values from db.

				myTable.addRows(rows,variatorColumn,selectionPattern);
			}


	}


	/**
	 * 
	 */
	private static final long serialVersionUID = -1074961225196569424L;




}
