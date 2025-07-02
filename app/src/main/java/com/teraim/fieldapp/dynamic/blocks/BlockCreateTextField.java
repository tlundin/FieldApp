package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_TextBlockWidget;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

import java.util.List;

public class BlockCreateTextField extends Block {

	/**
	 * 
	 */

	private final String background;
    private final String containerId;
	private boolean isVisible = true;
	private final List<EvalExpr> labelE;
	private int horizontalMargin;
    private int verticalMargin;
    private int textSize;
	
	public BlockCreateTextField(String id, String label, String background, String containerId, boolean isVisible,String textSize,String horiz,String vert) {
		this.blockId=id;
        this.labelE=Expressor.preCompileExpression(label);
		this.containerId=containerId;
		this.isVisible = isVisible;
		this.background = background;
		try {
			horizontalMargin=Integer.parseInt(horiz);

		} catch (NumberFormatException e) {
			horizontalMargin=5;

		}
		try {
			verticalMargin=Integer.parseInt(vert);

		} catch (NumberFormatException e) {
			verticalMargin=5;

		}
		try {
			this.textSize=Integer.parseInt(textSize);
		} catch (NumberFormatException e) {
			this.textSize=-1;
		}
	}

	
	
	
	public void create(WF_Context ctx) {
		o = LogRepository.getInstance();
		//Identify targetList. If no list, no game.
		Container myContainer = ctx.getContainer(containerId);
		if (myContainer != null)  {
			myContainer.add(new WF_TextBlockWidget(ctx,Expressor.analyze(labelE),background,blockId,isVisible,textSize,horizontalMargin, verticalMargin));
			o.addText("Added new TextField with ID"+blockId);
		} else {
			o.addCriticalText("Failed to add text field block with id "+blockId+" - missing container "+containerId);
		}
		
	}	
	
			
		
		
}


