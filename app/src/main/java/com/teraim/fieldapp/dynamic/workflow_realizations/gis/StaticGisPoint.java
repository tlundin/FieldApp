package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import com.teraim.fieldapp.dynamic.types.Location;

import java.util.Collections;
import java.util.Map;

public class StaticGisPoint extends GisPointObject {
	
	public StaticGisPoint(FullGisObjectConfiguration conf,Map<String, String> keyChain,Location myLocation, String statusVar, String statusVal) {
		super(conf,keyChain, Collections.singletonList(myLocation),statusVar,statusVal);
	}



	/*
	@Override
	public String toString() {
		String res="";
		res+=" \nDynamic: no";
		res+="\nLabel: "+this.getLabel();
		return res;
	}
*/


	@Override
	public boolean isDynamic() {
		return false;
	}



	@Override
	public boolean isUser() {
		return false;
	}

}
