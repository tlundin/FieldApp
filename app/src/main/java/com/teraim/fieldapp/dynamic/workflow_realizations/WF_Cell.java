package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.view.View;

import com.teraim.fieldapp.dynamic.types.Variable;

import java.util.Map;
import java.util.Set;

public interface WF_Cell {

	enum CellType { Aggregate, Normal };
	void addVariable(final String varId, boolean displayOut, String format, boolean isVisible, boolean showHistorical, String prefetchValue);
	
	
	boolean hasValue();
	
	void refresh();

	View getWidget();

	CellType getType();

	Map<String,String> getKeyHash();

	Set<Variable> getAssociatedVariables();
}
