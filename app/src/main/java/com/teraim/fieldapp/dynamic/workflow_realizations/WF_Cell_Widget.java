package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.non_generics.Constants;

import java.util.Map;

public class WF_Cell_Widget extends WF_ClickableField implements WF_Cell, EventListener {


	private final Map<String, String> myHash;
	private CellType cellType;

	public WF_Cell_Widget(Map<String, String> columnKeyHash, String headerT, String descriptionT,
			WF_Context context, String id,boolean isVisible,CellType type) {
		super(headerT,descriptionT, context, id,
				LayoutInflater.from(context.getContext()).inflate(R.layout.cell_field_normal,null),
				isVisible,new DisplayFieldBlock("black",null,null,null));

		myHash = columnKeyHash;
		cellType = type;
		context.registerEventListener(this, Event.EventType.onSave);
	}



	@Override
	public LinearLayout getFieldLayout() {
		//LayoutInflater.from(context.getContext()).inflate(R.layout.clickable_field_normal,null)
		//return 	(LinearLayout)LayoutInflater.from(ctx).inflate(R.layout.output_field,null);
		//o.setText(varId.getLabel()+": "+value);	
		//u.setText(" ("+varId.getPrintedUnit()+")");

		return (LinearLayout)LayoutInflater.from(myContext.getContext()).inflate(R.layout.cell_output_field_selection_element,null);
	}

	@Override
	protected boolean shouldHideOutputView() {
		return false;
	}

	int i=0;
	public void addVariable(final String varId, boolean displayOut,String format,boolean isVisible,boolean showHistorical, String prefetchValue) {	
		Variable var = GlobalState.getInstance().getVariableCache().getCheckedVariable(myHash, varId, prefetchValue, prefetchValue!=null);
		super.addVariable(var, displayOut, format, isVisible,showHistorical);
	}

	@Override
	public Map<String, String> getKeyHash() {

		return myHash;
	}


	@Override
	public void onEvent(Event e) {
		if (e.getProvider().equals(Constants.SYNC_ID)) {
			super.refreshInputFields();
		}
	}


	@Override
	public String getName() {
		return null;
	}

	@Override
	public CellType getType() {
		return cellType;
	}
}
