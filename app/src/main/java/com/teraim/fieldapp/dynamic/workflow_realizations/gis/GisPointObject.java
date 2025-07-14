package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;
import android.graphics.Paint.Style;

import com.teraim.fieldapp.dynamic.types.Location;
import android.util.Log; // Added for logging within animation logic

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public abstract class GisPointObject extends GisObject {

	final FullGisObjectConfiguration poc;

	private int[] xy=new int[2];

	// NEW: Animation fields for pulsing effect
	private long animationStartTime = -1;
	private long animationDuration = 800; // ms: total duration of one pulse cycle
	private float animationPeakScale = 1.2f; // Max scale (20% larger)
	private boolean isAnimating = false;
	private static final String TAG = "GisPointObject";

	GisPointObject(FullGisObjectConfiguration poc, Map<String, String> keyChain, List<Location> myCoordinates, String statusVar, String statusVal) {
		super(poc,keyChain,myCoordinates,statusVar,statusVal);
		this.poc=poc;

	}

	public abstract boolean isDynamic();
	public abstract boolean isUser();

	public Bitmap getIcon() {
		return poc.getIcon();
	}
	public float getRadius() {
		return poc.getRadius();
	}

	// NEW: Animation methods
	public void startPulseAnimation() {
		this.animationStartTime = System.currentTimeMillis();
		this.isAnimating = true;
		Log.d(TAG, "Starting pulse animation for: " + this.getLabel());
	}

	public void stopAnimation() {
		this.isAnimating = false;
		this.animationStartTime = -1;
		Log.d(TAG, "Stopping animation for: " + this.getLabel());
	}

	public boolean isAnimating() {
		// Also stop if duration exceeded, even if not explicitly stopped.
		if (isAnimating && (System.currentTimeMillis() - animationStartTime >= animationDuration)) {
			stopAnimation();
		}
		return isAnimating;
	}

	/**
	 * Calculates the progress of the current pulse animation (0 to 1).
	 * Returns 0 if not animating.
	 */
	public float getAnimationProgress() {
		if (!isAnimating()) return 0f;

		long elapsed = System.currentTimeMillis() - animationStartTime;
		// Ensure progress doesn't exceed 1.0f (or 0 if stopped)
		return Math.min(1f, (float) elapsed / animationDuration);
	}

	/**
	 * Calculates the current scale factor for the pulse animation.
	 * Returns 1.0f if not animating or animation is complete.
	 */
	public float getCurrentAnimationScale() {
		if (!isAnimating()) return 1.0f;

		float progress = getAnimationProgress(); // 0 to 1
		if (progress >= 1f) return 1.0f; // Animation finished

		// Use a smooth easing function for the pulse.
		// A common one is `0.5 - 0.5 * cos(progress * PI * 2)` for a full cycle (0->1->0)
		// Or a simpler parabolic-like: `progress * (1 - progress) * 4` (peaks at 1.0 at progress 0.5)
		float easedProgress = progress * (1 - progress) * 4; // This goes 0 -> 1 -> 0
		// You can try different easing functions for different effects.
		// For a simple smooth pulse:
		// float easedProgress = (float)(0.5 - 0.5 * Math.cos(progress * Math.PI * 2)); // 0->1->0

		// Scale factor interpolates between 1.0 and animationPeakScale
		return 1.0f + (animationPeakScale - 1.0f) * easedProgress;
	}


	@Override
	public boolean isTouchedByClick(Location mapLocationForClick,double pxr,double pyr) {
		Location myLocation = this.getLocation();
		if (myLocation==null) {
			return false;
		}
		if (this.getWorkflow()==null)
			return false;

		double xD = (mapLocationForClick.getX()-myLocation.getX())*pxr;
		double yD = (mapLocationForClick.getY()-myLocation.getY())*pyr;

		distanceToClick = Math.sqrt(xD*xD+yD*yD);
		return distanceToClick < ClickThresholdInMeters;
	}
	public Style getStyle() {
		return poc.getStyle();
	}


	private final Map<GisFilter,Boolean> filterCache = new HashMap<GisFilter,Boolean>();

	public boolean hasCachedFilterResult(GisFilter filter) {
		return filterCache.get(filter)!=null;
	}
	public void setCachedFilterResult(GisFilter filter, Boolean b) {
		filterCache.put(filter, b);

	}
	public boolean getCachedFilterResult(GisFilter filter) {

		return filterCache.get(filter);
	}

	public int[] getTranslatedLocation() {
		return xy;
	}

	public void setTranslatedLocation(int[] xy) {
		this.xy[0]=xy[0];
		this.xy[1]=xy[1];
	}

	@Override
	public void clearCache() {
		xy=new int[2];
		// Also clear animation state if needed, though `isAnimating()` check handles duration.
		// stopAnimation();
	}

	public void setLabel(String label) {
		this.label = label;
	}

}