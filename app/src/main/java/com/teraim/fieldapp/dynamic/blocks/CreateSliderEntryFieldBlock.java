package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_ClickableField_Slider;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;

public class CreateSliderEntryFieldBlock extends DisplayFieldBlock {

	private String name;
    private final String label;
    private final String containerId;
    private final String initialValue;
    private final String group;
	private final int min,max;
    private boolean isVisible = false;
    private final boolean showHistorical;
	private String variableName=null;
	private transient WF_ClickableField myField;
	public CreateSliderEntryFieldBlock(String id, String name,
									   String containerId, boolean isVisible, boolean showHistorical, String initialValue, String label, String variableName, String group,String textColor,String backgroundColor,int min,int max,String verticalFormat,String verticalMargin) {
		super(textColor,backgroundColor,verticalFormat,verticalMargin);
		this.name = name;
		this.group = group;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.blockId=id;
		this.initialValue=initialValue;
		this.showHistorical=showHistorical;
		this.label=label;
		this.variableName = variableName;
		this.min=min;
		this.max=max;
		if (name==null || name.isEmpty()) {
			this.name = id;
		}

	}

	/**
	 * @return the name
	 */
    private String getName() {
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
			Log.d("vortex","In slider create with hash: "+ gs.getVariableCache().getContext());
			Variable v = gs.getVariableCache().getVariable(variableName,initialValue,-1);
			if (v == null) {
				o.addText("");
				o.addCriticalText("Failed to create entryfield for block " + blockId);
				Log.d("nils", "Variable " + variableName + " referenced in block_create_entry_field not found.");
				o.addText("");
				o.addCriticalText("Variable ["+variableName+"] referenced in block_create_slider_entry_field "+this.getBlockId()+" not found.");
				o.addText("");
				o.addCriticalText("Current context: ["+ gs.getVariableCache().getContext()+"]");
			} else {
				if (v.getType()!= Variable.DataType.numeric ) {
					Log.d("vortex","variable "+variableName+" is not numeric in create_slider.");
					o.addText("");
					o.addCriticalText("Variable ["+variableName+"] referenced in block_create_slider_field "+this.getBlockId()+" is not of type numeric");
					return null;
				}
				Log.d("vortex", "current hash: " + gs.getVariableCache().getContext());
				myField = new WF_ClickableField_Slider(label==null||label.equals("")?v.getLabel():label, "This is a description for the entryfield"
						, myContext, name, isVisible,group,min,max,this);
				Log.d("nils", "In CreateSliderEntryFieldBlock.");
				myField.addVariable(v, true,"slider",true,showHistorical);
				myContext.addDrawable(v.getId(), myField);

				Log.d("vortex", "Adding Entryfield " + getName() + " to container " + containerId);
				o.addText("Adding Entryfield " + getName() + " to container " + containerId);
				myContainer.add(myField);
				//				myField.refreshInputFields();	
				//myField.refresh();
				return v;
			}

		} else {
			Log.e("vortex","Container null! Cannot add entryfield!");
			o.addText("");
			o.addCriticalText("Adding Entryfield for "+name+" failed. Container not configured");

		}
		return null;
	}
}


