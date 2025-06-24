package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filterable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_Column_Name_Filter;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_Column_Name_Filter.FilterType;
import com.teraim.fieldapp.dynamic.workflow_realizations.filters.WF_Column_RegExp_Filter;
import com.teraim.fieldapp.log.LoggerI;

public class AddFilter extends Block {


	private final String target;
	private final String type;
	private final String selectionField;
	private final String selectionPattern;

	public AddFilter(String id, String target, String type,
					 String selectionField, String selectionPattern, LoggerI o) {
		this.blockId=id;
		this.target=target;
		this.type=type;
		this.selectionField = selectionField;
		this.selectionPattern=selectionPattern;
	}

	public String getSelectionField() {
		return selectionField;
	}

	public String getSelectionPattern() {
		return selectionPattern;
	}

	public void create(WF_Context myContext) {
		o = GlobalState.getInstance().getLogger();
		Filterable myList = myContext.getFilterable(target);
		if (myList==null) {
			o.addRow("");
			o.addRedText("Target list ["+target+"] for block_create_list_filter (blockId: "+blockId+") could not be found");
			return;
		}
		
		if (type ==null) {
			o.addRow("");
			o.addRedText("FilterType was not set in block_create_filter (blockId: "+blockId+"). Cannot execute block.");
			return;
		}
		Log.d("vortex", "Adding filter of type " + type + " with selPat " + selectionPattern + " and selField " + selectionField);
		if (type.equals("regular_expression"))
			myList.addFilter(new WF_Column_RegExp_Filter(blockId,selectionField,selectionPattern));
		else if (type.equals("exact"))
			myList.addFilter(new WF_Column_Name_Filter(blockId,selectionPattern,selectionField,FilterType.exact));
		else if (type.equals("prefix"))
			myList.addFilter(new WF_Column_Name_Filter(blockId,selectionPattern,selectionField,FilterType.prefix));
	}

	public String getTarget() {
		return target;
	}
}
