package com.teraim.fieldapp.dynamic.workflow_abstracts;

import java.util.Map;
import java.util.Set;

import com.teraim.fieldapp.dynamic.types.Variable;

//Listable represents a row of data with columns. 
//TODO: Weaknesses : Cannot sort on value, only columns in Configuration Time data.
public interface Listable {
	String getSortableField(String columnId);
	//Return the keychain for this listables key.
    Map<String,String> getKeyChain();
	String getKey();
	//TODO: Must separate into Comparable class or similar?
    long getTimeStamp();
	boolean hasValue();
	String getLabel();
	void refresh();

	Set<Variable> getAssociatedVariables();


}