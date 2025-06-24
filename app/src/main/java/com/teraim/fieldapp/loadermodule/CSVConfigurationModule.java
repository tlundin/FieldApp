package com.teraim.fieldapp.loadermodule;

import android.content.Context;

import com.teraim.fieldapp.loadermodule.configurations.CI_ConfigurationModule;
import com.teraim.fieldapp.utils.PersistenceHelper;

public abstract class CSVConfigurationModule extends CI_ConfigurationModule {

	protected CSVConfigurationModule(Context context, PersistenceHelper gPh, PersistenceHelper ph,
									 String urlOrPath, String fileName, String moduleName) {
		super(context, gPh,ph, FileFormat.csv, urlOrPath, fileName, moduleName);
	}
	@Override
	public void finalizeMe() {
    }

}
