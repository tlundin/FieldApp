package com.teraim.fieldapp.loadermodule.configurations;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GISListConfiguration extends CI_ConfigurationModule {
    public static final String NAME = "content";
    private List<String> gisTypes = new ArrayList<>();

    public GISListConfiguration(Context context, PersistenceHelper gPh, PersistenceHelper ph, String urlOrPath) {
        super(context,gPh,ph, Type.txt, urlOrPath, "content", "Gis content                ");
    }

    @Override
    public LoadResult prepare() throws IOException, Dependant_Configuration_Missing {
        Log.d("GISListConfiguration","prepare()");
        return null;
    }

    @Override
    public LoadResult parse(String row, Integer currentRow) throws IOException {
        Log.d("GISListConfiguration","In parse with row: "+row);
        if (row.length()>0) {
            gisTypes.add(row.trim());
        }
        return null;
    }

    @Override
    public void finalizeMe() throws IOException {

    }

    @Override
    public float getFrozenVersion() {
        return 0;
    }

    @Override
    protected void setFrozenVersion(float version) {

    }

    @Override
    public boolean isRequired() {
        return false;
    }

    @Override
    protected void setEssence() {
        essence = gisTypes;
    }

    public List<String> getGisTypes() {
        return (essence!=null)?(List<String>)essence:null;
    }
}
