package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.view.ViewGroup;

import com.teraim.fieldapp.log.LoggerI;


public abstract class WF_Thing {

	private final String myId;
	private ViewGroup myWidget;
	protected LoggerI o;

	protected WF_Thing(String id) {
		myId = id;
	}
	
	public String getId() {
		return myId;
	}



}