package com.teraim.fieldapp.loadermodule.configurations;

import android.content.Context;

import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.IOException;

/**
 * Created by Terje on 2017-09-12.
 */

public abstract class CI_ConfigurationModule extends ConfigurationModule {

    protected CI_ConfigurationModule(Context context, PersistenceHelper gPh, PersistenceHelper ph, ConfigurationModule.Type type,
                                     ConfigurationModule.Source source, String urlOrPath, String fileName, String moduleName) {
        super(context,gPh,ph, type, source, urlOrPath, fileName, moduleName);
    }

    public abstract LoadResult prepare() throws IOException, Dependant_Configuration_Missing;
    public abstract LoadResult parse(String row, Integer currentRow) throws IOException;
    public abstract void finalizeMe() throws IOException;


}
