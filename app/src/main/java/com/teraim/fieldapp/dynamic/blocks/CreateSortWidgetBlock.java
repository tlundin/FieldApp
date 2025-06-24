package com.teraim.fieldapp.dynamic.blocks;

import android.content.Context;
import android.view.ViewGroup;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Filterable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_List;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_SorterWidget;

public class CreateSortWidgetBlock extends Block {

	private final String containerId;
    private final String type;
    private final String target;
    private final String selF;
    private final String selP;
    private final String dispF;
    private final String name;
    private boolean isVisible = true;
	

	public CreateSortWidgetBlock(String id,String name,String type,String containerId, String targetId,String selectionField,String displayField,String selectionPattern,boolean isVisible) {
		this.type = type;
		this.containerId = containerId;
		this.target = targetId;
		selF = selectionField;
		dispF = displayField;
		selP = selectionPattern;
		this.isVisible = isVisible;
		this.name=name;
		this.blockId=id;
	}


	public void create(WF_Context ctx) {

		o = GlobalState.getInstance().getLogger();
		//Identify targetList. If no list, no game.
		Container myContainer = ctx.getContainer(containerId);
		if (myContainer != null)  {
				
		//Log.d("nils","Sort target is "+target);
            Filterable targetList = ctx.getFilterable(target);
		if (targetList == null) {
			o.addRow("");
			o.addRedText("couldn't create sortwidget - could not find target list: "+target);
			
		}
		else {
			o.addRow("Adding new SorterWidget of type "+type);
			myContainer.add(new WF_SorterWidget(name,ctx,type,((WF_List) targetList),((WF_Container)myContainer).getViewGroup(), selF,dispF,selP,isVisible));
			//myContainer.add(new WF_Widget(buttonPanel));
		}
		
		} else {
			o.addRow("");
			o.addRedText("Failed to add sortwidget block with id "+blockId+" - missing container "+containerId);
		}

	}

	public void draw(Context ctx, ViewGroup container) {

	}
}