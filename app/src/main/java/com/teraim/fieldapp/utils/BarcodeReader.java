package com.teraim.fieldapp.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.non_generics.Constants;

/**
 * Created by Terje on 2016-10-12.
 */
public class BarcodeReader implements EventListener {

    private WF_Context myContext;
    private final BarcodeDetector detector;
    private Variable barCodeTarget;

    public BarcodeReader(WF_Context myContext, String targetVariableName) {
        detector =
                new BarcodeDetector.Builder(GlobalState.getInstance().getContext())
                        .build();
        this.myContext = myContext;
        barCodeTarget = GlobalState.getInstance().getVariableCache().getVariable(targetVariableName);
        barCodeTarget.setValue("");
        myContext.registerEvent(new WF_Event_OnSave("barcode"));
    }


    //Constructor when called from onActivityResult from outside the Vortex engine.
    public BarcodeReader(Context ctx) {

        detector =
                new BarcodeDetector.Builder(ctx)
                        .setBarcodeFormats(Barcode.QR_CODE)
                        .build();
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == Event.EventType.onActivityResult) {
            String code = analyze();
            if (code==null)
                code = "No barcode or error";
            if (barCodeTarget != null) {
                barCodeTarget.setValue(code);
            } else
                Log.e("vortex","cannot set result...barcode variable missing");
            myContext.registerEvent(new WF_Event_OnSave("barcode"));
        }
    }


    public String analyze() {
        if(!detector.isOperational()){
            return null;
        }
        Log.d("vortex", "barcode image is likely taken");
        try {

            BitmapFactory.Options option = new BitmapFactory.Options();
            //option.inJustDecodeBounds = false;
            option.inSampleSize = 4;
            Bitmap myBitmap = BitmapFactory.decodeFile(Constants.PIC_ROOT_DIR + Constants.TEMP_BARCODE_IMG_NAME,option);
            Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
            SparseArray<Barcode> barcodes = detector.detect(frame);
            if (barcodes!=null) {
                Log.d("vortex","barcodes size: "+barcodes.size());
                if (barcodes.size()>0) {
                    Log.d("vortex",barcodes.valueAt(0).displayValue);
                    return barcodes.valueAt(0).rawValue;

                } else
                    return null;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /*
            //turn image on disk to bitmap.
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            options.inJustDecodeBounds = false;



            Barcode thisCode = barcodes.valueAt(0);
            //Set the associated variable to the value given.
            if (barCodeTarget!=null)
                barCodeTarget.setValue(thisCode.rawValue);

        }
    }
    */

    @Override
    public String getName() {
        return "barcodereader";
    }
}
