package com.teraim.fieldapp.dynamic.workflow_realizations;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;

import java.util.Collections;
import java.util.List;



public class WF_Alphanumeric_Sorter implements Sorter {
	private static final String TAG = "WF_Alphanumeric_Sorter";


	@Override
	public List<? extends Listable> sort(List<? extends Listable> list) {
		//Log.d(TAG,"Before ALPHA Sort: ");
	//	for(Listable l:list)
	//		Log.d(TAG,l.getLabel()+",");
		Collections.sort(list, WF_ListEntry.Comparators.Alphabetic);
		//Log.d(TAG,"After ALPHA Sort: ");
		//for(Listable l:list)
		//	Log.d(TAG,l.toString());
		return list;
	}

	
}
