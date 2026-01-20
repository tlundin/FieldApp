package com.teraim.fieldapp.dynamic.blocks;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.utils.Expressor;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class StartCameraBlock extends Block implements EventListener {
    private static final String TAG = "StartCameraBlock";


    private final List<Expressor.EvalExpr> fileNameE;
    private final String rawName;
    private transient WF_Context myContext;

    public StartCameraBlock(String id, String fileName) {
        this.blockId = id;
        fileNameE = Expressor.preCompileExpression(fileName);
        Log.d(TAG,"precompile foto! "+fileNameE + "orig: "+fileName);
        rawName = fileName;
    }

    public void create(WF_Context myContext) {
        o = LogRepository.getInstance();
        String fileName = Expressor.analyze(fileNameE);
        File[] externalStorageVolumes =
                ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(),  null);
        File primaryExternalStorage = externalStorageVolumes[0];
        String PIC_ROOT_DIR = primaryExternalStorage+"pics/";
        Log.d(TAG,"foto evaluates to "+fileName);
        o.addText("StartCameraBlock fileName will be ["+PIC_ROOT_DIR +fileName+"]");
        if (fileName!=null) {
            File newfile = new File(PIC_ROOT_DIR + fileName);
            try {
                newfile.createNewFile();


                Uri outputFileUri = Uri.fromFile(newfile);

                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                this.myContext = myContext;
                // private String fileName;
                int TAKE_PHOTO_CODE = 0;
                myContext.getFragmentActivity().startActivityForResult(cameraIntent, TAKE_PHOTO_CODE);
            } catch (IOException e) {
                Log.e("vortex", "failed to create image file.");
            }
        } else {
            o.addText("");
            o.addCriticalText("FileName doesn't compute in startcamerablock "+blockId+" From xml target: "+this.rawName);
            Log.e("vortex", "fileName evaluated to null in startcamera");
        }
    }

    @Override
    public void onEvent(Event event) {
        if (event.getType() == Event.EventType.onActivityResult) {
            Log.d(TAG,"picture saved  ");
            myContext.registerEvent(new WF_Event_OnSave("photo"));
        }
    }

    @Override
    public String getName() {
        return "camerablock";
    }

}
