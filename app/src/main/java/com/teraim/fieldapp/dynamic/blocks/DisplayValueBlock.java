package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventGenerator;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_DisplayValueField;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.utils.Tools.Unit;

public class DisplayValueBlock extends DisplayFieldBlock implements EventGenerator {
	private final String namn;
	private final String label;
	private final String formula;
	private final String containerId;
	private final String format;
	private boolean isVisible = false;
	private final Unit unit;

    public DisplayValueBlock(String id,String namn, String label,Unit unit,
			String formula, String containerId,boolean isVisible,String format,String textColor,String bgColor,String verticalFormat,String verticalMargin ) {
		super(textColor,bgColor,verticalFormat,verticalMargin);
		this.blockId=id;
		this.unit=unit;
		this.namn=namn;
        this.label=label;
		this.formula=formula;
		this.containerId=containerId;
		this.isVisible=isVisible;
		this.format=format;

	}

	public void create(final WF_Context myContext) {
        GlobalState gs = GlobalState.getInstance();
		o= gs.getLogger();
		Container myContainer = myContext.getContainer(containerId);
		if (myContainer != null) {
		
		WF_DisplayValueField vf = new WF_DisplayValueField(namn,formula,myContext,unit,label,isVisible,format, this);
		myContainer.add(vf);
		vf.onEvent(new WF_Event_OnSave(namn));
		}  else {
			o.addCriticalText("Failed to add display value block with id "+blockId+" - missing container "+containerId);
		}
			
	}
}
