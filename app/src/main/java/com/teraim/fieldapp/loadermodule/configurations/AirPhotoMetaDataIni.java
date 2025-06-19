package com.teraim.fieldapp.loadermodule.configurations;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.PhotoMetaI;
import com.teraim.fieldapp.utils.PersistenceHelper;

//implements the PhotoMeta interface.

public class AirPhotoMetaDataIni extends CI_ConfigurationModule implements PhotoMetaI {

    public AirPhotoMetaDataIni(Context context, PersistenceHelper gPh, PersistenceHelper ph,
                                String urlOrPath, String fileName, String moduleName) {
        super(context,gPh,ph, Type.ini, urlOrPath, fileName, moduleName);
    }

    @Override
    public PhotoMeta getPhotoMeta() {
        Object pm = getEssence();
        if (!(pm instanceof PhotoMeta))
            return null;
        return (PhotoMeta)pm;
    }

    private enum Corners {
        LOWERLEFTCORNERX,
        UPPERRIGHTCORNERX,
        LOWERLEFTCORNERY,
        UPPERRIGHTCORNERY
    }


    private String s;
    private String w;
    private String e;
    private String n;



    @Override
    public LoadResult prepare() {
        //null means nothing to report and no error
        s=w=e=n=null;
        return null;
    }

    @Override
    public LoadResult parse(String row, Integer currentRow) {
        Log.d("franzon","Row"+currentRow+": "+row);
        String[] coord = row.split("=");
        if (coord==null || coord.length!=2)
            return new LoadResult(this, LoadResult.ErrorCode.ParseError);
        if(coord[0].equals(Corners.LOWERLEFTCORNERX.name())) {
            s=coord[1].trim();
        }
        else if(coord[0].equals(Corners.LOWERLEFTCORNERY.name())) {
            w=coord[1].trim();
        }
        else  if(coord[0].equals(Corners.UPPERRIGHTCORNERX.name())) {
            n=coord[1].trim();
        }
        else  if(coord[0].equals(Corners.UPPERRIGHTCORNERY.name())) {
            e=coord[1].trim();
        } else
            return new LoadResult(this, LoadResult.ErrorCode.ParseError);

        return null;
    }

    @Override
    public void finalizeMe() {
        if (w!=null&&e!=null&&s!=null&&n!=null) {
            Log.d("franzon","photometa is parsed");
            setEssence(new PhotoMeta(n ,e, s, w));
        }
        else {
            Log.e("vortex","Photometa file is corrupt");
            new LoadResult(this, LoadResult.ErrorCode.ParseError, "Photometa file is corrupt");
        }
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
    public void setEssence() {

    }

}
