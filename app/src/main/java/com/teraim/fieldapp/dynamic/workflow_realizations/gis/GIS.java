package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;

import com.teraim.fieldapp.Start;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Event_OnSave;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Widget;
import com.teraim.fieldapp.gis.GisImageView;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;

import java.util.List;
import java.util.Map;

import static com.teraim.fieldapp.GlobalState.getInstance;

public class GIS extends WF_Widget {

    private final WF_Context myContext;
    protected List<GisLayer> myLayers;
    private boolean isImageGis = false;
    private GisViewImplementation myGisView;

    public GIS(String id, View v, boolean isVisible, WF_Context myContext, GisViewImplementation myGisView) {
        super(id, v, isVisible, myContext);
        this.myGisView= myGisView;
        this.myContext = myContext;
    }

    public boolean isZoomLevel() {
        return false;
    }

    public boolean wasShowingPopup() {
        return false;
    }

    public List<GisLayer> getLayers() {
        return myLayers;
    }

    public GisViewImplementation getGis() {
        return myGisView;
    };

    public void addLayer(GisLayer layer) {
        if(layer!=null) {
            Log.d("vortex","Successfully added layer "+layer.getLabel());
            myLayers.add(layer);
        }
    }

    public GisLayer getLayerFromLabel(String label) {
        if (myLayers==null||myLayers.isEmpty()||label==null)
            return null;
        for (GisLayer gl:myLayers) {
            if (gl.getLabel().equals(label)) {
                Log.d("vortex","MATCH Label!!");
                return gl;
            }

        }
        Log.e("vortex","NO MATCH Label!!");
        return null;

    }

    public GisLayer getLayerFromId(String identifier) {
        if (myLayers==null||myLayers.isEmpty()||identifier==null)
            return null;
        for (GisLayer gl:myLayers) {
            //Log.d("vortex","ID for layer: "+gl.getId());
            if (gl.getId().equals(identifier)) {
                //	Log.d("vortex","MATCH GL!!");
                return gl;
            }
        }
        Log.d("vortex", "Did not find layer " + identifier + " from GisMap.");

        return null;
    }

    public void addGisObjectType(FullGisObjectConfiguration gop,String paletteName) {

    }


    public void clearLayerCaches() {
        for (GisLayer gl:myLayers)
            gl.clearCaches();

    }

    public void runSelectedWf(GisObject gop) {
        getInstance().setDBContext(new DB_Context(null,gop.getKeyHash()));
        Log.d("vortex","Setting current keyhash to "+gop.getKeyHash());
        String target = gop.getWorkflow();
        Workflow wf = getInstance().getWorkflow(target);
        if (wf ==null) {
            Log.e("vortex","missing click target workflow");
            new AlertDialog.Builder(myContext.getContext())
                    .setTitle("Missing workflow")
                    .setMessage("No workflow associated with the GIS object or workflow not found: ["+target+"]. Check your XML.")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setCancelable(false)
                    .setNeutralButton("Ok",new Dialog.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    } )
                    .show();
        } else {
            if (gop.getStatusVariableId()!=null) {
                Map<String, String> keyHash = gop.getKeyHash();
                if (keyHash!=null)
                    keyHash.put(VariableConfiguration.KEY_YEAR, Constants.getYear());
                Log.d("buu","wfclick keyhash is "+keyHash+" for "+gop.getLabel());
                Variable statusVariable = getInstance().getVariableCache().getVariable(keyHash,gop.getStatusVariableId());
                if (statusVariable!=null) {
                    String valS = statusVariable.getValue();
                    if (valS == null || valS.equals("0")) {
                        Log.d("grogg", "Setting status variable to 1");
                        statusVariable.setValue("1");
                        gop.setStatusVariable(statusVariable);
                    } else
                        Log.d("grogg", "NOT Setting status variable to 1...current val: " + statusVariable.getValue());
                    myContext.registerEvent(new WF_Event_OnSave("Gis"));
                } else {
                    Log.e("grogg", "StatusVariable definition error");
                    LoggerI o = getInstance().getLogger();
                    o.addRow("");
                    o.addRedText("StatusVariable definition missing for: "+gop.getStatusVariableId());
                }

            } else
                Log.e("grogg",gop.getStatusVariableId()+" is null");

            Start.singleton.changePage(wf,gop.getStatusVariableId());

        }
    }
}
