package com.teraim.fieldapp.dynamic.workflow_abstracts;


public interface EventListener {

	void onEvent(Event e);

	String getName();
}
