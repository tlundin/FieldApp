package com.teraim.fieldapp.loadermodule;

import java.util.List;


public class Configuration {
	
	private final List<ConfigurationModule> mModules;

	
	public Configuration(List<ConfigurationModule> modules) {
		mModules=modules;
	}

		

	public ConfigurationModule next() {

		for (ConfigurationModule cm:mModules) {
			if (!cm.isLoaded() && !cm.isThawing())
				return cm;
		}
		return null;
	}
	
	public ConfigurationModule getModule(String moduleName) {
		if(moduleName==null)
			return null;
		for(ConfigurationModule m:mModules) 
			if (m.getFileName().equals(moduleName))
				return m;
		return null;
	}
	public List<ConfigurationModule> getAll() {
		return mModules;
	}
	
	
}
