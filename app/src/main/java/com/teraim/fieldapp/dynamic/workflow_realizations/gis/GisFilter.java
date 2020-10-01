package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;
import android.graphics.Paint.Style;

import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.Shape;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

public interface GisFilter {

	EvalExpr getExpression();

	String getLabel();

	Bitmap getBitmap();

	float getRadius();

	String getColor();

	Style getStyle();

	Shape getShape();
	
	boolean isActive();

	

}
