package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;

public class WF_ClickableField_Selection extends WF_ClickableField {


    @SuppressWarnings("WrongConstant")
	public WF_ClickableField_Selection(String headerT, String descriptionT,
									   WF_Context context, String id, boolean isVisible, DisplayFieldBlock format) {
		super(headerT,descriptionT, context, id,
				LayoutInflater.from(context.getContext()).inflate(format.isHorisontal()?R.layout.selection_field_normal_horizontal:R.layout.selection_field_normal_vertical,null),
				isVisible,format);

    }



	@Override
	public LinearLayout getFieldLayout() {
		//LayoutInflater.from(context.getContext()).inflate(R.layout.clickable_field_normal,null)
		//return 	(LinearLayout)LayoutInflater.from(ctx).inflate(R.layout.output_field,null);
		//o.setText(varId.getLabel()+": "+value);	
		//u.setText(" ("+varId.getPrintedUnit()+")");
		return (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.output_field_selection_element,null);
	}

	@Override
	protected boolean shouldHideOutputView() {
		return true;
	}


}
