package com.teraim.fieldapp.dynamic.workflow_realizations;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;

import java.util.Collections;
import java.util.List;



public class WF_Alphanumeric_Sorter implements Sorter {

	@Override
	public List<? extends Listable> sort(List<? extends Listable> list) {
		//Log.d("nils","Before ALPHA Sort: ");
	//	for(Listable l:list)
	//		Log.d("nils",l.getLabel()+",");
		Collections.sort(list, WF_ListEntry.Comparators.Alphabetic);
		//Log.d("nils","After ALPHA Sort: ");
		//for(Listable l:list)
		//	Log.d("nils",l.toString());
		return list;
	}

	
}
