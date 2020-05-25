package com.teraim.fieldapp.dynamic.blocks;

import android.widget.LinearLayout;

/**
 * Created by Terje on 2016-11-01.
 */
public class DisplayFieldBlock extends Block {

    private String textColor,backgroundColor;
    private int verticalMargin;
    private int layoutDirection;

    public DisplayFieldBlock() {
        setDefaults();
    }

    public DisplayFieldBlock(String textColor,String backgroundColor,String verticalFormat,String verticalMargin) {
       setDefaults();
        if (textColor!=null)
            this.textColor=textColor;
        this.backgroundColor=backgroundColor;
        this.layoutDirection = LinearLayout.HORIZONTAL;
        if (verticalFormat!=null) {
            if (verticalFormat.equals("two_line")) {
                this.layoutDirection = LinearLayout.VERTICAL;


            }
        }

        if (verticalMargin!=null) {
            this.verticalMargin = Integer.parseInt(verticalMargin);
        }

    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public String getTextColor() {
        return textColor;
    }


    final public int getLayoutDirection() {
        return layoutDirection;
    }

    public int getVerticalMargin() {
        return verticalMargin;
    }

    private void setDefaults() {
        textColor="black";
        verticalMargin=5;
        layoutDirection=LinearLayout.HORIZONTAL;
    }

    public boolean isHorisontal() {
        return layoutDirection==LinearLayout.HORIZONTAL;
    }
}
