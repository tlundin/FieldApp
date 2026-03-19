package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;

public class WF_ClickableField_Selection extends WF_ClickableField {
	private static final String TAG = "WF_ClickableField_Selection";

	private final int outputValueTextSizeSp;

    @SuppressWarnings("WrongConstant")
	public WF_ClickableField_Selection(String headerT, String descriptionT,
									   WF_Context context, String id, boolean isVisible, DisplayFieldBlock format,
									   int outputValueTextSizeSp) {
		super(headerT,descriptionT, context, id,
				LayoutInflater.from(context.getContext()).inflate(format.isHorisontal()?R.layout.selection_field_normal_horizontal:R.layout.selection_field_normal_vertical,null),
				isVisible,format);

		boolean isHorizontal = format != null && format.isHorisontal();
		String layout = isHorizontal ? "selection_field_normal_horizontal" : "selection_field_normal_vertical";
		Log.d(TAG, "id=" + id + ", isHorizontal=" + isHorizontal + ", layout=" + layout);

		this.outputValueTextSizeSp = outputValueTextSizeSp;
    }



	@Override
	public LinearLayout getFieldLayout() {
		LinearLayout ll = (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.output_field_selection_element,null);
		// Apply block text_size to the selected spinner item output field.
		if (outputValueTextSizeSp > 0) {
			TextView outputValueField = ll.findViewById(R.id.outputValueField);
			outputValueField.setTextSize(TypedValue.COMPLEX_UNIT_SP, outputValueTextSizeSp);
		}
		return ll;
	}

	@Override
	protected boolean shouldHideOutputView() {
		return true;
	}


}
