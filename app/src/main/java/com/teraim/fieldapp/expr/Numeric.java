package com.teraim.fieldapp.expr;

public class Numeric extends Aritmetic {

	public Numeric(String name, String label) {
		super(name,label);
	}

	@Override
	public Type getType() {
		return Type.NUMERIC;
	}
	
}
