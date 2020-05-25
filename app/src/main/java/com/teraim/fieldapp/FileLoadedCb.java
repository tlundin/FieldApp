package com.teraim.fieldapp;

import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;

public interface FileLoadedCb {

	void onFileLoaded(LoadResult res);

	void onFileLoaded(ErrorCode errCode, String version);

	void onUpdate(Integer... args);
	
}
