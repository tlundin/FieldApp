package com.teraim.fieldapp.dynamic.templates;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.lifecycle.ViewModelProvider;

import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.types.GisMapView;
import com.teraim.fieldapp.dynamic.types.LatLong;
import com.teraim.fieldapp.dynamic.types.LatLong;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPointObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.MapboxMapHolder;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.TeamMemberMapPoint;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.viewmodels.TeamStatusViewModel;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
	/** When set by AddGisMapViewBlock, used for initial camera and style when the map loads. */
	private GisMapView pendingGisMapViewConfig;
	private final Handler teamUpdateHandler = new Handler(Looper.getMainLooper());
	private Runnable teamUpdateRunnable;
	private TeamStatusViewModel teamStatusViewModel;
	/** Latest team member points for re-apply when map style loads (observer may run before style is ready). */
	private List<TeamMemberMapPoint> lastTeamMemberPoints;

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
		// Always set up team layer when we have the Mapbox map (block_add_gis_map_view). Cached workflow may have teamVisible=false.
		boolean teamEnabled = pendingGisMapViewConfig != null && mapboxMapHolder != null;
		Log.d(TAG, "onViewCreated: pendingConfig=" + (pendingGisMapViewConfig != null)
				+ " mapboxMapHolder=" + (mapboxMapHolder != null) + " -> teamLayerEnabled=" + teamEnabled);

		if (teamEnabled) {
			Log.d(TAG, "Setting up team layer: observing TeamStatusViewModel");
			teamStatusViewModel = new ViewModelProvider(requireActivity()).get(TeamStatusViewModel.class);
			teamStatusViewModel.sendAndReceiveTeamPositions(); // trigger initial fetch
			// Apply current value immediately (LiveData may already have 4 members from MenuActivity polling)
			applyTeamMembersToMap(teamStatusViewModel.teamMemberGisObjects.getValue());
			teamStatusViewModel.teamMemberGisObjects.observe(getViewLifecycleOwner(), this::applyTeamMembersToMap);
			// Periodic refresh so positions update when team members move
			teamUpdateRunnable = new Runnable() {
				@Override
				public void run() {
					if (teamStatusViewModel != null) {
						teamStatusViewModel.sendAndReceiveTeamPositions();
						teamUpdateHandler.postDelayed(this, TimeUnit.SECONDS.toMillis(Constants.LOCATION_UPDATE_INTERVAL));
					}
				}
			};
			teamUpdateHandler.postDelayed(teamUpdateRunnable, TimeUnit.SECONDS.toMillis(Constants.LOCATION_UPDATE_INTERVAL));
		}
		
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

	private static boolean geomatteSelfTestRun;

	private static final String MAP_NEEDLE_DEBUG = "MapNeedle";

	/** Converts team member GisPointObjects to map points and updates the team layer. Called from observer and onViewCreated. */
	private void applyTeamMembersToMap(Set<GisPointObject> teamMembers) {
		if (teamMembers == null || mapboxMapHolder == null) return;
		List<TeamMemberMapPoint> points = new ArrayList<>();
		int index = 0;
		for (GisPointObject gop : teamMembers) {
			com.teraim.fieldapp.dynamic.types.Location loc = gop.getLocation();
			if (loc == null) {
				Log.d(TAG, "Team member " + index + " skipped: loc null");
				continue;
			}
			LatLong latLong;
			if (loc instanceof LatLong) {
				latLong = (LatLong) loc;
			} else if (loc instanceof SweLocation) {
				SweLocation swe = (SweLocation) loc;
				latLong = Geomatte.convertToLatLong(swe.getY(), swe.getX());
			} else {
				Log.d(TAG, "Team member " + index + " skipped: loc type " + loc.getClass().getSimpleName());
				continue;
			}
			// Use name as unique key for map point id (one latest position per user name)
			String id = gop.getKeyHash() != null && gop.getKeyHash().containsKey("author")
					? gop.getKeyHash().get("author") : (gop.getKeyHash() != null && gop.getKeyHash().containsKey("uuid") ? gop.getKeyHash().get("uuid") : "team_" + index);
			android.graphics.Bitmap iconBitmap = gop.getIcon();
			String iconSource = "gop.getIcon()";
			// Always resolve a non-null bitmap so the team layer shows the correct needle
			if (iconBitmap == null) {
				android.content.Context ctx = (view != null) ? view.getContext() : mapboxMapHolder.getWidget().getContext();
				android.graphics.drawable.Drawable d = ContextCompat.getDrawable(ctx, R.drawable.ic_needle_symbol);
				if (d != null) iconBitmap = Tools.drawableToBitmap(d);
				iconSource = "fallback ic_needle_symbol";
			}
			Log.d(MAP_NEEDLE_DEBUG, "[MapTemplate.applyTeamMembersToMap] index=" + index + " name=" + gop.getLabel() + " iconBitmap=" + (iconBitmap != null ? "non-null" : "null") + " iconSource=" + iconSource + " (this bitmap is stored in TeamMemberMapPoint and passed to updateTeamLayer)");
			points.add(new TeamMemberMapPoint(id, latLong.getX(), latLong.getY(),
					gop.getLabel(), iconBitmap));
			if (index < 3) {
				Log.d(TAG, "Team point " + index + " " + gop.getLabel() + " WGS84(" + latLong.getX() + "," + latLong.getY() + ")");
			}
			index++;
		}
		Log.d(TAG, "Team observer: " + teamMembers.size() + " members -> " + points.size() + " points");
		int meCount = 0;
		for (TeamMemberMapPoint p : points) {
			if (p.name != null && p.name.contains("(me)")) meCount++;
		}
		if (meCount != 1) {
			Log.w(MAP_NEEDLE_DEBUG, "[MapTemplate] DUPLICATE_ME? points with (me) in name: " + meCount + " (expected 1)");
		}
		lastTeamMemberPoints = points;
		// Same list (points) is passed below; MapboxMapHolder will use member.iconBitmap which we just set from gop.getIcon()
		String batchId = points.size() + "_" + (points.isEmpty() ? "empty" : points.get(0).name);
		Log.d(MAP_NEEDLE_DEBUG, "[MapTemplate] calling updateTeamLayer with list size=" + points.size() + " batchId=" + batchId);
		mapboxMapHolder.updateTeamLayer(points, batchId);
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
			// Style: satellite vs standard from GisMapView, or default satellite
			String styleUri = Style.SATELLITE_STREETS;
			if (pendingGisMapViewConfig != null && "standard".equalsIgnoreCase(pendingGisMapViewConfig.getMapType())) {
				styleUri = Style.MAPBOX_STREETS;
			}
			Log.d(TAG, "Loading Mapbox style: " + styleUri);
			mapboxMap.loadStyleUri(styleUri, new com.mapbox.maps.Style.OnStyleLoaded() {
				@Override
				public void onStyleLoaded(com.mapbox.maps.Style style) {
					mapReady = true;
					if (mapboxMapHolder != null) {
						mapboxMapHolder.setMapboxMap(mapboxMap);
						// Re-apply team layer if we had data before the map was ready
						if (pendingGisMapViewConfig != null
								&& lastTeamMemberPoints != null && !lastTeamMemberPoints.isEmpty()) {
							mapboxMapHolder.updateTeamLayer(lastTeamMemberPoints, null);
						}
					}
					Log.d(TAG, "Mapbox style loaded successfully");
					// Prefer GisMapView (from block_add_gis_map_view), then pending GIS object center, else Stockholm
					if (pendingGisMapViewConfig != null) {
						applyCameraFromGisMapViewConfig();
					} else {
						double lat = 59.3293;
						double lng = 18.0686;
						double zoom = 8.0;
						double[] pending = GlobalState.getInstance().getAndClearPendingMapCenter();
						if (pending != null && pending.length >= 2) {
							lat = pending[0];
							lng = pending[1];
							zoom = 14.0;
							Log.d(TAG, "Map centering on GIS object (WGS84): lat=" + lat + ", lng=" + lng);
						} else {
							Log.d(TAG, "Map using default camera: Stockholm");
						}
						Point initialPoint = Point.fromLngLat(lng, lat);
						mapboxMap.setCamera(new CameraOptions.Builder().center(initialPoint).zoom(zoom).build());
					}
					Log.d(TAG, "Map is ready");
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

	/**
	 * Called by Executor when AddGisMapViewBlock runs. Registers the map under the block's map name
	 * and applies initial camera/style when the map loads. Applies camera post-draw if the map is already ready.
	 */
	public void registerMapboxMapFromGisMapView(GisMapView config) {
		if (config == null) return;
		pendingGisMapViewConfig = config;
		registerMapboxMapAsDrawable(config.getName());
		Log.d(TAG, "Registered map from GisMapView: " + config.getName() + " center=" + config.getCenterLat() + "," + config.getCenterLng() + " zoom=" + config.getZoom());
		// Apply camera after next layout/draw so we use GisMapView even if map is already initialized
		if (mapView != null) {
			mapView.post(this::applyCameraFromGisMapViewConfig);
		}
	}

	/**
	 * Applies camera (and optional pitch/bearing) from pendingGisMapViewConfig when the map is ready.
	 * Called from style load callback and post-draw from registerMapboxMapFromGisMapView.
	 */
	private void applyCameraFromGisMapViewConfig() {
		if (pendingGisMapViewConfig == null || mapboxMap == null) return;
		double lat = pendingGisMapViewConfig.getCenterLat();
		double lng = pendingGisMapViewConfig.getCenterLng();
		double zoom = pendingGisMapViewConfig.getZoom();
		double pitch = pendingGisMapViewConfig.getPitch();
		double bearing = pendingGisMapViewConfig.getBearing();
		Point point = Point.fromLngLat(lng, lat);
		CameraOptions.Builder builder = new CameraOptions.Builder().center(point).zoom(zoom);
		if (pitch != 0.0 || bearing != 0.0) {
			builder.pitch(pitch).bearing(bearing);
		}
		mapboxMap.setCamera(builder.build());
		Log.d(TAG, "Applied GisMapView camera: lat=" + lat + ", lng=" + lng + ", zoom=" + zoom);
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
	public void onResume() {
		super.onResume();
		// When map is visible: apply latest team data (from ViewModel or cached). Team layer always enabled for Mapbox map.
		if (pendingGisMapViewConfig != null && mapboxMapHolder != null) {
			if (teamStatusViewModel == null) {
				teamStatusViewModel = new ViewModelProvider(requireActivity()).get(TeamStatusViewModel.class);
			}
			// Trigger sync so "me" is updated with current needle preference (e.g. after returning from settings).
			teamStatusViewModel.sendAndReceiveTeamPositions();
			Set<GisPointObject> current = teamStatusViewModel.teamMemberGisObjects.getValue();
			if (current != null && !current.isEmpty()) {
				Log.d(TAG, "onResume: applying team layer from ViewModel (" + current.size() + " members)");
				applyTeamMembersToMap(current);
			} else if (lastTeamMemberPoints != null && !lastTeamMemberPoints.isEmpty()) {
				Log.d(TAG, "onResume: re-applying cached team layer (" + lastTeamMemberPoints.size() + " members)");
				mapboxMapHolder.updateTeamLayer(lastTeamMemberPoints, null);
			}
		}
	}

	@Override
	public void onDestroyView() {
		if (teamUpdateRunnable != null) {
			teamUpdateHandler.removeCallbacks(teamUpdateRunnable);
			teamUpdateRunnable = null;
		}
		super.onDestroyView();
		if (mapView != null) {
			mapView.onDestroy();
			mapView = null;
			mapboxMap = null;
			mapReady = false;
		}
	}

}
