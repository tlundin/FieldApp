package com.teraim.fieldapp.log;

import android.content.Context;
import android.content.Intent;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.TextView;

import com.teraim.fieldapp.ui.MenuActivity;

public class PassiveLogger implements LoggerI {

    private final StringBuilder log = new StringBuilder();
    private final Context myContext;
    int ticky=0;
    private boolean hasRed=false;

    public PassiveLogger(Context c,String loggerId) {
        myContext = c;
    }

    public void setOutputView(TextView txt) {
        //has no outputview.
    }

    public void addRow(String text) {
        addText(text+"\n");

    }
    public void addRedText(String text) {
        Log.d("amazon","hasred!");
        if (!hasRed) {
            hasRed = true;
            LocalBroadcastManager.getInstance(myContext).sendBroadcast(new Intent(MenuActivity.REDRAW));
        }
        addText(text);
        //Log.d("vortex","hasRed true for "+this.toString());
        //Log.d("vortex",""+this.toString());
    }
    public void addGreenText(String text) {
       addText(text);
    }
    public void addYellowText(String text) {
        addText(text);
    }
    public void addText(String text) {
        log.append(text);
    }

    @Override
    public void addCriticalText(String text) {
        addRow(text);
    }


    public CharSequence getLogText() {
        return log.toString();
    }

    public void draw() {
        //has no widget.
    }


    public void clear() {
    }



    @Override
    public void addPurpleText(String text) {
        addText(text);
    }

    @Override
    public void removeLine() {

    }

    @Override
    public boolean hasRed() {
        if (hasRed) {
            hasRed=false;
            return true;
        }
        return false;
    }


}
