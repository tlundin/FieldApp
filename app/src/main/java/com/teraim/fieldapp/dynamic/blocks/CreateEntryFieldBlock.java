package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField_Selection_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.utils.Tools.Unit;

public class CreateEntryFieldBlock extends DisplayFieldBlock {

	private transient WF_ClickableField myField;
	private final String name;
    private final String label;
    private final String containerId;
    private final String initialValue;
    private boolean isVisible = false;
    private final boolean showHistorical;
    private boolean autoOpenSpinner=true;
	private final String format;
	public CreateEntryFieldBlock(String id,String name, 
			String containerId,boolean isVisible,String format,boolean showHistorical,String initialValue, String label, boolean autoOpenSpinner,String textColor,String backgroundColor,String verticalFormat,String verticalMargin) {
		super(textColor,backgroundColor,verticalFormat,verticalMargin);
		this.name = name;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.format = format;
		this.blockId=id;
		this.initialValue=initialValue;
		this.showHistorical=showHistorical;
		this.label=label;
		this.autoOpenSpinner=autoOpenSpinner;

	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}



	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}






	public Variable create(WF_Context myContext) {
        GlobalState gs = GlobalState.getInstance();
		Container myContainer = myContext.getContainer(containerId);
		o = gs.getLogger();
		if(myContainer !=null) {
			VariableConfiguration al = gs.getVariableConfiguration();
			Log.d("vortex","current hash: "+ gs.getVariableCache().getContext());
			Variable v = gs.getVariableCache().getVariable(name,initialValue,-1);
			if (v == null) {
				o.addText("");
				o.addCriticalText("Failed to create entryfield for block " + blockId);
				Log.d("nils", "Variable " + name + " referenced in block_create_entry_field not found.");
				o.addCriticalText("Variable ["+name+"] referenced in block_create_entry_field not found.");
				o.addCriticalText("Current DB Context: ["+ gs.getVariableCache().getContext()+"]");
			} else	{	
				myField = new WF_ClickableField_Selection_OnSave(label==null||label.equals("")?v.getLabel():label,
						al.getDescription(v.getBackingDataSet()),myContext,name,isVisible,autoOpenSpinner,this);
				//Log.d("nils", "In CreateEntryField. Description: "+al.getDescription(v.getBackingDataSet()));
				//Log.d("nils","Backing data: "+v.getBackingDataSet().toString());
				myField.addVariable(v, true,format,true,showHistorical);
				myContext.addDrawable(v.getId(), myField);

				Log.d("vortex","Adding Entryfield "+v.getId()+" to container "+containerId);
				o.addText("Adding Entryfield "+v.getId()+" to container "+containerId);
				myContainer.add(myField);
				//				myField.refreshInputFields();	
				//myField.refresh();

			}

			return v;
		} else {
			Log.e("vortex","Container null! Cannot add entryfield!");
			o.addText("");
			o.addCriticalText("Adding Entryfield for "+name+" failed. Container "+containerId+" not configured");
			return null;
		}
	}

	public void attachRule(Rule r) {
		if (myField == null) {
			Log.e("vortex","no entryfield created. Rule block before entryfield block?");
		} else {
			myField.attachRule(r);
		}
	}




}


