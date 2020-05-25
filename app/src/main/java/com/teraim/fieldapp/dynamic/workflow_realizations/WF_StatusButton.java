package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Button;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.utils.Expressor;

import java.util.List;

/**
 * Created by Terje on 2016-08-24.
 */



public class WF_StatusButton extends WF_Button {


    private final String statusVariableName;
    private final WF_Context myContext;
    private final List<Expressor.EvalExpr> hash;
    private Variable statusVariable=null;
    private Status myStatus=Status.none;

    public static Button createInstance(int statusOrdinal, String text, Context ctx) {


        //Enforce a state
        Status s = getStatusFromOrdinal(statusOrdinal);

        int layout = WF_StatusButton.getLayoutIdFromStatus(s);
        final LayoutInflater inflater = (LayoutInflater)ctx.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);

        Button button = (Button)inflater.inflate(layout,null);
        button.setText(text);

        return button;
    }

    private static Status getStatusFromOrdinal(int statusOrdinal) {
        Status s = Status.none;
        switch (statusOrdinal) {
            case 0:
                s=Status.none;
                break;
            case 1:
                s=Status.started;
                break;
            case 2:
                s=Status.started_with_errors;
                break;
            case 3:
                s=Status.ready;
                break;

        }
        return s;
    }

    public boolean refreshStatus() {
        DB_Context statusContext=null;
        if (hash==null) {
            Log.d("vortex","hash null in statusrefresh...will try currenthash");
            statusContext=myContext.getHash();
        } else
            statusContext = DB_Context.evaluate(hash);
        if (statusContext!=null)
            setStatusVariable(statusContext);

       if (statusVariable!=null) {
           int statusI = 0;

           try {
               String v = statusVariable.getValue();
               if (v == null) {
                   statusVariable.setValueNoSync("0");
               }
               statusI = (v == null) ? 0 : Integer.parseInt(v);
               Log.d("gomorra", "statusvariable " + statusVariable.getId() + " has value " + statusI + " with hash " + statusVariable.getKeyChain());
           } catch (NumberFormatException e) {
               Log.e("vortex", "Parseerror in refresh status. This is not an integer: " + statusVariable.getValue());
           }
           myStatus = getStatusFromOrdinal(statusI);
           refreshButton(WF_StatusButton.getIdFromStatus(myStatus));
       }
       return (statusVariable!=null);

    }


    private void setStatusVariable(DB_Context statusContext) {
        statusVariable = GlobalState.getInstance().getVariableCache().getVariable(statusContext.getContext(),statusVariableName);
    }

    public Status getStatus() {
        return myStatus;
    }

    public enum Status {
        none,
        started,
        started_with_errors,
        ready
    }

    

    public WF_StatusButton(String text, Button button, boolean isVisible, WF_Context myContext, String statusVariableS, List<Expressor.EvalExpr> rawStatusHash) {
        super(text, button, isVisible, myContext);
        this.statusVariableName = statusVariableS;
        this.hash = rawStatusHash;
        this.myContext = myContext;

    }

    public void changeStatus(Status status) {
        if (statusVariable!=null) {
            Log.d("vortex", "Button status changes to " + status);
            //Variable statusVariable = GlobalState.getInstance().getVariableCache().getVariable(statusVariableHash,statusVariableName);
            statusVariable.setValue(status.ordinal() + "");
            int id = getIdFromStatus(status);
            refreshButton(id);
        } else
            Log.e("vortex","no status variable in changestatus!");
    }

    private void refreshButton(int id) {

        Drawable image = ContextCompat.getDrawable(ctx, id);
        int h = image.getIntrinsicHeight();
        int w = image.getIntrinsicWidth();
        image.setBounds( 0, 0, w, h );
        myButton.setCompoundDrawables(image,null,null,null);
    }

    private static int getIdFromStatus(Status status) {
        int id=R.drawable.btn_icon_none;
        switch (status) {
            case none:
                id = R.drawable.btn_icon_none;
                break;
            case started:
                id = R.drawable.btn_icon_started;
                break;
            case started_with_errors:
                id = R.drawable.btn_icon_started_with_errors;
                break;
            case ready:
                id = R.drawable.btn_icon_ready;
                break ;

        }
        return id;
    }
    private static int getLayoutIdFromStatus(Status status) {
        int id=R.layout.button_status_none;
        switch (status) {
            case none:
                id = R.layout.button_status_none;
                break;
            case started:
                id = R.layout.button_status_started;
                break;
            case started_with_errors:
                id = R.layout.button_status_started_with_errors;
                break;
            case ready:
                id = R.layout.button_status_ready;
                break ;
        }
        return id;
    }
}
