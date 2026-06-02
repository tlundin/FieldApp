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
		if (gs == null) {
			return;
		}
		// Same logic as in MenuHeaderBlock: if a previous run already defined
		// the menu and this context has not yet marked that it "hasMenu", this
		// is the first block in a new menu build. Clear the existing menu and
		// definition so we rebuild cleanly.
		if (gs.isMenuDefined() && !wf_context.hasMenu()) {
			if (gs.getDrawerMenu() != null) {
				gs.getDrawerMenu().clear();
			}
			gs.clearMenuDefinition();
		}
		Workflow wf = gs.getWorkflow(target);
		Context ctx = wf_context.getContext();
		try {
			if (wf == null)
				gs.getLogger().addCriticalText("Workflow "+target+" not found!!");
			else {
				String label = wf.getLabel();
				gs.getDrawerMenu().addItem(label, wf, textColor, bgColor);
				// Record the menu entry so it can be re-applied to a new
				// DrawerMenu instance without re-running this block.
				gs.recordMenuEntry(target, textColor, bgColor);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

}
