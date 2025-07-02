/**
 * 
 */
package com.teraim.fieldapp.dynamic.blocks;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.utils.Tools;

/**
 * @author tlundin
 *
 */

public class MenuEntryBlock extends Block {

	private final String target;
    private final String bgColor;
    private final String textColor;
	public MenuEntryBlock(String id, String target, String type, String bgColor, String textColor) {
		this.blockId=id;
		this.target=target;
		this.bgColor=bgColor;
		this.textColor=textColor;
	}
	public void create(WF_Context wf_context) {
		GlobalState gs = GlobalState.getInstance();
		Workflow wf = gs.getWorkflow(target);
		Context ctx = wf_context.getContext();
		try {
			if (wf == null)
				gs.getLogger().addCriticalText("Workflow "+target+" not found!!");
			else {
				String label = wf.getLabel();
				gs.getDrawerMenu().addItem(label,wf);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

}
