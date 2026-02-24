package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;

/**
 * Holds a team member's position and icon for display on the Mapbox map.
 * @param timestampMs Last position update time (epoch ms). Used to decide if needle should wiggle (&lt; 5 min).
 * @param uuid User UUID from server; used to identify current user ("me") via GlobalState.getUserUUID().
 */
public class TeamMemberMapPoint {
	public final String id;
	public final double lat;
	public final double lng;
	public final String name;
	public final Bitmap iconBitmap;
	public final long timestampMs;
	public final String uuid;

	public TeamMemberMapPoint(String id, double lat, double lng, String name, Bitmap iconBitmap) {
		this(id, lat, lng, name, iconBitmap, 0L, null);
	}

	public TeamMemberMapPoint(String id, double lat, double lng, String name, Bitmap iconBitmap, long timestampMs) {
		this(id, lat, lng, name, iconBitmap, timestampMs, null);
	}

	public TeamMemberMapPoint(String id, double lat, double lng, String name, Bitmap iconBitmap, long timestampMs, String uuid) {
		this.id = id;
		this.lat = lat;
		this.lng = lng;
		this.name = name;
		this.iconBitmap = iconBitmap;
		this.timestampMs = timestampMs;
		this.uuid = uuid;
	}
}
