package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.templates.PageWithTable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Table;

public class BlockAddVariableToTable extends Block {


	private String target=null;
    private String variableSuffix=null;
    private String format=null;
    private String initialValue=null;
	private boolean displayOut=false;
    private boolean isVisible=true;
    private boolean showHistorical=false;
	
	
	public BlockAddVariableToTable(String id,String target, String variableSuffix,
			boolean displayOut,String format,boolean isVisible,boolean showHistorical, String initialValue 
			 ) {
		super();
		this.target = target;
		this.variableSuffix = variableSuffix;
		this.format = format;
		this.blockId = id;
		this.initialValue = initialValue;
		this.displayOut = displayOut;
		this.isVisible = isVisible;
		this.showHistorical = showHistorical;
	}
	
	
	public void create(WF_Context myContext) {

		PageWithTable table = (PageWithTable)myContext.getTemplate();
		o = GlobalState.getInstance().getLogger();
		if (table==null) {
			o.addRow("");
			o.addRedText("Couldn't find list with ID "+target+" in AddVariableToEveryListEntryBlock");
		} else {
			Log.d("nils","Calling AddVariableToTable for "+variableSuffix);
			table.addVariableToEveryCell(variableSuffix, displayOut,format,isVisible,showHistorical,initialValue);
		}
    }


}
