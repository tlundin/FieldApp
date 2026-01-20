package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.utils.Tools;

public class WF_TextBlockWidget extends WF_Widget {
	private static final String TAG = "WF_TextBlockWidget";


	public WF_TextBlockWidget(WF_Context ctx,String label,String background,String id, boolean isVisible,int textSize, int horizontalMargin,int verticalMargin) {
		super(id, LayoutInflater.from(ctx.getContext()).inflate(R.layout.text_block,null), isVisible,ctx);
		if (label!=null) {
			Log.d(TAG,"Label is: "+label);
			Log.d(TAG,"Margins: "+horizontalMargin+","+verticalMargin+" t: "+textSize);
			TextView tv = getWidget().findViewById(R.id.text_block);
			//tv.setText("Sliders in a group can have values auto-level In this case to a sum of 100.");
			tv.setText(Html.fromHtml(label));
			if (textSize!=-1)
				tv.setTextSize(TypedValue.COMPLEX_UNIT_SP,textSize);
			ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams)tv.getLayoutParams();
			lp.setMargins(horizontalMargin,verticalMargin,horizontalMargin,verticalMargin);
		}
		if (background!=null)
			getWidget().setBackgroundColor(Tools.getColorResource(ctx.getContext(),background));
	}
	
	
}
