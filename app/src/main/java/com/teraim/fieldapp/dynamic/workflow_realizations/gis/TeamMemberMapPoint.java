package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;

/**
 * Holds a team member's position and icon for display on the Mapbox map.
 */
public class TeamMemberMapPoint {
	public final String id;
	public final double lat;
	public final double lng;
	public final String name;
	public final Bitmap iconBitmap;

	public TeamMemberMapPoint(String id, double lat, double lng, String name, Bitmap iconBitmap) {
		this.id = id;
		this.lat = lat;
		this.lng = lng;
		this.name = name;
		this.iconBitmap = iconBitmap;
	}
}
