package com.teraim.fieldapp.dynamic.workflow_abstracts;

import java.util.List;

import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;

public interface Container {	
	Container getParent();
	void
	add(WF_Widget d);
	void remove(WF_Widget d);
	Container getRoot();
	List<WF_Widget> getWidgets();
	void draw();
	void removeAll();

}