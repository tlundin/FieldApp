package com.teraim.fieldapp.dynamic.types;

import android.graphics.Rect;

/**
 * Created by Terje on 2016-06-08.
 */
public class TabButton {
    //Full rect, cut rect..
    public final Rect r;
    public final Rect fr;
    public final float centY;
    private final String text;
    public final String fullText;

    public TabButton(String textOnButton, Rect tabButtonRect, Rect fullRect,float v,String origText) {
        this.text = textOnButton;
        this.r=tabButtonRect;
        this.centY=v;
        this.fullText = origText;
        fr = fullRect;

    }

    public boolean clickInside(float x, float y) {
        return r.contains((int)x,(int)y);
    }

    public String getShortedText() {
        return text;
    }
}
