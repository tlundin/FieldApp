package com.teraim.fieldapp.loadermodule;


public class LoadResult {
	
	public enum ErrorCode {
		ok,
		loaded,
		HostNotFound,
		notFound,
		ParseError,
		IOError,
		configurationError, 
		Aborted,
		BadURL,
		frozen,
		parsed,
		noData,
		thawed,
		Unsupported,
		socket_timeout,
		reloadDependant, thawFailed, majorVersionNotUpdated,majorVersionUpdated
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
