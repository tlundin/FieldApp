/**
 * 
 */
package com.teraim.fieldapp.dynamic.blocks;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.utils.Tools;

/**
 * @author tlundin
 *
 */


public class MenuHeaderBlock extends Block {

	private final String label;
    private final String textColor;
    private final String bgColor;
	public MenuHeaderBlock(String id, String label, String textColor,
			String bgColor) {
		this.blockId=id;
		this.label=label;
		this.textColor=textColor;
		this.bgColor=bgColor;
	}
    public void create(WF_Context wf_context) {
		GlobalState gs = GlobalState.getInstance();
		if (gs == null) {
			return;
		}
		// If a menu definition already exists from a previous Activity instance,
		// and this is the first time in this context that we touch the menu,
		// clear both the visual menu and the recorded definition so this run
		// can rebuild a clean menu (avoids duplicates on rotation).
		if (gs.isMenuDefined() && !wf_context.hasMenu()) {
			if (gs.getDrawerMenu() != null) {
				gs.getDrawerMenu().clear();
			}
			gs.clearMenuDefinition();
		}
		Context ctx = wf_context.getContext();
		try {
			gs.getDrawerMenu().addHeader(label, textColor, bgColor);
			// Remember how the menu was built so it can be reconstructed after
			// configuration changes without re-running this workflow.
			gs.recordMenuHeader(label, textColor, bgColor);
		} catch (IllegalArgumentException e) {
		    Log.e("vortex","Couldn't deal with color: "+bgColor+" or "+textColor);
        }
	}
}
