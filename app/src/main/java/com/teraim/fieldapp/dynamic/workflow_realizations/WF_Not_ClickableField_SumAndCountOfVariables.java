package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.log.LogRepository;

import java.util.HashSet;
import java.util.Set;

public class WF_Not_ClickableField_SumAndCountOfVariables extends WF_Not_ClickableField implements EventListener {

    private final WF_Static_List targetList;
	private final WF_Context myContext;
	private final String myPattern;
	private Set<Variable> allMatchingVariables=null;

	public enum Type {
		sum,
		count
	}
	private final Type myType;

	public WF_Not_ClickableField_SumAndCountOfVariables(String header, String descriptionT, WF_Context myContext,
														String myTarget, String pattern, Type sumOrCount, boolean isVisible, DisplayFieldBlock format) {
		super(header,header, descriptionT, myContext, LayoutInflater.from(myContext.getContext()).inflate(format.isHorisontal()?R.layout.selection_field_normal_horizontal:R.layout.selection_field_normal_vertical,null),isVisible,format);
		this.myContext=myContext;
		o = LogRepository.getInstance();
		targetList = myContext.getList(myTarget);
		myType = sumOrCount;
		myPattern = pattern;

		//TextView text = (TextView)getWidget().findViewById(R.id.editfieldtext);
		//LinearLayout bg = (LinearLayout)getWidget().findViewById(R.id.background);
		//if (bgColor!=null)
		//	bg.setBackgroundColor(Color.parseColor(bgColor));
		//if (textColor!=null)
		//	text.setTextColor(Color.parseColor(textColor));

		if (targetList == null) {
			o.addCriticalText("Couldn't create "+header+" since target list: "+myTarget+" does not exist");
			Log.e("parser","couldn't create SumAndCountOfVariables - could not find target list "+myTarget);
		} else {
			myContext.registerEventListener(this,EventType.onRedraw);
			myContext.registerEventListener(this,EventType.onFlowExecuted);

		}

    }

	@Override
	public LinearLayout getFieldLayout() {

		return (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.output_field_selection_element,null);
	}

	@Override
	protected boolean shouldHideOutputView() {
		return true;
	}


	@Override
	public void onEvent(Event e) {
		Log.d("nils","In ADDNUMBER event targetListId: "+targetList.getId()+" e.getProvider: "+e.getProvider()+
				"type of event: "+e.getType().name());
		if (e.getType().equals(EventType.onFlowExecuted)) {
			long t = System.currentTimeMillis();
			matchAndRecalculateMe();
			refresh();
			Log.d("vortex","sum calc time "+(System.currentTimeMillis()-t));
		} else
			if (e.getProvider().equals(targetList.getId())) {
			//Log.d("nils","This is my list!");
			matchAndRecalculateMe();
			refresh();
		} else
			Log.d("nils","event discarded - from wrong list");

	}

	@Override
	public String getName() {
		return "SUM_AND_COUNT "+this.getId();
	}

	private void matchAndRecalculateMe() {
		String variablesWithNoValue = "[";
		Long sum=Long.valueOf(0);
		if (targetList==null)
			return;
		if (allMatchingVariables==null) {
			allMatchingVariables = new HashSet<Variable>();
			for (Listable l : targetList.get()) {
				Set<Variable> vars = l.getAssociatedVariables();
				for (Variable v : vars) {
//					Log.e("vortex","VAR: "+v.getId());
					if (v.getId().matches(myPattern))
						allMatchingVariables.add(v);
//					else
//						Log.e("vortex","DIDNT MATCH: "+v.getId());
				}
			}
		}


		if (allMatchingVariables.isEmpty()) {
			Log.e("vortex","no variables matching pattern "+myPattern+" in block_add_sum_of_selected_variables_display with target "+targetList.getId());
			o.addCriticalText("no variables matching pattern "+myPattern+" in block_add_sum_of_selected_variables_display with target "+targetList.getId());
		}

		for (Variable v:allMatchingVariables) {
			String val=v.getValue();

			if (val!=null&&!val.isEmpty()) {
				//Log.d("nils","VAR: "+v.getId()+"VALUE: "+v.getValue());
				if (myType == Type.count) {
					sum++;
				}

				else {
					try {
						sum+=Long.parseLong(val);
					} catch (NumberFormatException e) {
						Log.e("vortex","Numberformatexception for "+val);
					}
				}

			} else
				variablesWithNoValue += v.getId()+",";
		}

		if (sum==0) {
			variablesWithNoValue+="]";
			o.addYellowText("Sum zero in Count/Add Block. with pattern ["+myPattern+"] No value found for:");
			o.addText(variablesWithNoValue);
			Log.d("vortex","VARIABLES WITH NO VALUE:"+variablesWithNoValue);
		} else {
			o.addGreenText("Found match(es) in Count/Add Block with pattern ["+myPattern+"]");
		}

		if (myVar !=null)
			myVar.setValue(sum.toString());

	}

}
