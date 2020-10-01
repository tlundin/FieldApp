package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;
import android.graphics.Paint;

import com.teraim.fieldapp.dynamic.types.DB_Context;


//Subclass with interfaces that restricts access to all.

public interface FullGisObjectConfiguration extends GisObjectBaseAttributes {

	enum GisPolyType {
		Point,
		Multipoint,
		Polygon,
		Linestring
	}
	
	enum Shape {
		circle,
		rect,
		triangle,
		marker
	}
	
	float getRadius();
	String getColor();
	GisPolyType getGisPolyType();
	Bitmap getIcon();
	Paint.Style getStyle();
	Shape getShape();
	String getClickFlow();
	DB_Context getObjectKeyHash();
	String getStatusVariable();
	boolean isUser();
	String getName();
	String getRawLabel();
	String getCreator();
	boolean useIconOnMap();
	
	
}
