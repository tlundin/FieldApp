package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.util.Log;

import com.teraim.fieldapp.dynamic.types.LatLong;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.GisObjectType;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration.PolyType;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.Expressor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GisObject {


	static final double ClickThresholdInMeters = 30;
	double distanceToClick=-1;
	protected String label=null;
	private String statusVariableId=null,statusVariableValue=null;
	private FullGisObjectConfiguration foc;

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
		//if (statusVariableValue!=null && statusVariableValue.equals(Constants.STATUS_KLAR_I_DB))
		//	Log.d("plaxxo","status 100 for "+statusVarName+" "+conf.getRawLabel());
	}

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

		if (label!=null) {
			//Log.d("pex2","returning "+label);
			return label;
		}
		if (foc.getLabelExpression()==null) {
			//Log.d("pex2","returning null");
			return null;
		}
		//Log.d("pex2","In getLabel gisobject..analyzing: "+foc.getLabelExpression()+" with keychain "+keyChain);
		label = Expressor.analyze(foc.getLabelExpression(),keyChain);
		//@notation for id
		if (label!=null && label.startsWith("@")) {
			String key = label.substring(1, label.length());
			if (key.length()>0)
				label = keyChain.get(key);

		}
		if (label==null)
			label = "";
		//Log.d("pex2","returning OUT"+label);
		return label;
	}

	public String getId() {
		return foc.getName();
	}

	public  GisObjectType getGisPolyType() {
		return foc.getGisPolyType();
	}

	public PolyType getShape() {
		return foc.getShape();
	}

	public String getStatusVariableId() {
		return statusVariableId;
	}

	public String getStatusVariableValue() {
		return statusVariableValue;
	}

	public String getColor() {
		return foc.getColor();
	}

	public String getBorderColor() {
		return foc.getBorderColor();
	}
	public String getStatusColor() {
		if (statusVariableId==null || statusVariableValue==null)
			return null;
		int statusValue = Integer.parseInt(statusVariableValue);
		//Log.d("fenris","statvar "+statusVariableId+" value was "+statusVariableValue+" statval is "+statusValue);
		if (statusVariableId.equals("STATUS:status_trakter")) {
			//Log.d("fenris","gop "+this.getLabel()+" object "+this.hashCode()+" has status "+statusVariableValue);
			if (statusVariableValue.equals(Constants.STATUS_HIGH_PRIORITY))
					return "#9900ff";
			else if (statusVariableValue.equals(Constants.STATUS_INITIAL))
				return foc.getColor();
			else if (statusValue>0 && statusValue<=30)
				return "#ff9600";
			else if (statusValue>30 && statusValue<=70)
				return "#a6fc00";
			else if (statusValue>70 && statusValue<100)
				return "#008500";
			else if (statusValue==100)
				return "#00FFFF";
			else
				return foc.getColor();
		}
		switch (statusVariableValue) {
			case Constants.STATUS_STARTAD_MED_FEL:
				return "#ff0000";
			case Constants.STATUS_AVSLUTAD_EXPORT_MISSLYCKAD:
				return "#fca005";
			case Constants.STATUS_STARTAD_MEN_INTE_KLAR:
				return "black";
			case Constants.STATUS_AVSLUTAD_EXPORTERAD:
				return "#008500";
			case Constants.STATUS_KLAR_I_DB:
				return "#00FFFF";

		}
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
