package com.teraim.fieldapp.utils;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.Variable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FilterFactory {

	private static final int MAX_MIN = 999999999;

	private static FilterFactory singleton;
	
	private final Map<String, CombinedRangeAndListFilter> filterCache = new HashMap<String, CombinedRangeAndListFilter>();

    private final Vibrator myVibrator;
	
	public static class Range {
		final String minS;
        final String maxS;
		int min, max;
		Range(int min, int max) {
			this.min=min;
			this.max=max;
			minS = min+"";
			maxS = max+"";
		}
		Range(String min, String max) {
			this.maxS=max;
			this.minS=min;
			try {
			this.min = Integer.parseInt(minS);
			this.max = Integer.parseInt(maxS);
			} catch (NumberFormatException e) {
				Log.e("nils","NumberformatException with range "+min+","+max);
				this.min = 0;
				this.max = 0;
			}
		}
		
		
	}
	
	
	
	private FilterFactory(Context ctx) {
		myVibrator = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
	}
	
	
	public static FilterFactory getInstance(Context ctx) {
		
		if (singleton == null)
			singleton = new FilterFactory(ctx);
		return singleton;			
	}
	
	public CombinedRangeAndListFilter createLimitFilter(Variable myVar,String limitDesc) {
		limitDesc = limitDesc.replaceAll("\\s+","");
		CombinedRangeAndListFilter cached = filterCache.get(limitDesc);
		if (cached!=null)
			return cached;
		//I know the limitDesc is not null or empty.
		limitDesc = limitDesc.replace("\"", "");
		Set<String> allowedValues= new HashSet<String>();
		List<Range> allowedRanges= new ArrayList<Range>();
		//Log.d("nils","Creating input filter from: "+limitDesc);
		//Each element in the string is comma separated.
		String[] elems = limitDesc.split(",");
		for (String elem:elems) {
			String[] pair = elem.split("-");
			if (pair.length==1) 
				//This should be a number.
				allowedValues.add(pair[0]);
			else
				//this should be a range.
				allowedRanges.add(new Range(pair[0],pair[1]));
		}
		if (allowedValues.size()==0)
			allowedValues = null;
		if (allowedRanges.size()==0)
			allowedRanges = null;
		filterCache.put(limitDesc, cached);
        int currentMin = MAX_MIN;
		if (allowedRanges!=null) {
			for (Range r:allowedRanges) {
				if (r.min< currentMin)
					currentMin = r.min;
			}
			//Log.d("nils","CurrentMin is set to "+currentMin);
		} //else
		//	Log.d("nils","allowedranges was null for "+limitDesc);
		boolean hasDefaultFilter = false;
		if (currentMin >0 && currentMin <MAX_MIN) {
			allowedRanges.add(0,new Range(0, currentMin -1));
			hasDefaultFilter = true;
		}
		cached = new CombinedRangeAndListFilter(myVibrator,myVar,allowedValues,allowedRanges,hasDefaultFilter);
		return cached;
	}

}
