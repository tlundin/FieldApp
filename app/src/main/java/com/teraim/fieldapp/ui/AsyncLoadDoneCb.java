package com.teraim.fieldapp.ui;

import java.util.List;

import com.teraim.fieldapp.loadermodule.ConfigurationModule;

public interface AsyncLoadDoneCb {
	void onLoadSuccesful(List<ConfigurationModule> modules);
	
}
