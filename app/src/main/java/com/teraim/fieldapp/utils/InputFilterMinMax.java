package com.teraim.fieldapp.utils;

import android.text.Spanned;
import android.util.Log;

public class InputFilterMinMax implements TextFilter {

	private final int min;
    private final int max;
	private String myPrint = "";
	
	public InputFilterMinMax(int min, int max) {
		this.min = min;
		this.max = max;
		myPrint = "{"+min+".."+max+"}";
	}

	public InputFilterMinMax(String min, String max) {
		this.min = Integer.parseInt(min);
		this.max = Integer.parseInt(max);
		myPrint = "{"+min+".."+max+"}";
	}

	@Override
	public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

		// Remove the string out of destination that is to be replaced
		String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
		// Add the new string in
		newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());
		if (isLegal(newVal))
			return null;
		return "";
	}

	private boolean isInRange(int a, int b, int c) {
		//Log.d("nils","Testing range for a: "+a+" b: "+b+" c:"+c);
		return b > a ? c >= a && c <= b : c >= b && c <= a;
	}


	private boolean isLegal(String newVal) {
		//Log.d("nils","String to test: "+newVal);
		if (newVal.length()==0)
			return false;
		try {
			int input = Integer.parseInt(newVal.trim());
			return (isInRange(min, max, input));
		} catch (NumberFormatException nfe) {Log.d("nils","this is no number"); }
		return false;

	}
	
	@Override
	public String prettyPrint() {
		return myPrint;
	}
	
}