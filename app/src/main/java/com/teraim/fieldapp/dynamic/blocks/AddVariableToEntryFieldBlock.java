package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;


public class AddVariableToEntryFieldBlock extends Block {

	
	/**
	 * 
	 */

	private final boolean displayOut;
    private final boolean isVisible;
    private final boolean showHistorical;
	private final String target;
    private final String namn;
    private final String format;
    private final String initialValue;

    public AddVariableToEntryFieldBlock(String id,String target,String namn,boolean displayOut,String format,boolean isVisible,boolean showHistorical,String initialValue)  {
		this.blockId=id;
		this.target=target;
		this.namn=namn;
		this.displayOut=displayOut;
		this.format = format;
		this.isVisible=isVisible;
		this.initialValue=initialValue;
		this.showHistorical=showHistorical;

	}
	
	public Variable create(WF_Context myContext) {
        GlobalState gs = GlobalState.getInstance();
		o = gs.getLogger();

		WF_ClickableField myField = (WF_ClickableField)myContext.getDrawable(target);
		if (myField == null) {

			o.addCriticalText("Couldn't find Entry Field with name "+target+" in AddVariableToEntryBlock" );
			myContext.printD();
			
		} else {
			Variable var =  gs.getVariableCache().getVariable(namn,initialValue,-1);
			if (var!=null) {
				myField.addVariable(var, displayOut, format,isVisible,showHistorical);
				return var;
			} else {
				o.addCriticalText("Couldn't find Variable with name "+namn+" in AddVariableToEntryBlock" );
			}

		}
		return null;
	}
}
