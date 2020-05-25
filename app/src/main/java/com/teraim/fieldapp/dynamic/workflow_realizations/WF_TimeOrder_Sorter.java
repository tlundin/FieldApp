package com.teraim.fieldapp.dynamic.workflow_realizations;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Sorter;

import java.util.Collections;
import java.util.List;


public class WF_TimeOrder_Sorter implements Sorter {

	@Override
	public List<? extends Listable> sort(List<? extends Listable> list) {
//		Log.d("nils","Before TIME Sort: ");
//	for(Listable l:list)
//			Log.d("nils",l.getLabel());
		Collections.sort(list, WF_ListEntry.Comparators.Time);
//		Log.d("nils","After TIME Sort: ");
//		for(Listable l:list)
//			Log.d("nils",l.getLabel());
		return list;
	}

}
