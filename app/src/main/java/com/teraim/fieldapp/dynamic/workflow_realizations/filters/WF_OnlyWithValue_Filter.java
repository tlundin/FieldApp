package com.teraim.fieldapp.dynamic.workflow_realizations.filters;

import android.util.Log;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Filter;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;

import java.util.Iterator;
import java.util.List;

public class WF_OnlyWithValue_Filter extends WF_Filter implements Filter {

	public WF_OnlyWithValue_Filter(String id) {
		super(id);
		
	}

	@Override
	public void filter(List<? extends Listable> list) {
		Log.d("vortex","In only_with_value filter with "+list.size()+" elements");
		Iterator<? extends Listable> it = list.iterator();
		while(it.hasNext()) {
			Listable l = it.next();
			if(!l.hasValue()) {
				it.remove();
				//Log.d("vortex", "filter removes element " + l.getKey() + " because its value is null");
			} //else
			//	Log.d("vortex", "Element " + l.getKey() + " has value ");
		}
		Log.d("nils","Exit only_with_value filter with "+list.size()+" elements");
    }

	@Override
	public boolean isRemovedByFilter(Listable l) {
		return !l.hasValue();
	}


}

