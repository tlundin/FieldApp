package com.teraim.fieldapp.dynamic.workflow_realizations;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;

import java.util.Collections;
import java.util.List;


public class WF_IndexOrder_Sorter implements Sorter {
	private static final String TAG = "WF_IndexOrder_Sorter";


	@Override
	public List<? extends Listable> sort(List<? extends Listable> list) {
//		Log.d(TAG,"Before TIME Sort: ");
//	for(Listable l:list)
//			Log.d(TAG,l.getLabel());
		Collections.sort(list, WF_ListEntry.Comparators.Index);
//		Log.d(TAG,"After TIME Sort: ");
//		for(Listable l:list)
//			Log.d(TAG,l.getLabel());
		return list;
	}

}
