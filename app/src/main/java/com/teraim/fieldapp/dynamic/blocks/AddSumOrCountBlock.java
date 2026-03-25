package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Not_ClickableField_SumAndCountOfVariables;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.NamedVariables;

/**Blocks that so far implements only signal
 * 
 * @author Terje
 *
 */
public  class AddSumOrCountBlock extends DisplayFieldBlock {
	private static final String TAG = "AddSumOrCountBlock";
	/**
	 * 
	 */

	private final String containerId;
    private final String label;
    private final String myPattern;
    private final String target;
    private final String result;
	private final WF_Not_ClickableField_SumAndCountOfVariables.Type type;
	private final String format;

	private boolean isVisible = true;
	private transient VariableConfiguration al;
	public AddSumOrCountBlock(String id,String containerId, String label,String postLabel,
			String filter, String target,
			WF_Not_ClickableField_SumAndCountOfVariables.Type sumOrCount,String result,
			boolean isVisible, String format, String textColor, String bgColor,String verticalFormat,String verticalMargin) {
		super(textColor,bgColor,verticalFormat,verticalMargin);
		this.containerId=containerId;
		this.label=label;
		this.myPattern=filter; //.trim();
		this.target=target;
		type = sumOrCount;
		this.result = result;
        this.isVisible = isVisible;
		this.format = format;
		this.blockId=id;

	}

	public void create(WF_Context myContext) {
		o = LogRepository.getInstance();
		

		Container myContainer = myContext.getContainer(containerId);
		if (myContainer!=null) {
			String fieldId = (label != null && !label.isEmpty()) ? label : blockId;
			WF_Not_ClickableField_SumAndCountOfVariables.Type effectiveType = type;
			if (effectiveType == null) {
				// Defensive fallback for old serialized blocks where type could be missing.
				// Infer mode from metadata/result variable instead of hardcoding COUNT.
				effectiveType = inferTypeFromMetadata();
				Log.e(TAG, "type is null for block " + blockId + " (" + label + "). Inferred " + effectiveType + ".");
				o.addCriticalText("AddSumOrCountBlock type missing for block " + blockId + " (" + label + "). Inferred " + effectiveType + ".");
			}
			WF_Not_ClickableField_SumAndCountOfVariables field = new WF_Not_ClickableField_SumAndCountOfVariables(
					fieldId, label != null ? label : "", myContext, 
					target, myPattern,
					effectiveType,isVisible,
					this);

			if (result == null) {
				o.addText("");
				o.addCriticalText("Error in XML: block_add_sum_of_selected_variables_display is missing a result parameter for:"+label);
			} else {
				Variable v = GlobalState.getInstance().getVariableCache().getVariable(result);
				if (v==null) {
					o.addText("");
					o.addCriticalText("Error in block_add_sum_of_selected_variables_display: missing variable for result parameter: "+result);
				} else 
					field.addVariable(v, true,format,true);			
			}
			/*

		if (type==WF_Not_ClickableField_SumAndCountOfVariables.Type.count)
			field.addVariable(label, "AntalArter", Unit.nd, Variable.DataType.numeric, Variable.StorageType.delyta, true);
		else
			field.addVariable(label, "SumTackning", Unit.percentage, Variable.DataType.numeric, Variable.StorageType.delyta, true);
			 */



			myContainer.add(field);
		} else {
			o.addText("");
			o.addCriticalText("Cannot add block_add_sum_of_selected_variables_display: missing container");
			
		}
	}

	private WF_Not_ClickableField_SumAndCountOfVariables.Type inferTypeFromMetadata() {
		String resultName = result;
		if (resultName != null) {
			int idx = resultName.indexOf(':');
			if (idx >= 0 && idx < resultName.length() - 1) {
				resultName = resultName.substring(idx + 1);
			}
			resultName = resultName.toLowerCase();
		}
		String blockName = blockId == null ? "" : blockId.toLowerCase();
		String labelName = label == null ? "" : label.toLowerCase();

		if ((resultName != null && resultName.equals(NamedVariables.number_of_selections.toLowerCase()))
				|| blockName.contains("number_of_selections")
				|| labelName.contains("antal")) {
			return WF_Not_ClickableField_SumAndCountOfVariables.Type.count;
		}
		return WF_Not_ClickableField_SumAndCountOfVariables.Type.sum;
	}
}


