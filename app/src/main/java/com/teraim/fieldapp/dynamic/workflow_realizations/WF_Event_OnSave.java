package com.teraim.fieldapp.dynamic.workflow_realizations;

import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;

import java.util.Map;

public class WF_Event_OnSave extends Event {

	Map<Variable, String> varsAffected=null;
	private Listable myListable;

	public WF_Event_OnSave(String id) {
		super(id,EventType.onSave);
	}
	
	public WF_Event_OnSave(String id, Map<Variable, String> changes, Listable l) {
		super(id,EventType.onSave);
		varsAffected=changes;
		myListable = l;
	}

	public Listable getListable() {
		return myListable;
	}
	
	
}
