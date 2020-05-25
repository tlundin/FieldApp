package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;

import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;

public class WF_ClickableField_Selection_OnSave extends WF_ClickableField_Selection implements EventListener {

	
	
	public WF_ClickableField_Selection_OnSave(String headerT, String descriptionT,
											  WF_Context context, String id, boolean isVisible, boolean autoOpenSpinner, DisplayFieldBlock format) {
		super(headerT,descriptionT, context, id,isVisible,format);
		context.registerEventListener(this, EventType.onSave);
		setAutoOpenSpinner(autoOpenSpinner);
	}

	@Override
	public void onEvent(Event e) {
		if (myContext.myEndIsNear()) {
			Log.d("vortex","Disregarding onsave due to ongoing reexecute.");
			return;
		}

		if (!getId().equals(e.getProvider())) {
			Log.d("nils","In onEvent for WF_ClickableField_Selection_OnSave. Provider: "+e.getProvider());
			if (iAmOpen)
				refreshInputFields();
			refresh();
		} else
			Log.d("nils","Discarded...from me");
	}

	@Override
	public String getName() {
		return "CLICKABLE "+this.getId();
	}


}
