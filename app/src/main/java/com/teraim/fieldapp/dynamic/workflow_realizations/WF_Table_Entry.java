package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.view.View;

import com.teraim.fieldapp.dynamic.types.Variable;

import java.util.HashSet;
import java.util.Set;

public class WF_Table_Entry extends WF_ListEntry {
    private Variable myVar;

	public WF_Table_Entry(String id, View v, WF_Context ctx, boolean isVisible) {
		super(id, v, ctx, isVisible);
    }

	@Override
	public Set<Variable> getAssociatedVariables() {
		final Set<Variable> s = new HashSet<Variable>();
		s.add(myVar);
		return s;
	}

	@Override
	public void refresh() {
		// TODO Auto-generated method stub

	}

}
