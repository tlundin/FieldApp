package com.teraim.fieldapp.dynamic.types;

/**
 * Created by Terje on 2016-07-13.
 */
public interface NudgeListener {


    enum Direction {
        LEFT,
        RIGHT,
        UP,
        DOWN,
        NONE
    }


    void onNudge(Direction d, int step);
    void centerOnNudgeDot();

}
