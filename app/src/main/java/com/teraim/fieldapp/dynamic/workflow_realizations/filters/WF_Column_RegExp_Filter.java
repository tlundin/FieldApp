package com.teraim.fieldapp.dynamic.workflow_realizations.filters;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;

import java.util.Iterator;
import java.util.List;

//Specialized filter. Will filter a list on entries in column matching Regexp

public class WF_Column_RegExp_Filter extends WF_Filter {

	private String regularExpression = "";
	String filterColumn;
	private final String columnToMatch;



	public WF_Column_RegExp_Filter(String id,String columnToMatch,String regExp) {
		super(id);
		regularExpression = regExp;
		this.columnToMatch=columnToMatch;
		
	}

	@Override
	public void filter(List<? extends Listable> list) {
		String key;
		Iterator<? extends Listable> it = list.iterator();	
		boolean noMatchAtAll = true;
		
		while(it.hasNext()) {
			Listable l = it.next();
			key = l.getSortableField(columnToMatch);
			if (key==null) {
				Log.e("nils","Key was null in filter");
				continue;
			}				
			

			if (!key.matches(regularExpression)) {
				it.remove();
				//Log.d("nils","filter removes element "+key+" because it doesn't match "+regularExpression);
			} else
				noMatchAtAll=false;
			

		}
		if (noMatchAtAll) {
			o = GlobalState.getInstance().getLogger();
			o.addRow("");
			o.addYellowText("No matches found in Regexp filter. Column used: ["+columnToMatch+"]");
		}

    }

	@Override
	public boolean isRemovedByFilter(Listable l) {
		String key = l.getSortableField(columnToMatch);
		if (key!=null)
			return (!key.matches(regularExpression));
		else {
			Log.e("vortex","null in filter");
			return true;
		}
	}


}


