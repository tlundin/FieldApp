package com.teraim.fieldapp.dynamic.blocks;

import android.widget.LinearLayout;
public class DisplayFieldBlock extends Block {

    private String textColor=null;
    private String backgroundColor=null;
    private int verticalMargin=5;
    private int layoutDirection=LinearLayout.HORIZONTAL;

    public DisplayFieldBlock() {
    }

    public DisplayFieldBlock(String textColor,String backgroundColor,String verticalFormat,String verticalMargin) {
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
    public boolean isHorisontal() {
        return layoutDirection==LinearLayout.HORIZONTAL;
    }
}
