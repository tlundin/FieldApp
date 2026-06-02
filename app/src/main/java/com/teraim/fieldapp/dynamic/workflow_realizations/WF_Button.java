package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import com.teraim.fieldapp.R;

/**
 * Created by Terje on 2016-08-23.
 */
public class WF_Button extends WF_Widget {

    Button myButton = null;
    final Context ctx;

    public WF_Button(String id, Button button, boolean isVisible, WF_Context myContext) {
        super(id, button, isVisible, myContext);
        myButton = button;
        ctx = myContext.getContext();
    }

    public void setText(String text) {
        if (myButton != null)
            myButton.setText(text);
        else
            Log.e("vortex", "BUTTON NULL IN SETTEXT: " + text);
    }

    public void setOnClickListener(View.OnClickListener l) {
        myButton.setOnClickListener(l);
    }

    /**
     * Creates a Material-styled action button (same look as status buttons, no status icon).
     */
    public static Button createInstance(String text, Context ctx) {
        LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        Button button = (Button) inflater.inflate(R.layout.button_action, null);
        button.setText(text);
        return button;
    }
}
