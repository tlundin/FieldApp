package com.teraim.fieldapp.loadermodule.configurations;

import android.graphics.BitmapFactory;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.loadermodule.ConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.PhotoMetaI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.PersistenceHelper;

/**
 * Created by terje on 3/18/2018.
 */

public class AirPhotoMetaDataJgw extends CI_ConfigurationModule implements PhotoMetaI {

    private final String[] pars = new String[6];
    private final String imgUrlorPath;
    private double Width,Height;
    public AirPhotoMetaDataJgw(PersistenceHelper gPh, PersistenceHelper ph,
                               ConfigurationModule.Source source, String urlOrPath, String fileName, String moduleName) {
        super(gPh, ph, Type.jgw, source, urlOrPath, fileName, moduleName);
        Log.d("jgw","setting simple version to false");
        Log.d("jgw","urlorpath: "+urlOrPath);
        Log.d("jgw","fileName: "+fileName);


        imgUrlorPath = fileName+".jpg";

        hasSimpleVersion=false;
    }
    @Override
    public PhotoMeta getPhotoMeta() {
        Object pm = getEssence();
        if (!(pm instanceof PhotoMeta))
            return null;
        return (PhotoMeta)pm;
    }
    @Override
    public LoadResult prepare()  {
        //null means nothing to report and no error
        Log.d("jgw","in prepare");

        //need to check the size of the real image.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        String pathName = Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/cache/"+imgUrlorPath;
        BitmapFactory.decodeFile(pathName, options);
        Log.d("jgw","imgUrlorPath: "+imgUrlorPath);
        Log.d("jgw","cached image path: "+pathName);
        //File directory = new File(Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/cache/");
        //File[] files = directory.listFiles();
        //Log.d("jgw", "Size: "+ files.length);
        //for (int i = 0; i < files.length; i++)
        //{
        //    Log.d("jgw", "FileName:" + files[i].getName());
        //}
        Width = options.outWidth;
        Height = options.outHeight;
        Log.d("jgw","WIDTH HEIGHT "+Width+","+Height);
        if (Width==0 && Height==0)
            return new LoadResult(this, LoadResult.ErrorCode.ParseError,"Could not calculate image width and height for "+imgUrlorPath);
        return null;
    }

    @Override
    public LoadResult parse(String row, Integer currentRow)  {
        Log.d("jgw","Row"+currentRow+": "+row);
        if (currentRow<=pars.length) {
            pars[currentRow-1]=row;
            return null;
        } else  //overflow.
            return new LoadResult(this, LoadResult.ErrorCode.ParseError);


    }

    @Override
    public void finalizeMe()  {



        try {
            //pars[n] now contains row n in jgq file.
            double XCellSize = Double.parseDouble(pars[0]);
            //dont care about rotation in row 1 and row 2.
            double YCellSize = Double.parseDouble(pars[3]);
            double WorldX = Double.parseDouble(pars[4]);
            double WorldY = Double.parseDouble(pars[5]);

            double W = WorldX - (XCellSize / 2);
            double N = WorldY - (YCellSize / 2);
            double E = (WorldX + (Width * XCellSize)) - (XCellSize / 2);
            double S = (WorldY + (Height * YCellSize)) - (YCellSize / 2);

            setEssence(new PhotoMeta(N, E, S, W));
            Log.d("jgw","N: E: S: W: "+N+","+E+","+","+S+","+W);
        }
        catch(NumberFormatException ex) {
            Log.e("jgw","Photometa file is corrupt");
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