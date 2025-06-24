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
		Context ctx = wf_context.getContext();
		try {
			gs.getDrawerMenu().addHeader(label);
		} catch (IllegalArgumentException e) {
		    Log.e("vortex","Couldn't deal with color: "+bgColor+" or "+textColor);
        }
	}
}
