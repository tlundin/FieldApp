package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;
import android.graphics.Paint;

import com.teraim.fieldapp.dynamic.types.DB_Context;


//Subclass with interfaces that restricts access to all.

public interface FullGisObjectConfiguration extends GisObjectBaseAttributes {

	enum GisObjectType {
		Point,
		Multipoint,
		Polygon, Linestring
	}
	
	enum PolyType {
		circle,
		rect,
		triangle
	}
	
	float getRadius();
	String getColor();
	String getBorderColor();
	GisObjectType getGisPolyType();
	Bitmap getIcon();
	Paint.Style getStyle();
	PolyType getShape();
	String getClickFlow();
	DB_Context getObjectKeyHash();
	String getStatusVariable();
	boolean isUser();
	String getName();
	float getLineWidth();
	String getRawLabel();
	String getCreator();
	boolean useIconOnMap();
	
	
}
