package com.teraim.fieldapp.expr;

import com.teraim.fieldapp.dynamic.types.Numerable;

public class Literal implements Numerable {

	private final String name;
    private final String label;
    private String value;
	
	public Literal(String name,String label) {
		value=null;
		this.name=name;
		this.label=label;
	}

	

	public void setValue(String val) {
		value = val;
	}

	@Override
	public Type getType() {
		return Type.LITERAL;
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getLabel() {
		return label;
	}
	

}
