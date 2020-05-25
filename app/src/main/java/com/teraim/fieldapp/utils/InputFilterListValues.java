package com.teraim.fieldapp.utils;


import android.text.Spanned;
import android.util.Log;

import java.util.Set;

public class InputFilterListValues implements TextFilter {

	private String myPrint="";
	private final Set<String> allowed;
    public InputFilterListValues(Set<String> allowedValues) {
        this.allowed=allowedValues;
 		myPrint = "[";
		for (String s:allowed)
			myPrint+="["+s+"]";
		myPrint+="]";
	
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
      // Log.d("nils","SOURC: "+source.toString()+" DEST: "+dest.toString());
       String newVal = dest.toString().substring(0, dstart) + dest.toString().substring(dend, dest.toString().length());
       // Add the new string in
       newVal = newVal.substring(0, dstart) + source.toString() + newVal.substring(dstart, newVal.length());

       if (isLegal (newVal))
    	   return null;
       else {
    	   Log.d("nils","string "+newVal+" was not allowed by filter");
    	   return "";
       }

    }


	private boolean isLegal(String input) {
		if (input.length()==0) {
			Log.d("nils","allowing empty string");
			return true;
		}
		for (String value:allowed) {
			if (input.length()>value.length())
				continue;
			else if (value.substring(0, input.length()).equals(input)) {
				Log.d("nils","Found match for: "+input);
				return true;
			}			
		}
		Log.d("nils","found no match for input "+input);
		return false;
	}

	@Override
	public String prettyPrint() {
		return myPrint;
	}

    
}
