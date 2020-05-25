package com.teraim.fieldapp.loadermodule;


public class LoadResult {
	
	public enum ErrorCode {
		ok,
		newVarPatternVersionLoaded,
		loaded,
		bothFilesLoaded,
		HostNotFound,
		notFound,
		ParseError,
		IOError,
		sameold, 
		whatever, 
		configurationError, 
		Aborted, 
		LoadInBackground, newConfigVersionLoaded, 
		BadURL,frozen, parsed, noData, thawed, 
		ClassNotFound, Unsupported, socket_timeout,
		reloadDependant, existingVersionIsMoreCurrent, slowConnection, thawFailed, tick, majorVersionNotUpdated,majorVersionUpdated
	}
	
	
	public final ErrorCode errCode;
	public ConfigurationModule module;
	public String errorMessage;


	public LoadResult(ConfigurationModule module,ErrorCode errC,String errM) {
		errCode = errC;
		this.module=module;
		errorMessage=errM;
	}

	public LoadResult(ConfigurationModule module, ErrorCode errC) {
		errCode = errC;
		this.module = module;
		errorMessage="";
	}

}
