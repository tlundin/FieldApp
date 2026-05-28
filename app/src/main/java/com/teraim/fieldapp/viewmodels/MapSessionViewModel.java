package com.teraim.fieldapp.viewmodels;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;

/**
 * Retains map camera and walk-navigation state across MapTemplate view recreation
 * (e.g. device rotation or configuration change).
 */
public class MapSessionViewModel extends ViewModel {

	public boolean hasSavedCamera;
	public double savedCenterLat;
	public double savedCenterLng;
	public double savedZoom;
	public double savedPitch;
	public double savedBearing;

	public static final int WALK_NAV_ACTION_NONE = 0;
	public static final int WALK_NAV_ACTION_GIS_OBJECT = 1;
	public static final int WALK_NAV_ACTION_TRAKT = 2;

	public boolean walkNavActive;
	public double walkNavTargetLat;
	public double walkNavTargetLng;
	public boolean hasWalkNavStart;
	public double walkNavStartLat;
	public double walkNavStartLng;
	@Nullable
	public String walkNavTargetLabel;
	public int walkNavStartActionType = WALK_NAV_ACTION_NONE;
	@Nullable
	public String walkNavFeatureJson;
	@Nullable
	public String walkNavObjContext;
	@Nullable
	public String walkNavOnClick;
	@Nullable
	public String walkNavTraktName;

	public boolean mapTypeSatellite = true;

	public void clearWalkNavigation() {
		walkNavActive = false;
		hasWalkNavStart = false;
		walkNavTargetLabel = null;
		walkNavStartActionType = WALK_NAV_ACTION_NONE;
		walkNavFeatureJson = null;
		walkNavObjContext = null;
		walkNavOnClick = null;
		walkNavTraktName = null;
	}

	public void clearCamera() {
		hasSavedCamera = false;
	}
}
