package com.teraim.fieldapp.dynamic.workflow_realizations;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;

public class WF_Event_OnFlowExecuted extends Event {

	public WF_Event_OnFlowExecuted(String fromId) {
		super(fromId, EventType.onFlowExecuted);
		
	}

}
