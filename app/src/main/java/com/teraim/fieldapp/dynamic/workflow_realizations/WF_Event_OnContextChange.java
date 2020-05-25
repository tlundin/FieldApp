package com.teraim.fieldapp.dynamic.workflow_realizations;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;

class WF_Event_OnContextChange extends Event {

	public WF_Event_OnContextChange(String fromId) {
		super(fromId, EventType.onContextChange);
		
	}

}
