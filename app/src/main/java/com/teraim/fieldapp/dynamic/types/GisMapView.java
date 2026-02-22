package com.teraim.fieldapp.dynamic.types;

import java.io.Serializable;

/**
 * Configuration for a GIS map view, created by block_add_gis_map_view.
 * Holds map type, initial center [lng, lat], zoom, pitch, and bearing.
 */
public class GisMapView implements Serializable {

	private final String blockId;
	private final String name;
	private final String containerName;
	/** "satellite" or "standard" */
	private final String mapType;
	private final double centerLng;
	private final double centerLat;
	private final double zoom;
	private final double pitch;
	private final double bearing;
	/** True to show team member needles. Null when deserialized from old cache (missing field) → treated as true. */
	private final Boolean teamVisible;

	public GisMapView(String blockId, String name, String containerName,
			String mapType, double centerLng, double centerLat,
			double zoom, double pitch, double bearing, boolean teamVisible) {
		this.blockId = blockId;
		this.name = name;
		this.containerName = containerName;
		this.mapType = mapType;
		this.centerLng = centerLng;
		this.centerLat = centerLat;
		this.zoom = zoom;
		this.pitch = pitch;
		this.bearing = bearing;
		this.teamVisible = Boolean.valueOf(teamVisible);
	}

	public String getBlockId() { return blockId; }
	public String getName() { return name; }
	public String getContainerName() { return containerName; }
	public String getMapType() { return mapType; }
	public double getCenterLng() { return centerLng; }
	public double getCenterLat() { return centerLat; }
	public double getZoom() { return zoom; }
	public double getPitch() { return pitch; }
	public double getBearing() { return bearing; }
	/** True when team needles should be shown. Defaults to true when missing in cached JSON (backward compat). */
	public boolean isTeamVisible() { return teamVisible == null || teamVisible; }
}
