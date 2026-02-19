package com.teraim.fieldapp.dynamic.templates;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.common.MapboxOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;
import com.mapbox.maps.MapboxMap;
import com.mapbox.maps.Style;
import com.teraim.fieldapp.BuildConfig;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.MapboxMapHolder;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Template that can render a workflow with an optional Mapbox map background.
 */
public class MapTemplate extends Executor {
	private static final String TAG = "MapTemplate";

	private View view;
	private LinearLayout my_root;
	private MapView mapView;
	private MapboxMap mapboxMap;
	private MapboxMapHolder mapboxMapHolder;
	private FloatingActionButton fabLayerToggle;
	private boolean mapReady = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d(TAG, "In onCreate - MapTemplate");

		// Ensure Mapbox has an access token set
		try {
			MapboxOptions.setAccessToken(BuildConfig.MAPBOX_ACCESS_TOKEN);
		} catch (Exception e) {
			Log.e(TAG, "Failed to set Mapbox access token", e);
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		Log.d(TAG, "I'm in the onStart method");
	}

	@Override
	public void onPause() {
		super.onPause();
		Log.d(TAG, "MapTemplate - onPause");
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		Log.d(TAG, "I'm in the onCreateView method for MapTemplate");
		if (GlobalState.getInstance() == null) {
			Log.e("Vortex", "globalstate is null...exiting");
			return null;
		}
		if (view == null) {
			view = inflater.inflate(R.layout.template_map, container, false);

			mapView = view.findViewById(R.id.mapView);
			my_root = view.findViewById(R.id.myRoot);
			fabLayerToggle = view.findViewById(R.id.fab_layer_toggle);
			fabLayerToggle.setVisibility(View.GONE);

			String gisObjectsBaseUrl = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.SERVER_URL)
					+ GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME).toLowerCase(Locale.ROOT)
					+ "/gis_objects/wgs/";
			mapboxMapHolder = new MapboxMapHolder(mapView, gisObjectsBaseUrl);

			if (myContext != null) {
				myContext.addContainers(getContainers());
			} else {
				Log.e("brexit", "mycontext was null! Couldnt add containers");
			}

			if (wf != null) {
				Log.d(TAG, "Executing workflow!!");
				run();

			} else {
				Log.d(TAG, "No workflow found in onCreate MapTemplate!!!!");
			}

		} else {
			// If view exists, we are moving backwards in the stack. GIS objects need to drop their cached values.
			if (myContext != null && myContext.getCurrentGis() != null) {
				Log.d(TAG, "Clearing gis cache in onCreateView");
				myContext.getCurrentGis().clearLayerCaches();
				myContext.getCurrentGis().getGis().initializeAndSiftGisObjects();
			}
		}
		return view;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		Log.d(TAG, "onViewCreated called");
		
		// Wait for view to be attached to window before initializing map
		if (view.getWindowToken() != null) {
			initializeMapIfReady();
		} else {
			view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
				@Override
				public void onViewAttachedToWindow(View v) {
					Log.d(TAG, "View attached to window, initializing map");
					initializeMapIfReady();
					v.removeOnAttachStateChangeListener(this);
				}

				@Override
				public void onViewDetachedFromWindow(View v) {
					// No-op
				}
			});
		}
	}

	private void initializeMapIfReady() {
		if (shouldShowMap()) {
			initializeMapIfNeeded();
		} else if (mapView != null) {
			mapView.setVisibility(View.GONE);
		}
	}

	private void initializeMapIfNeeded() {
		if (mapView == null) {
			Log.w(TAG, "MapView is null, cannot initialize map.");
			return;
		}
		
		Log.d(TAG, "Initializing map. MapView visibility: " + mapView.getVisibility() + 
				", width: " + mapView.getWidth() + ", height: " + mapView.getHeight());
		
		mapView.setVisibility(View.VISIBLE);
		
		// Ensure MapView has valid dimensions before initializing
		if (mapView.getWidth() == 0 || mapView.getHeight() == 0) {
			Log.d(TAG, "MapView has zero dimensions, waiting for layout");
			mapView.post(() -> {
				Log.d(TAG, "MapView layout complete, width: " + mapView.getWidth() + 
						", height: " + mapView.getHeight());
				doInitializeMap();
			});
		} else {
			doInitializeMap();
		}
	}

	private void doInitializeMap() {
		if (mapboxMap == null) {
			mapboxMap = mapView.getMapboxMap();
			Log.d(TAG, "Loading Mapbox style...");
			// Detailed satellite style
			mapboxMap.loadStyleUri(Style.SATELLITE_STREETS, new com.mapbox.maps.Style.OnStyleLoaded() {
				@Override
				public void onStyleLoaded(com.mapbox.maps.Style style) {
					mapReady = true;
					if (mapboxMapHolder != null) {
						mapboxMapHolder.setMapboxMap(mapboxMap);
					}
					Log.d(TAG, "Mapbox style loaded successfully");
					// Use pending map center (from GIS object) if set, otherwise default to Stockholm
					double lat = 59.3293;
					double lng = 18.0686;
					double zoom = 8.0;
					double[] pending = GlobalState.getInstance().getAndClearPendingMapCenter();
					if (pending != null && pending.length >= 2) {
						lat = pending[0];
						lng = pending[1];
						zoom = 14.0; // closer zoom when focusing on a specific object
						Log.d(TAG, "Map centering on GIS object (WGS84): lat=" + lat + ", lng=" + lng);
					}
					Point initialPoint = Point.fromLngLat(lng, lat);
					mapboxMap.setCamera(new CameraOptions.Builder()
							.center(initialPoint)
							.zoom(zoom)
							.build());
					Log.d(TAG, "Mapbox style loaded, map is ready. Camera: lat=" + lat + ", lng=" + lng + ", zoom=" + zoom);
					setupLayerToggleFab();
				}
			});
		}
	}

	/**
	 * Called by Executor when CreateGisBlock is skipped so the map drawable is registered under the workflow's map name.
	 */
	public void registerMapboxMapAsDrawable(String mapName) {
		if (myContext != null && mapboxMapHolder != null) {
			myContext.addDrawable(mapName, mapboxMapHolder);
			Log.d(TAG, "Registered MapboxMapHolder as drawable: " + mapName);
		}
	}

	public MapboxMapHolder getMapboxMapHolder() {
		return mapboxMapHolder;
	}

	private void setupLayerToggleFab() {
		if (fabLayerToggle == null || mapboxMapHolder == null) return;
		fabLayerToggle.setVisibility(View.VISIBLE);
		fabLayerToggle.setOnClickListener(v -> showLayerSelectionDialog());
	}

	private void showLayerSelectionDialog() {
		if (mapboxMapHolder == null || getContext() == null) return;
		List<String> layerNames = mapboxMapHolder.getLayerNames();
		if (layerNames.isEmpty()) {
			new AlertDialog.Builder(requireContext())
					.setMessage(getString(R.string.no_layers_loaded))
					.setPositiveButton(android.R.string.ok, null)
					.show();
			return;
		}
		LinearLayout container = new LinearLayout(requireContext());
		container.setOrientation(LinearLayout.VERTICAL);
		container.setPadding(50, 40, 50, 40);
		for (String name : layerNames) {
			CheckBox cb = new CheckBox(requireContext());
			cb.setText(name);
			cb.setChecked(mapboxMapHolder.isLayerVisible(name));
			cb.setOnCheckedChangeListener((buttonView, isChecked) -> mapboxMapHolder.setLayerVisibility(name, isChecked));
			container.addView(cb);
		}
		new AlertDialog.Builder(requireContext())
				.setTitle(R.string.select_layers_title)
				.setView(container)
				.setPositiveButton(android.R.string.ok, null)
				.show();
	}

	private boolean shouldShowMap() {
		GlobalState gs = GlobalState.getInstance();
		if (gs == null) {
			return false;
		}
		PersistenceHelper globalPh = gs.getGlobalPreferences();
		boolean mapsEnabled = globalPh.getPreferences().getBoolean(PersistenceHelper.MAP_ENABLED, true);
		return mapsEnabled && Tools.hasNetworkConnection(gs.getContext());
	}

	/**
	 * Indicates whether the Mapbox map has finished loading its style.
	 */
	public boolean mapIsReady() {
		return mapReady;
	}

	@Override
	protected List<WF_Container> getContainers() {
		ArrayList<WF_Container> ret = new ArrayList<>();
		ret.add(new WF_Container("root", my_root, null));
		return ret;
	}

	@Override
	public boolean execute(String function, String target) {
		return true;
	}


	@Override
	public void onDestroyView() {
		super.onDestroyView();
		if (mapView != null) {
			mapView.onDestroy();
			mapView = null;
			mapboxMap = null;
			mapReady = false;
		}
	}

}
