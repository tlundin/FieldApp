package com.teraim.fieldapp.dynamic.types;

import static com.teraim.fieldapp.GlobalState.getInstance;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.DynamicGisPoint;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisFilter;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPathObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPointObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.WF_Gis_Map;
import com.teraim.fieldapp.gis.CurrStatVar;
import com.teraim.fieldapp.gis.GisImageView;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**
 *
 * A Layer holds the GIS Objects drawn in GisImageView, created by block_add_gis_layer.
 * Each GIS Layer may hold reference to any GIS Object type.
 * Each GIS Layer may be visible or hidden, controlled by user.
 * A GIS Layer may or may not have a Widget, controlled by XML Tag.
 * Please see the XML Block definition for block_add_gis_layer
 */

public class GisLayer {

	private final String name;
	private final String label;
	private final boolean hasWidget;
	private boolean hasDynamic=false;
	private Map<String,Set<GisObject>> myObjects;
	private boolean showLabels;
	private Map<String, Set<GisFilter>> myFilters;
	private boolean myVisibility,myBoldness;



	public GisLayer(String name, String label, boolean isVisible, boolean isBold,
					boolean hasWidget, boolean showLabels) {
		super();
		this.name = name;
		this.label = label;
		this.hasWidget = hasWidget;
		this.showLabels=showLabels;
		Log.d("zaza","Creating layer "+label+" with showlabels "+showLabels+" showgislayer "+isVisible+" layer id "+name);

		//check if there is a persisted value for the layer visibility
		Log.d("banjo","Persist layer id "+PersistenceHelper.LAYER_VISIBILITY+name);
		int persistedVisibility = (GlobalState.getInstance()!=null?GlobalState.getInstance().getPreferences().getI(PersistenceHelper.LAYER_VISIBILITY+getId()):-1);
		if (persistedVisibility == -1)
			myVisibility = isVisible;
		else {
			Log.d("zaza","PERSISTED: "+persistedVisibility);
			//a value of 1 means the layer is visible. 0 invisible.
			myVisibility = (persistedVisibility == 1);
		}
		int persistedBoldness = (GlobalState.getInstance()!=null?GlobalState.getInstance().getPreferences().getI(PersistenceHelper.LAYER_BOLDNESS+getId()):-1);
		if (persistedBoldness == -1)
			myBoldness = isBold;
		else {
			Log.d("zaza","PERSISTED: "+persistedBoldness);
			//a value of 1 means the layer is visible. 0 invisible.
			myBoldness = (persistedBoldness == 1);
		}

	}


	public void clear() {
		myObjects=null;
	}

	public void addObjectBag(String key, Set<GisObject> myGisObjects, boolean dynamic) {
		if (myObjects == null) {
			myObjects = new HashMap<String, Set<GisObject>>();
		}

		// Overwrite the existing set with the new set
		myObjects.put(key, myGisObjects); // This already replaces the Set reference

		// Ensure all objects in the newly added bag are marked as useful initially.
		// filterLayer will then unmark those that are out of bounds.
//		if (myGisObjects != null) {
//			for (GisObject go : myGisObjects) {
//				go.markAsUseful(); // Mark all incoming objects as useful
//			}
//		}
		this.hasDynamic = dynamic; //
	}

	/**
	 * Clears the set of GIS objects for a specific bag type within this layer.
	 * This is useful for dynamic layers like "Team" where contents are replaced.
	 * @param typeId The String key (type name) of the bag to clear.
	 */
	public void clearObjectBag(String typeId) {
		if (myObjects != null && myObjects.containsKey(typeId)) {
			myObjects.get(typeId).clear(); // Clear the HashSet associated with this typeId
			Log.d("GisLayer", "Cleared object bag for type: " + typeId + " in layer: " + getId());
		} else {
			Log.w("GisLayer", "Attempted to clear non-existent bag for type: " + typeId + " in layer: " + getId());
		}
	}

	public void addObjectFilter(String key, GisFilter f) {
		Set<GisFilter> setOfFilters = myFilters.get(key);
		if (setOfFilters==null)
			setOfFilters = new HashSet<GisFilter>();

		setOfFilters.add(f);
		Log.d("vortex","added filter "+getId()+" of type "+key);
		myFilters.put(key, setOfFilters);

	}

	public Map<String,Set<GisObject>> getGisBags() {
		return myObjects;
	}

	public Set<GisObject> getBagOfType(String type) {
		if (myObjects !=  null )
			return myObjects.get(type);
		return null;
	}

	public Map<String,Set<GisFilter>> getFilters() {
		if (myFilters !=  null )
			return myFilters;
		return null;
	}

	public void setBold(boolean isBold) {

		Log.d("vortex","SetBold called with "+isBold+" on "+this.getLabel()+" Obj: "+this.toString());
		GlobalState.getInstance().getPreferences().put(PersistenceHelper.LAYER_BOLDNESS+getId(), isBold?1:0);
		this.myBoldness=isBold;
	}

	public void setVisible(boolean isVisible) {

		Log.d("vortex","SetVisible called with "+isVisible+" on "+this.getLabel()+" Obj: "+this.toString());
		GlobalState.getInstance().getPreferences().put(PersistenceHelper.LAYER_VISIBILITY+getId(), isVisible?1:0);
		this.myVisibility=isVisible;
	}

	/** Search for GisObject in all bags.
	 * @param go   -- the object to look for.
	 * @return -- the first instance of the object if found.
	 * */
	public Set<GisObject> getBagContainingGo(GisObject go) {
		if (myObjects == null)
			return null;
		for (String k:myObjects.keySet()) {
			Set<GisObject> gos = myObjects.get(k);
			if (gos.contains(go)) {
				return gos;
			}
		}
		return null;
	}

	public void setShowLabels(boolean show) {
		showLabels=show;
	}

	public boolean hasDynamic() {
		return hasDynamic;
	}

	public boolean isVisible() {
		return myVisibility;
	}

	public boolean isBold() {
		return myBoldness;
	}

	public boolean showLabels() {
		return showLabels;
	}

	public String getLabel() {
		return label;
	}

	public String getId() {
		return name;
	}

	public void clearCaches() {
		if (myObjects==null)
			return;
		for (String key:myObjects.keySet()) {
			Set<GisObject> bag = myObjects.get(key);
			for (GisObject go:bag) {
				go.clearCache();
				go.unmark();
			}
		}
	}


	public boolean hasWidget() {
		return hasWidget;
	}



	/**
	 *
	 *
	 *
	 * Will go through a layer and check if the gisobjects are inside the map.
	 * If inside, the object is marked as useful.
	 * As a sideeffect, calcualte all the local coordinates.
	 */

	public void filterLayer(GisImageView gisImageView) {

		if (myObjects == null) {
			Log.e("fenris","Layer "+ getLabel()+" has no bags. Exiting filterlayer");
			return;
		}

		for (String key:myObjects.keySet()) {
			Set<GisObject> bag = myObjects.get(key);
			Iterator<GisObject> iterator = bag.iterator();
			while (iterator.hasNext()) {
				GisObject go = iterator.next();
				markIfUseful(go,gisImageView);
				if (go.isDefect())
					iterator.remove();
			}
			int c=0;
			for (GisObject gop:bag) {
				if (gop.isUseful())
					c++;
			}
			Log.d("grogg","bag "+key+" has "+c+" useful members");
		}
	}

	private static final int[] xy= new int[2];

	public static void markIfUseful(GisObject go, GisImageView gisImageView) {

		go.unmark();
		if (go instanceof DynamicGisPoint) {
			go.markAsUseful();
		}
		else if (go instanceof GisPointObject) {
			GisPointObject gop = (GisPointObject)go;
			boolean inside = gisImageView.translateMapToRealCoordinates(gop.getLocation(),xy);
			Log.d("GisLayer","translated "+gop.getLocation().getX()+","+gop.getLocation().getY()+" to "+xy[0]+","+xy[1]+" inside "+inside);
			if (inside) {
				go.markAsUseful();
				gop.setTranslatedLocation(xy);
			}
			return;
		}
		else if (go instanceof GisPolygonObject) {
			GisPolygonObject gpo = (GisPolygonObject) go;


			if (gpo.getPolygons()==null) {
				LogRepository.getInstance().addText("");
				LogRepository.getInstance().addCriticalText("POLY had *NULL* coordinates: "+go.getLabel());
				go.markForDestruction();
				return;
			}
			for (List<Location> ll:gpo.getPolygons().values()) {
				for (Location location : ll) {
					if (gisImageView.translateMapToRealCoordinates(location, xy)) {
						gpo.markAsUseful();
						return;
					}
				}
			}

		}
		else if (go instanceof GisPathObject) {
			GisPathObject gpo = (GisPathObject)go;
			boolean hasAtleastOneCornerInside = false;
			if (go.getCoordinates()==null) {
				LogRepository.getInstance().addText("");
				LogRepository.getInstance().addCriticalText("Gis object had *NULL* coordinates: "+go.getLabel());
				go.markForDestruction();
				return;
			}
			for (Location location:go.getCoordinates()) {
				if(gisImageView.translateMapToRealCoordinates(location,xy)) {
					go.markAsUseful();
					return;
				}

			}
		}
	}

	private String printCoordinates(List<Location> coordinates) {
		String s = "";
		if (coordinates==null||coordinates.isEmpty())
			return "EMPTY! Null: "+(coordinates==null);
		for (Location l:coordinates) {
			String st="["+l.getX()+","+l.getY()+"] ";
			s+=st;
		}
		return s;
	}


	public void clearBags() {
		clear();
	}
}