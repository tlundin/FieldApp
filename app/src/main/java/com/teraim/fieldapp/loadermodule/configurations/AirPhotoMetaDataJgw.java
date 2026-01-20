package com.teraim.fieldapp.loadermodule.configurations;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.PhotoMeta;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.PhotoMetaI;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.lang.reflect.Type;
import java.util.Locale;

/**
 * Created by terje on 3/18/2018.
 */

public class AirPhotoMetaDataJgw extends CI_ConfigurationModule implements PhotoMetaI {
    private static final String TAG = "AirPhotoMetaDataJgw";


    private final String[] pars = new String[6];
    private final String imgUrlorPath;
    private double Width,Height;
    public AirPhotoMetaDataJgw(Context context, PersistenceHelper gPh, PersistenceHelper ph,
                                String urlOrPath, String fileName, String moduleName) {
        super(context,gPh, ph, FileFormat.jgw, urlOrPath, fileName, moduleName);
        Log.d(TAG,"setting simple version to false");
        Log.d(TAG,"urlorpath: "+urlOrPath);
        Log.d(TAG,"fileName: "+fileName);


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
        Log.d(TAG,"in prepare");

        //need to check the size of the real image.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        String pathName = GlobalState.getInstance().getContext().getFilesDir()+"/"+globalPh.get(PersistenceHelper.BUNDLE_NAME).toLowerCase(Locale.ROOT)+"/cache/"+imgUrlorPath;
        BitmapFactory.decodeFile(pathName, options);
        Log.d(TAG,"imgUrlorPath: "+imgUrlorPath);
        Log.d(TAG,"cached image path: "+pathName);
        //File directory = new File(Constants.VORTEX_ROOT_DIR+globalPh.get(PersistenceHelper.BUNDLE_NAME)+"/cache/");
        //File[] files = directory.listFiles();
        //Log.d(TAG, "Size: "+ files.length);
        //for (int i = 0; i < files.length; i++)
        //{
        //    Log.d(TAG, "FileName:" + files[i].getName());
        //}
        Width = options.outWidth;
        Height = options.outHeight;
        Log.d(TAG,"WIDTH HEIGHT "+Width+","+Height);
        if (Width==0 && Height==0)
            return new LoadResult(this, LoadResult.ErrorCode.ParseError,"Could not calculate image width and height for "+imgUrlorPath);
        return null;
    }

    @Override
    public LoadResult parse(String row, Integer currentRow)  {
        Log.d(TAG,"Row"+currentRow+": "+row);
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
            Log.d(TAG,"N: E: S: W: "+N+","+E+","+","+S+","+W);
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
    protected Type getEssenceType() {
        return PhotoMeta.class;
    }

    @Override
    protected void setFrozenVersion(float version) {

    }



    public boolean isRequired() {
        return false;
    }

    @Override
    public void setEssence() {

    }

}