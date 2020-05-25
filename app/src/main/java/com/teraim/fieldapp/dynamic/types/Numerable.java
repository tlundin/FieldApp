package com.teraim.fieldapp.dynamic.types;

public interface Numerable {

	enum Type {
		LITERAL,ARITMETIC,NUMERIC,FLOAT,BOOLEAN
	}
	
	String toString();
	 
	Type getType();

	String getName();
	
	String getLabel();
	
	void setValue(String value);

	
	

	
}
