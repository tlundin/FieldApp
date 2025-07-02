package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;

public class AddVariableToListEntry extends Block {

	/**
	 * 
	 */

	private boolean isVisible = true;
    private boolean isDisplayed=false;
    private boolean showHistorical=false;
	private final String targetField;
    private final String targetList;
    private final String format;
    private final String varNameSuffix;
    private final String initialValue;

    public AddVariableToListEntry(String id,String varNameSuffix,
			String targetList,String targetField, boolean isDisplayed,String format,boolean isVisible,boolean showHistorical,String initialValue) {
		super();
		this.blockId=id;
		this.isVisible = isVisible;
		this.targetField = targetField;
		this.targetList = targetList;
		this.format = format;
		this.varNameSuffix=varNameSuffix;
		this.isDisplayed=isDisplayed;
		this.showHistorical=showHistorical;
		this.initialValue=initialValue;
	} 


	public Variable create(WF_Context myContext) {
        GlobalState gs = GlobalState.getInstance();
		o = gs.getLogger();
		VariableConfiguration al = gs.getVariableConfiguration();
		
		WF_Static_List l= myContext.getList(targetList);
			if (l!=null) {
				Log.d("nils","Found entry field in AddVariableToListEntry");
				Variable var = l.addVariableToListEntry(varNameSuffix,isDisplayed,targetField,format,isVisible,showHistorical,initialValue);
				if (var == null) {
					Log.e("nils","Didn't find list entry"+targetField+ " in AddVariableToListEntry");
				} else
					return var;
			} else
				Log.e("nils","Didn't find list in AddVariableToListEntry");
				
		return null;
	}
		
		
	
}
