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

	/**
	 * 
	 */
	private static final long serialVersionUID = -2542614941496953004L;
	private final String label;
    private final String textColor;
    private final String bgColor;
    final private int bg_default_color = R.color.primary,
            text_default_color = R.color.primary_text;

	public MenuHeaderBlock(String id, String label, String textColor,
			String bgColor) {
		this.blockId=id;
		this.label=label;
		this.textColor=textColor;
		this.bgColor=bgColor;

	}

	public void create(WF_Context wf_context) {
		//Log.d("vortex","In create menuheader");
		
		GlobalState gs = GlobalState.getInstance();
		//Figure out the correct colors.
		Context ctx = wf_context.getContext();
		try {
			int _bgColor = Tools.getColorResource(ctx, bgColor,bg_default_color);
			int _textColor = Tools.getColorResource(ctx,textColor,text_default_color);
            Log.d("plax","entrycolors: "+_bgColor+" and "+_textColor+ " for "+bgColor+" and "+textColor);

			gs.getDrawerMenu().addHeader(label, _bgColor, _textColor);
		} catch (IllegalArgumentException e) {
		    Log.e("vortex","Couldn't deal with color: "+bgColor+" or "+textColor);
        }

	}



}
