package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.util.Log;

import com.teraim.fieldapp.dynamic.types.LatLong;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisPolyType;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.Shape;
import com.teraim.fieldapp.utils.Expressor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GisObject {

	
	static final double ClickThresholdInMeters = 30;
	double distanceToClick=-1;
	protected String label=null;
	private String statusVariableId=null,statusVariableValue=null;
	private Variable statusVariable = null;
	public void setStatusVariable(Variable statusVariable) {
		this.statusVariable = statusVariable;
	}


	public enum CoordinateType {
		sweref,
		latlong
	}

	protected FullGisObjectConfiguration foc;
	
	public GisObject(Map<String, String> keyChain,List<Location> myCoordinates) {
		this.keyChain=keyChain;this.myCoordinates=myCoordinates;
		
	}
	
	public GisObject(Map<String, String> keyChain,
			List<Location> myCoordinates, Map<String, String> attributes) {
		this.keyChain=keyChain;this.myCoordinates=myCoordinates;this.attributes=attributes;
	}

	GisObject(FullGisObjectConfiguration conf,
              Map<String, String> keyChain, List<Location> myCoordinates, String statusVarName, String statusVariableValue) {
		this.keyChain=keyChain;
		this.foc = conf;
		this.myCoordinates=myCoordinates;
		this.statusVariableId=statusVarName;
		this.statusVariableValue=statusVariableValue;
		
	}
	
	

	protected CoordinateType coordinateType = CoordinateType.sweref;
	List<Location> myCoordinates = new ArrayList<Location>();
	private final Map<String, String> keyChain;
	private Map<String, String> attributes;
	private boolean isUseful;
	private boolean isDefect;
	

	//This default behavior is overridden for objects with more than one point or dynamic value. See subclasses for implementation.
	public Location getLocation() {
		if (myCoordinates==null || myCoordinates.isEmpty())
			return null;
		return myCoordinates.get(0);		
	}


    public List<Location> getCoordinates() {
		return myCoordinates;
	}
	
	public Map<String, String> getKeyHash() {
		return keyChain;
	}
	
	public Map<String, String> getAttributes() {
		return attributes;
	}
	
	public String getWorkflow() {
		return foc.getClickFlow();
	}
	
	public String getLabel() {
		if (label!=null)
			return label;
		if (foc.getLabelExpression()==null)
			return null;
		//Log.d("vortex","In getLabel gisobject..analyzing: "+foc.getLabelExpression()+" with keychain "+keyChain);
		label = Expressor.analyze(foc.getLabelExpression(),keyChain);
		//@notation for id
		if (label!=null && label.startsWith("@")) {
			String key = label.substring(1, label.length());
			if (key.length()>0) 
				label = keyChain.get(key);
			
		} 
		if (label==null)
			label = "";
		
		return label;
	}
	
	public String getId() {
		return foc.getName();
	}
	
	public GisPolyType getGisPolyType() {
		return foc.getGisPolyType();
	}
	
	public Shape getShape() {
		return foc.getShape();
	}
	
	public String getStatusVariableId() {
		return statusVariableId;
	}

	public String getStatus() {
		if (statusVariable==null) {
			//Log.d("vortex","value only");
			return statusVariableValue;
		}
		else
			return statusVariable.getValue();
	}

	
	public String getColor() {
		return foc.getColor();
	}

	public FullGisObjectConfiguration getFullConfiguration() {return foc;}
	
	public double getDistanceToClick() {
		return distanceToClick;
	}


	public static List<Location> createListOfLocations(String value, String coordType) {
		if (value==null) {
			return null;
		}
		String[] coords = value.split(",");
		boolean isXCoordinate=true;
		String coordX=null;
		List<Location> ret = new ArrayList<Location>();

		for (String coord:coords) {
			if (isXCoordinate) {
				isXCoordinate=false;
				coordX=coord;
			} else {
				isXCoordinate=true;
				if(coordType.equalsIgnoreCase(GisConstants.SWEREF))
					ret.add(new SweLocation(coordX,coord));
				else {

					Log.e("vortex","kukkaborra: "+coordType);
					ret.add(new LatLong(coordX, coord));
				}
			}
		}
		//Log.d("vortex","createlistlocations returning: "+ret.toString());

		return ret;
	}
	
	public String coordsToString() {
		if (myCoordinates == null)
			return null;
		StringBuilder sb = new StringBuilder();
		for (Location l:myCoordinates) {
			
			sb.append(l.toString());
			sb.append(",");
		}
		if (sb.length()>0)
			return sb.substring(0, sb.length()-1);
		else
			return null;
	}

	//Should be overridden by subclasses.

	public boolean isTouchedByClick(Location mapLocationForClick, double pxr,
			double pyr) {
		Log.e("vortex","Should never be here");
		return false;
	}


	//Should be overridden. 
	
	public void clearCache() {Log.e("vortex","ERROR: Getszzzzz");}

	public void markAsUseful() {
		this.isUseful = true;
	}
	
	//Used in rare cases when one parameter of the object has null value
	public void markForDestruction() {
		this.isDefect = true;
	}
	
	public void unmark() {
		this.isUseful = false;
	}

	public boolean isUseful() {
		return isUseful;
	}

	public boolean isDefect() {
		return isDefect;
	}


}
