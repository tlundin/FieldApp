package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;
import android.os.SystemClock;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.templates.PageWithTable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table;
import com.teraim.fieldapp.log.LogRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class BlockCreateTableEntriesFromFieldList extends Block {
	private static final String TAG = "BlockCreateTableEntriesFromFieldList";
	private static final String TIMING_TAG = "TableTiming";


    private String target=null;
    private String selectionField=null;
    private String selectionPattern=null;
    private String variatorColumn=null;
	private static final Map<String,List<List<String>>> cacheMap = new HashMap<>();

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
		final long t0 = SystemClock.elapsedRealtime();
		o = LogRepository.getInstance();
		PageWithTable myTable = (PageWithTable) myContext.getTemplate();

		if (myTable==null) {
			Log.e("vortex","could not find table "+target+" in createTableEntriesFromFieldList, block "+blockId);
			o.addCriticalText("could not find table "+target+" in createTableEntriesFromFieldList, block "+blockId);
			return;
		}
		VariableConfiguration al = GlobalState.getInstance().getVariableConfiguration();
			final String cacheKey = blockId + "|" + selectionField + "|" + selectionPattern + "|" + myContext.getHash();
			List<List<String>> rows = cacheMap.get(cacheKey);
			final long tCacheLookup = SystemClock.elapsedRealtime();
			if (rows==null) {
				rows  = al.getTable().getRowsContaining(selectionField, selectionPattern);
				cacheMap.put(cacheKey, rows);
			}
			final long tAfterRowsLookup = SystemClock.elapsedRealtime();
			if (rows==null||rows.size()==0) {
				Log.e("vortex","Selectionfield: "+selectionField+" selectionPattern: "+selectionPattern+" returns zero rows! List cannot be created");
				o.addCriticalText("Selectionfield: "+selectionField+" selectionPattern: "+selectionPattern+" returns zero rows! List cannot be created");
			} else {		
				Log.d(TAG,"Number of rows in CreateEntrieFromList "+rows.size());
				//prefetch values from db.
				myTable.addRows(rows,variatorColumn,selectionPattern);
			}
			final long tAfterAddRows = SystemClock.elapsedRealtime();
			Log.d(TIMING_TAG,
					"BlockCreateTableEntriesFromFieldList#create id=" + blockId +
							" rows=" + (rows==null ? "null" : rows.size()) +
							" cacheLookup=" + (tCacheLookup - t0) + "ms" +
							" queryOrHit=" + (tAfterRowsLookup - tCacheLookup) + "ms" +
							" addRows=" + (tAfterAddRows - tAfterRowsLookup) + "ms" +
							" total=" + (tAfterAddRows - t0) + "ms");


	}






}
