package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.graphics.Paint.Style;
import android.util.Log;

import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.utils.Geomatte;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GisPolygonObject extends GisPathObject {
	private static final String TAG = "GisPolygonObject";


	private Map<String, List<Location>> polygons;
	private FullGisObjectConfiguration conf;
	//String representation of polygon.
	private String polyString;
	

	//Called when XML has defined the object with a full configuration
	public GisPolygonObject(FullGisObjectConfiguration conf, Map<String, String> keyChain,
			String polygons,String coordType,String statusVar,String statusVal) {

		super(conf,keyChain,null,statusVar,statusVal);
		this.polygons=null;
		polyString = polygons;
		this.conf=conf;
		//Log.d(TAG,"Added "+keyChain.toString()+" name:"+conf.getName()+" db "+conf.getObjectKeyHash());
	}

	//Called when the object is imported using only a partial configuration. 
	public GisPolygonObject(Map<String, String> keyChain,
			Map<String, List<Location>> polygons,
			Map<String, String> attributes) {
		super(keyChain,null,attributes);
		this.polygons=polygons;
		//Log.d(TAG,"polygons key "+polygons.keySet()+" cont: "+polygons.values());
	}

	private static Map<String, List<Location>> buildMap(String polygons, String coordType) {
		int i=1;
		if (polygons==null) 
			return null;
		String[] polys = polygons.split("\\|");
		Map<String, List<Location>> ret = new HashMap<String, List<Location>>();
		if (polys == null || polys.length==0)
			ret.put("Poly 1", new ArrayList<Location>());
		else {
		for (String poly:polys) {
			//Log.d(TAG,"in poly with poly: ["+poly+"]");
			ret.put("Poly "+i, GisObject.createListOfLocations(poly, coordType));
			
			i++;
		}
		}
		
		return ret;
	}

	//returns the first main polygon. The others are apparaently only holes.
	@Override
	public List<Location> getCoordinates() {
		//If string representation, parse it now and cache
		if (polygons==null && polyString!=null)
			polygons = buildMap(polyString,GisConstants.SWEREF);
		//still null?
		if (polygons==null) {
			Log.e("Vortex","No polygon found");
			return null;
		}
		myCoordinates = polygons.get("Poly 1");
		
		if (myCoordinates==null)
				Log.e("Vortex","No poly 1 found");
		//else
		//	Log.d(TAG,"coordies "+myCoordinates);
		return myCoordinates;
	}


	@Override
	public String coordsToString() {
		if (polygons==null)
			return null;
		String ret="";
		for(List<Location> l:polygons.values()) {
			myCoordinates = l;
			ret+= super.coordsToString()+"|";
		}
		if (!ret.isEmpty())
			ret = ret.substring(0,ret.length()-1);
		return ret;
	}

	//return the geometrical centroid of the first polygon. 
	@Override
	public Location getLocation() {
		if (polygons == null) return null;
		List<Location> p1 = getCoordinates();
		if (p1==null||p1.isEmpty()) {
			return null;
		}
		double centroidX=0,centroidY=0;
		for (Location l:p1) {
			centroidX+=l.getX();
			centroidY+=l.getY();
			
		}
		//TODO: Add LATLONG!!
		return new SweLocation(centroidX/p1.size(),centroidY/p1.size());
	}

	public Map<String, List<Location>> getPolygons() {
		//If string representation, parse it now.
		if (polygons==null && polyString!=null)
			polygons = buildMap(polyString,GisConstants.SWEREF);
		return polygons;
	}

	public String getColor() {
		return conf.getColor();
	}

	public Style getStyle() {
		return conf.getStyle();
	}
	
	@Override
	public boolean isTouchedByClick(Location mapLocationForClick,double pxr,double pyr) {
		
		if (this.getWorkflow()==null)
			return false;
		myCoordinates =  getCoordinates() ;
		if (myCoordinates == null||myCoordinates.isEmpty()) {
			Log.d(TAG, "found no coordinates...exiting");
			return false;
		}
		distanceToClick = ClickThresholdInMeters;
		for (int i=0;i<myCoordinates.size()-1;i++) {

			//Location A = Geomatte.subtract(myCoordinates.get(i),mapLocationForClick);
			//Location B = Geomatte.subtract(myCoordinates.get(i+1),mapLocationForClick);
			Location A = myCoordinates.get(i);
			Location B = myCoordinates.get(i+1);
			//double dist = Geomatte.pointToLineDistance(A, B, origo);
			double dist2 = Geomatte.pointToLineDistance4(mapLocationForClick.getX(),mapLocationForClick.getY(),A.getX(),A.getY(),B.getX(),B.getY());

			if (dist2<distanceToClick)
				distanceToClick=dist2;

		}

        return distanceToClick < ClickThresholdInMeters;


		/*
		distanceToClick=ClickThresholdInMeters;
		for (int i=0;i<myCoordinates.size()-1;i++) {

			Location A = myCoordinates.get(i);
			Location B = myCoordinates.get(i+1);
			double dist = Geomatte.pointToLineDistance3(A, B, mapLocationForClick,pxr,pyr);
			//Log.d(TAG,"dist to "+this.getId()+" is "+dist+ "Thresh was Divided by pxr: "+ClickThresholdInMeters/pxr);
			if (dist<distanceToClick) 
				distanceToClick=dist;
		}
		if (distanceToClick<ClickThresholdInMeters)
			return true;
		return false;
		*/
	}
	

}
