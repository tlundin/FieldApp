package com.teraim.fieldapp.dynamic.workflow_realizations.filters;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;

import java.util.Iterator;
import java.util.List;

//Specialized filter. Will remove elements with a value.
public class WF_OnlyWithoutValue_Filter extends WF_Filter {
	private static final String TAG = "WF_OnlyWithoutValue_Filter";

	
	
	
	public WF_OnlyWithoutValue_Filter(String id) {
		super(id);
		
	}

	@Override
	public void filter(List<? extends Listable> list) {
		Iterator<? extends Listable> it = list.iterator();
		while(it.hasNext()) {
			Listable l = it.next();
		if (l.hasValue())
			it.remove();
				//Log.d(TAG,"filter removes element "+l.getKey()+" because its value is null");
			
		}
    }

	@Override
	public boolean isRemovedByFilter(Listable l) {
		return l.hasValue();
	}


}