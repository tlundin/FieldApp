package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;

class WF_Event_OnRedraw extends Event {
	private static final String TAG = "WF_Event_OnRedraw";


	public WF_Event_OnRedraw(String id) {
		super(id,EventType.onRedraw);
		Log.d(TAG,"CREATED ONREDRAW FOR ID: "+id);
	}
	
	
}
