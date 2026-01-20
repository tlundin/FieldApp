package com.teraim.fieldapp.dynamic.types;

import android.util.Log;

import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;

/**
 * Created by Terje on 2016-05-11.
 */
public class MapGisLayer extends GisLayer {
    private static final String TAG = "MapGisLayer";

    private final String myImg;
    public MapGisLayer( String label, String myImgName) {
        super(label+myImgName, label, false, false,true, false);
        myImg = myImgName;
        Log.d(TAG,"Created map layer for id: "+getId());
    }

public Object getImageName() {
        return myImg;
    }

}
