package com.teraim.fieldapp.dynamic;

public interface AsyncResumeExecutorI {

	
	void continueExecution(String fromWhere);
	void abortExecution(String string);
}
