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
	/**
	 * 
	 */
	private static final long serialVersionUID = 6297016520560017438L;
	private final String target;
    private final String bgColor;
    private final String textColor;

	final private int bg_default_color = R.color.primary,
			text_default_color = R.color.primary_text;


	public MenuEntryBlock(String id, String target, String type, String bgColor, String textColor) {
		this.blockId=id;
		this.target=target;
		this.bgColor=bgColor;
		this.textColor=textColor;
	}
	public void create(WF_Context wf_context) {
		//Log.d("vortex","In create menuentry");
		
		GlobalState gs = GlobalState.getInstance();
		Workflow wf = gs.getWorkflow(target);

		Context ctx = wf_context.getContext();
		try {
		    //Log.d("vortex","Package name: "+ctx.getPackageName());
			int _bgColor = Tools.getColorResource(ctx, bgColor,bg_default_color);
			int _textColor = Tools.getColorResource(ctx,textColor,text_default_color);
            //Log.d("flax","entrycolors: "+_bgColor+" and "+_textColor+ " for "+bgColor+" and "+textColor);

			if (wf == null)
				gs.getLogger().addRedText("Workflow "+target+" not found!!");
			else {
				String label = wf.getLabel();
				gs.getDrawerMenu().addItem(label,wf,_bgColor,_textColor);
			}

		} catch (IllegalArgumentException e) {
			Log.e("vortex","Couldn't deal with color: "+bgColor+" or "+textColor);
		}

	}

}
