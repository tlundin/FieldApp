package com.teraim.fieldapp.loadermodule.configurations;

public class Dependant_Configuration_Missing extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String depM;
	
	public Dependant_Configuration_Missing(String dependantModule) {
		depM = dependantModule;
	}

	public String getDependendant() {
		return depM;
	}
	
}
