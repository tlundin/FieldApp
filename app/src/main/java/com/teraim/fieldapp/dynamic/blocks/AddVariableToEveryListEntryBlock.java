package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;

public class AddVariableToEveryListEntryBlock extends Block {


	private final String target;
    private final String variableSuffix;
    private final String format;
	private final boolean displayOut;
    private final boolean isVisible;
    private final boolean showHistorical;
	private String initialValue=null;




	public AddVariableToEveryListEntryBlock(String id,String target,
			String variableSuffix, boolean displayOut, String format,boolean isVisible,boolean showHistorical,String initialValue) {
		super();

		this.target = target;
		this.variableSuffix = variableSuffix;
		this.displayOut = displayOut;
		this.format = format;
		this.isVisible=isVisible;
		this.blockId=id;
		this.initialValue = initialValue;
		this.showHistorical=showHistorical;
	}



	//addVariableToEveryListEntry(String varSuffix,boolean displayOut)
	//addVariable(String varLabel,Unit unit,String varId,boolean displayOut)

	public void create(WF_Context myContext) {

		final WF_Static_List l = myContext.getList(target);
		if (l==null) {
			o = GlobalState.getInstance().getLogger();
			o.addRow("");
			o.addRedText("Couldn't find list with ID "+target+" in AddVariableToEveryListEntryBlock");
        } else {
			Log.d("nils","Calling AddVariableToEveryListEntry for list "+l.getId()+"and suffix "+variableSuffix);
            l.addVariableToEveryListEntry(variableSuffix, displayOut, format, isVisible, showHistorical, initialValue);
        }

	}

	public String getTarget() {
		return target;
	}
}