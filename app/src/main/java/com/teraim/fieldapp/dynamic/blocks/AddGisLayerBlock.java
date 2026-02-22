package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Drawable;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.MapboxMapHolder;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.fieldapp.log.LogRepository;

public class AddGisLayerBlock extends Block {
	private static final String TAG = "AddGisLayerBlock";


	private final String name;
    private final String label;
    private final String target;
	private final boolean isVisible;
    private final boolean hasWidget;
    private final boolean showLabels;
	private final boolean isBold;
	private final String fillColor;
	private final Float fillOpacity;
	private final String lineColor;
	private final Float lineWidth;
	private final Float circleRadius;
	private final String polyType;

	public AddGisLayerBlock(String id, String name, String label,
			String target, boolean isVisible, boolean hasWidget, boolean showLabels, boolean isBold,
			String fillColor, Float fillOpacity, String lineColor, Float lineWidth,
			Float circleRadius, String polyType) {
		super();
		this.blockId = id;
		this.name = name;
		this.label = label;
		this.target = target;
		this.isVisible = isVisible;
		this.hasWidget = hasWidget;
		this.showLabels = showLabels;
		this.isBold = isBold;
		this.fillColor = fillColor;
		this.fillOpacity = fillOpacity;
		this.lineColor = lineColor;
		this.lineWidth = lineWidth;
		this.circleRadius = circleRadius;
		this.polyType = polyType;
	}

	public void create(WF_Context myContext) {

		Drawable gisMap = myContext.getDrawable(target);
		
		if (gisMap instanceof MapboxMapHolder) {
			MapboxMapHolder holder = (MapboxMapHolder) gisMap;
			Log.d(TAG, "Adding Mapbox layer: " + name + ", polyType=" + polyType);
			holder.addLayer(name, label, isVisible, hasWidget, showLabels, isBold,
					fillColor, fillOpacity, lineColor, lineWidth, circleRadius, polyType);
			Log.d(TAG, "Added Mapbox layer: " + name);
		} else if (gisMap instanceof WF_Gis_Map) {
            WF_Gis_Map myGis = ((WF_Gis_Map) gisMap);
			if (!myGis.isZoomLevel()) {
			final GisLayer gisLayer = new GisLayer(name,label,isVisible,isBold,hasWidget,showLabels);
			Log.d(TAG,"Adding layer "+name+" with myObj"+gisLayer.hashCode());
			myGis.addLayer(gisLayer);
			}
		} else {
			if (gisMap==null) {
				o = LogRepository.getInstance();
				o.addCriticalText("The target map in Gislayerblock "+getBlockId()+" is not found so the layer is not added");
			}
		}
		
	}
	
	
}