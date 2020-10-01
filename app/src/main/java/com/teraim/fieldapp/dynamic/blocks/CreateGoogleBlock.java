package com.teraim.fieldapp.dynamic.blocks;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.google.android.libraries.maps.CameraUpdateFactory;
import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.model.LatLng;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.AsyncResumeExecutorI;
import com.teraim.fieldapp.dynamic.templates.MapTemplate;
import com.teraim.fieldapp.dynamic.types.MapGisLayer;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Container;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisViewImplementation;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GoogleGis;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools.Unit;

import java.util.List;

public class CreateGoogleBlock extends Block {

	/**
	 *
	 */
	private static final long serialVersionUID = 2013870148670474255L;
	final String name, containerId,mapType;
	double lat, lng;
	float zoom;
	public boolean isVisible = false, hasSatNav = false, showUser = false, showTeam = false;


	public CreateGoogleBlock(String id, String name, String containerId, boolean isVisible, String lat, String lng, String zoom, boolean showUser,
							 boolean hasSatNav, boolean showTeam,String mapType) {
		super();
		this.name = name;
		this.containerId = containerId;
		this.isVisible = isVisible;
		this.blockId = id;
		this.lat = Double.parseDouble(lat);
		this.lng = Double.parseDouble(lng);
		this.zoom = Float.parseFloat(zoom);
		this.showUser = showUser;
		this.hasSatNav = hasSatNav;
		this.showTeam = showTeam;
		this.mapType = mapType;

	}


	//Callback after image has loaded.
	private AsyncResumeExecutorI cb;


	/**
	 * @param myContext
	 * @param cb
	 * @return true if loaded. False if executor should pause.
	 */

	public boolean create(WF_Context myContext, final AsyncResumeExecutorI cb) {
		//mapLayers  = new ArrayList<MapGisLayer>();
		Log.d("vortex", "in create for GoogleBlock!");
		Context ctx = myContext.getContext();
		this.cb = cb;
		MapTemplate t = ((MapTemplate) (myContext.getTemplate()));

		LatLng latLng = new LatLng(lat, lng);
		t.getMap().setMapType(getGoogleMapType(mapType));
		t.getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
		if (showUser)
			if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				Log.e("google","FAIL ON PERMISSIONS");
				return true;
			} else
				t.getMap().setMyLocationEnabled(true);

		GoogleGis gis = new GoogleGis(blockId,t.getMapView(),new GisViewImplementation(t.getMap()),myContext,this);
		final Container myContainer = myContext.getContainer(containerId);
		myContainer.add(gis);
		myContext.addGis(gis.getId(),gis);
		myContext.registerEventListener(gis, EventType.onSave);
		myContext.registerEventListener(gis, EventType.onFlowExecuted);
		myContext.addDrawable(name,gis);
		return true;
	}

	private int getGoogleMapType(String mapType) {
		switch (mapType) {
			case "normal":
				return GoogleMap.MAP_TYPE_NORMAL;
			case "satellite":
				return GoogleMap.MAP_TYPE_SATELLITE;
			case "terrain":
				return GoogleMap.MAP_TYPE_TERRAIN;
			case "hybrid":
				return GoogleMap.MAP_TYPE_HYBRID;
			case "none":
				return GoogleMap.MAP_TYPE_NONE;
			default:
				return GoogleMap.MAP_TYPE_NORMAL;

		}
	}
}


