package com.teraim.fieldapp.dynamic.types;

import android.annotation.SuppressLint;

/**
 * Point on 2D landscape
 *
 * @author Roman Kushnarenko (sromku@gmail.com)</br>
 */
public record Point(float x, float y) {

    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("(%.2f,%.2f)", x, y);
    }
}