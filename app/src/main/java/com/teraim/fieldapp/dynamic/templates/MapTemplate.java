package com.teraim.fieldapp.dynamic.templates;

import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.common.MapboxOptions;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.CameraState;
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
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.MapboxOrnamentsKt;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.TeamMemberMapPoint;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.viewmodels.MapSessionViewModel;
import com.teraim.fieldapp.viewmodels.TeamStatusViewModel;

import androidx.core.content.ContextCompat;

import kotlin.Unit;

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
	private FloatingActionButton fabTools;
	private FloatingActionButton fabRefresh;
	private View fabMenuContainer;
	private boolean fabMenuExpanded = false;
	private boolean mapReady = false;
	/** When set by AddGisMapViewBlock, used for initial camera and style when the map loads. */
	private GisMapView pendingGisMapViewConfig;
	private final Handler teamUpdateHandler = new Handler(Looper.getMainLooper());
	private Runnable teamUpdateRunnable;
	private Runnable meUpdateRunnable;
	private TeamStatusViewModel teamStatusViewModel;
	/** Latest team member points for re-apply when map style loads (observer may run before style is ready). */
	private List<TeamMemberMapPoint> lastTeamMemberPoints;
	/** True = satellite, false = standard streets. */
	private boolean mapTypeSatellite = true;
	private MapSessionViewModel mapSessionViewModel;

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
		android.app.Activity activity = getActivity();
		if (activity instanceof com.teraim.fieldapp.Start) {
			((com.teraim.fieldapp.Start) activity).setToolbarTransparent(true);
		}
		if (mapView != null) {
			mapView.onStart();
		}
		Log.d(TAG, "I'm in the onStart method");
	}

	@Override
	public void onPause() {
		persistMapSessionState();
		super.onPause();
		android.app.Activity activity = getActivity();
		if (activity instanceof com.teraim.fieldapp.Start) {
			((com.teraim.fieldapp.Start) activity).setToolbarTransparent(false);
		}
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
			fabTools = view.findViewById(R.id.fab_tools);
			fabRefresh = view.findViewById(R.id.fab_refresh);
			fabMenuContainer = view.findViewById(R.id.fab_menu_container);
			fabLayerToggle.setVisibility(View.GONE);
			setupFabMenu(view);
			setupRefreshButton(view);
			setupZoomButtons(view);
			setupCenterOnUserButton(view);
			setupMapTypeToggleButton(view);

			String gisObjectsBaseUrl = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.SERVER_URL)
					+ GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME).toLowerCase(Locale.ROOT)
					+ "/gis_objects/";
			mapboxMapHolder = new MapboxMapHolder(mapView, gisObjectsBaseUrl);
			mapboxMapHolder.setOnWalkNavigationStateChanged(nav -> {
				syncWalkNavigationToSession(nav);
				return kotlin.Unit.INSTANCE;
			});
			setupMapNotePlacementListener();
			ViewGroup trakterContainer = view.findViewById(R.id.trakter_card_container);
			if (trakterContainer != null) {
				mapboxMapHolder.setTrakterCardContainer(trakterContainer);
			}
			if (wf != null && wf.getMyPageDefineBlock() != null) {
				String gisMode = wf.getMyPageDefineBlock().getGisMode();
				mapboxMapHolder.setGisMode(gisMode);
				Log.d(TAG, "MapTemplate: set gisMode=" + gisMode + " from PageDefineBlock");
			} else {
				Log.d(TAG, "MapTemplate: no PageDefineBlock or wf null, gisMode stays default");
			}

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
		// Always wire up team overlay when we have a GisMapView; initial visibility is controlled by GisMapView.isTeamVisible().
		boolean teamEnabled = pendingGisMapViewConfig != null && mapboxMapHolder != null;
		Log.d(TAG, "onViewCreated: pendingConfig=" + (pendingGisMapViewConfig != null)
				+ " mapboxMapHolder=" + (mapboxMapHolder != null) + " -> teamLayerEnabled=" + teamEnabled);

		if (teamEnabled) {
			// Apply initial visibility (checked/unchecked) for Team layer based on GisMapView.teamVisible.
			mapboxMapHolder.setInitialTeamLayerVisible(pendingGisMapViewConfig.isTeamVisible());
			Log.d(TAG, "Setting up team layer: observing TeamStatusViewModel");
			teamStatusViewModel = new ViewModelProvider(requireActivity()).get(TeamStatusViewModel.class);
			mapboxMapHolder.setTeamStatusViewModel(teamStatusViewModel);
			teamStatusViewModel.sendAndReceiveTeamPositions(); // trigger initial fetch
			// Apply current value immediately (LiveData may already have 4 members from MenuActivity polling)
			applyTeamMembersToMap(teamStatusViewModel.teamMemberGisObjects.getValue());
			teamStatusViewModel.teamMemberGisObjects.observe(getViewLifecycleOwner(), this::applyTeamMembersToMap);
			// Periodic refresh: full sync every 10s, "me" position every 1s
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
			meUpdateRunnable = new Runnable() {
				@Override
				public void run() {
					if (teamStatusViewModel != null) {
						teamStatusViewModel.sendMyPositionOnly();
						teamUpdateHandler.postDelayed(this, TimeUnit.SECONDS.toMillis(1));
					}
				}
			};
			teamUpdateHandler.postDelayed(meUpdateRunnable, TimeUnit.SECONDS.toMillis(1));
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

	/** Converts team member GisPointObjects to map points and updates the team layer. Called from observer and onViewCreated. */
	private void applyTeamMembersToMap(Set<GisPointObject> teamMembers) {
		if (teamMembers == null || mapboxMapHolder == null) return;
		List<TeamMemberMapPoint> points = new ArrayList<>();
		int index = 0;
		for (GisPointObject gop : teamMembers) {
			com.teraim.fieldapp.dynamic.types.Location loc = gop.getLocation();
			if (loc == null) {
				continue;
			}
			LatLong latLong;
			if (loc instanceof LatLong) {
				latLong = (LatLong) loc;
			} else if (loc instanceof SweLocation swe) {
                latLong = Geomatte.convertToLatLong(swe.getY(), swe.getX());
			} else {
				continue;
			}
			String uuid = gop.getKeyHash() != null && gop.getKeyHash().containsKey("uuid") ? gop.getKeyHash().get("uuid") : null;
			String currentUserUuid = GlobalState.getInstance().getUserUUID();
			// Use unique id: "me" for current user (by UUID) to avoid collision with another user with same name
			String id;
			if (uuid != null && currentUserUuid != null && uuid.trim().equalsIgnoreCase(currentUserUuid.trim())) {
				id = "me";
			} else {
				id = gop.getKeyHash() != null && gop.getKeyHash().containsKey("author")
						? gop.getKeyHash().get("author") : (uuid != null ? uuid : "team_" + index);
			}
			android.graphics.Bitmap iconBitmap = gop.getIcon();
			if (iconBitmap == null) {
				android.content.Context ctx = (view != null) ? view.getContext() : mapboxMapHolder.getWidget().getContext();
				android.graphics.drawable.Drawable d = ContextCompat.getDrawable(ctx, R.drawable.ic_needle_symbol);
				if (d != null) iconBitmap = Tools.drawableToBitmap(d);
			}
			long timestamp = 0;
			if (gop.getKeyHash() != null && gop.getKeyHash().containsKey("timestamp")) {
				try {
					timestamp = Long.parseLong(gop.getKeyHash().get("timestamp"));
				} catch (NumberFormatException ignored) { }
			}
			points.add(new TeamMemberMapPoint(id, latLong.getX(), latLong.getY(),
					gop.getLabel(), iconBitmap, timestamp, uuid));
			index++;
		}
		String currentUserUuid = GlobalState.getInstance().getUserUUID();
		int meCount = 0;
		for (TeamMemberMapPoint p : points) {
			if (p.uuid != null && currentUserUuid != null && p.uuid.trim().equalsIgnoreCase(currentUserUuid.trim())) meCount++;
		}
		if (meCount != 1) {
			Log.w(TAG, "DUPLICATE_ME? points matching current user UUID: " + meCount + " (expected 1)");
		}
		lastTeamMemberPoints = points;
		String batchId = points.size() + "_" + (points.isEmpty() ? "empty" : points.get(0).name);
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
			MapSessionViewModel session = getMapSessionViewModel();
			// Style: restore user toggle after rotation, else GisMapView / default satellite
			String styleUri = Style.SATELLITE_STREETS;
			if (session.hasSavedCamera) {
				mapTypeSatellite = session.mapTypeSatellite;
				styleUri = mapTypeSatellite ? Style.SATELLITE_STREETS : Style.MAPBOX_STREETS;
			} else if (pendingGisMapViewConfig != null && "standard".equalsIgnoreCase(pendingGisMapViewConfig.getMapType())) {
				styleUri = Style.MAPBOX_STREETS;
				mapTypeSatellite = false;
			} else {
				mapTypeSatellite = true;
			}
			Log.d(TAG, "Loading Mapbox style: " + styleUri);
			mapboxMap.loadStyleUri(styleUri, new com.mapbox.maps.Style.OnStyleLoaded() {
				@Override
				public void onStyleLoaded(com.mapbox.maps.Style style) {
					mapReady = true;
					if (mapboxMapHolder != null) {
						mapboxMapHolder.setMapboxMap(mapboxMap);
						if (pendingGisMapViewConfig != null) {
							String onClick = pendingGisMapViewConfig.getOnCenterClick();
							mapboxMapHolder.setOnCenterClickWorkflow(onClick);
							Log.d(TAG, "Style loaded: set onCenterClickWorkflow=" + onClick);
						}
						// Re-apply team layer if we had data before the map was ready
						if (pendingGisMapViewConfig != null
								&& lastTeamMemberPoints != null && !lastTeamMemberPoints.isEmpty()) {
							mapboxMapHolder.updateTeamLayer(lastTeamMemberPoints, null);
						}
					}
					Log.d(TAG, "Mapbox style loaded successfully");
					MapSessionViewModel session = getMapSessionViewModel();
					if (session.hasSavedCamera) {
						restoreSavedCamera(session);
						Log.d(TAG, "Restored saved map camera after view recreation");
					} else {
					// Pending map camera (GeoJSON feature open preserves user zoom; TRAKT center-on uses default zoom)
					GlobalState.PendingMapCamera pending = GlobalState.getInstance().getAndClearPendingMapCamera();
					if (pending != null) {
						double lat = pending.lat;
						double lng = pending.lng;
						// If no explicit zoom was requested with pending center, honor block_add_gis_map_view zoom.
						double zoom = pending.zoom != null
								? pending.zoom
								: (pendingGisMapViewConfig != null ? pendingGisMapViewConfig.getZoom() : 10.5);
						Log.d(TAG, "Map centering on pending (WGS84): lat=" + lat + ", lng=" + lng + ", zoom=" + zoom);
						Point initialPoint = Point.fromLngLat(lng, lat);
						mapboxMap.setCamera(new CameraOptions.Builder().center(initialPoint).zoom(zoom).build());
					} else if (pendingGisMapViewConfig != null) {
						applyCameraFromGisMapViewConfig();
					} else {
						double lat = 59.3293;
						double lng = 18.0686;
						double zoom = 8.0;
						Log.d(TAG, "Map using default camera: Stockholm");
						Point initialPoint = Point.fromLngLat(lng, lat);
						mapboxMap.setCamera(new CameraOptions.Builder().center(initialPoint).zoom(zoom).build());
					}
					}
					restoreWalkNavigationIfAny(session);
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
		if (mapboxMapHolder != null && pendingGisMapViewConfig != null) {
			String onClick = pendingGisMapViewConfig.getOnCenterClick();
			mapboxMapHolder.setOnCenterClickWorkflow(onClick);
			Log.d(TAG, "Set onCenterClickWorkflow=" + onClick + " for map " + config.getName());
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

	/** Exposes current GisMapView config so map realizations can honor its settings (e.g. team visibility). */
	public GisMapView getPendingGisMapViewConfig() {
		return pendingGisMapViewConfig;
	}

	private void setupFabMenu(View rootView) {
		if (fabTools == null || fabMenuContainer == null) return;
		View[] subFabs = {
			rootView.findViewById(R.id.fab_zoom_plus),
			rootView.findViewById(R.id.fab_zoom_minus),
			rootView.findViewById(R.id.fab_refresh),
			rootView.findViewById(R.id.fab_layer_toggle),
			rootView.findViewById(R.id.fab_center_on_user),
			rootView.findViewById(R.id.fab_map_type_toggle),
			rootView.findViewById(R.id.fab_add_object)
		};
		fabTools.setOnClickListener(v -> {
			fabMenuExpanded = !fabMenuExpanded;
			animateFabMenu(subFabs, fabMenuExpanded);
		});
		FloatingActionButton addObjectFab = rootView.findViewById(R.id.fab_add_object);
		if (addObjectFab != null) {
			addObjectFab.setOnClickListener(v -> showAddObjectTypeDialog());
		}
	}

	private void showAddObjectTypeDialog() {
		if (getContext() == null || mapboxMapHolder == null) return;
		// Object types for "map notes" layer (gistyp = value stored in DB, label = display name)
		final String[][] types = {
			{ "map_note", "Note" },
			{ "map_parking", "Parking" },
			{ "map_poi", "Point of interest" }
		};
		String[] labels = new String[types.length];
		for (int i = 0; i < types.length; i++) labels[i] = types[i][1];
		new AlertDialog.Builder(requireContext())
			.setTitle(getString(R.string.select_object_type))
			.setItems(labels, (dialog, which) -> {
				String gistyp = types[which][0];
				String label = types[which][1];
				mapboxMapHolder.startAddObjectMode(gistyp, label);
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
	}

	private void setupMapNotePlacementListener() {
		if (mapboxMapHolder == null) return;
		mapboxMapHolder.setOnMapNotePlacementListener(new com.teraim.fieldapp.dynamic.workflow_realizations.gis.MapboxMapHolder.OnMapNotePlacementListener() {
			@Override
			public void onMapNotePlaced(com.mapbox.geojson.Point point, String gistyp, String label) {
				java.util.Map<String, String> keyHash = new java.util.HashMap<>();
				keyHash.put("uid", java.util.UUID.randomUUID().toString());
				keyHash.put("gistyp", gistyp);
				String gpsCoordValue = point.latitude() + "," + point.longitude();
				com.teraim.fieldapp.GlobalState.getInstance().getDb().insertMapNotePoint(keyHash, gpsCoordValue);
				if (getMapboxMapHolder() != null) getMapboxMapHolder().refreshMapNoteLayer();
				android.widget.Toast.makeText(getContext(), getString(R.string.ok), android.widget.Toast.LENGTH_SHORT).show();
			}
			@Override
			public void onMapNotePlacementCancelled() {
				// no-op
			}
		});
	}

	private void animateFabMenu(View[] subFabs, boolean expand) {
		int duration = 200;
		boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
		float slidePx = 16f;
		for (int i = 0; i < subFabs.length; i++) {
			View f = subFabs[i];
			if (f == null) continue;
			if (expand) {
				f.setVisibility(View.VISIBLE);
				f.setAlpha(0f);
				if (landscape) {
					f.setTranslationX(slidePx);
					f.setTranslationY(0f);
				} else {
					f.setTranslationY(slidePx);
					f.setTranslationX(0f);
				}
				AnimatorSet set = new AnimatorSet();
				if (landscape) {
					set.playTogether(
						ObjectAnimator.ofFloat(f, View.ALPHA, 1f),
						ObjectAnimator.ofFloat(f, View.TRANSLATION_X, 0f)
					);
				} else {
					set.playTogether(
						ObjectAnimator.ofFloat(f, View.ALPHA, 1f),
						ObjectAnimator.ofFloat(f, View.TRANSLATION_Y, 0f)
					);
				}
				set.setDuration(duration);
				set.setStartDelay(i * 40L);
				set.start();
			} else {
				AnimatorSet set = new AnimatorSet();
				if (landscape) {
					set.playTogether(
						ObjectAnimator.ofFloat(f, View.ALPHA, 0f),
						ObjectAnimator.ofFloat(f, View.TRANSLATION_X, slidePx)
					);
				} else {
					set.playTogether(
						ObjectAnimator.ofFloat(f, View.ALPHA, 0f),
						ObjectAnimator.ofFloat(f, View.TRANSLATION_Y, slidePx)
					);
				}
				set.setDuration(duration);
				set.setStartDelay((subFabs.length - 1 - i) * 40L);
				set.addListener(new android.animation.AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(android.animation.Animator animation) {
						f.setVisibility(View.GONE);
						f.setAlpha(1f);
						f.setTranslationX(0f);
						f.setTranslationY(0f);
					}
				});
				set.start();
			}
		}
	}

	private void setupCenterOnUserButton(View rootView) {
		FloatingActionButton centerBtn = rootView.findViewById(R.id.fab_center_on_user);
		if (centerBtn == null) return;
		centerBtn.setOnClickListener(v -> centerMapOnUser());
	}

	private void setupMapTypeToggleButton(View rootView) {
		FloatingActionButton toggleBtn = rootView.findViewById(R.id.fab_map_type_toggle);
		if (toggleBtn == null) return;
		toggleBtn.setOnClickListener(v -> toggleMapType());
	}

	private void toggleMapType() {
		if (mapboxMap == null || !mapReady) return;
		mapTypeSatellite = !mapTypeSatellite;
		String styleUri = mapTypeSatellite ? Style.SATELLITE_STREETS : Style.MAPBOX_STREETS;
		Log.d(TAG, "Toggling map type to: " + (mapTypeSatellite ? "satellite" : "standard"));
		// Preserve current camera before style change
		com.mapbox.maps.CameraState state = mapboxMap.getCameraState();
		Point center = state.getCenter();
		double zoom = state.getZoom();
		mapboxMap.loadStyleUri(styleUri, new com.mapbox.maps.Style.OnStyleLoaded() {
			@Override
			public void onStyleLoaded(com.mapbox.maps.Style style) {
				if (mapboxMapHolder != null) {
					mapboxMapHolder.setMapboxMap(mapboxMap);
					if (pendingGisMapViewConfig != null) {
						mapboxMapHolder.setOnCenterClickWorkflow(pendingGisMapViewConfig.getOnCenterClick());
					}
					if (lastTeamMemberPoints != null && !lastTeamMemberPoints.isEmpty()) {
						mapboxMapHolder.updateTeamLayer(lastTeamMemberPoints, null);
					}
					mapboxMapHolder.refreshLayers(null);
					restoreWalkNavigationIfAny(getMapSessionViewModel());
				}
				mapboxMap.setCamera(new CameraOptions.Builder().center(center).zoom(zoom).build());
				Log.d(TAG, "Map type toggled, camera restored");
			}
		});
	}

	private void centerMapOnUser() {
		if (mapboxMap == null) return;
		String currentUserUuid = GlobalState.getInstance().getUserUUID();
		double lat = Double.NaN, lng = Double.NaN;
		// Try lastTeamMemberPoints first (contains "me" identified by UUID)
		if (lastTeamMemberPoints != null) {
			for (TeamMemberMapPoint p : lastTeamMemberPoints) {
				if (p.uuid != null && currentUserUuid != null && p.uuid.trim().equalsIgnoreCase(currentUserUuid.trim())) {
					lat = p.lat;
					lng = p.lng;
					break;
				}
			}
		}
		// Fallback: get from teamStatusViewModel
		if (Double.isNaN(lat) && teamStatusViewModel != null) {
			Set<GisPointObject> team = teamStatusViewModel.teamMemberGisObjects.getValue();
			if (team != null) {
				for (GisPointObject g : team) {
					if (g.isUser()) {
						com.teraim.fieldapp.dynamic.types.Location loc = g.getLocation();
						if (loc instanceof LatLong ll) {
                            lat = ll.getX();
							lng = ll.getY();
							break;
						} else if (loc instanceof SweLocation) {
							LatLong ll = Geomatte.convertToLatLong(loc.getY(), loc.getX());
							lat = ll.getX();
							lng = ll.getY();
							break;
						}
					}
				}
			}
		}
		if (Double.isNaN(lat) || Double.isNaN(lng)) {
			Log.w(TAG, "Center on user: no user position available");
			if (getContext() != null) {
				new AlertDialog.Builder(requireContext())
						.setMessage(R.string.no_user_position_available)
						.setPositiveButton(android.R.string.ok, null)
						.show();
			}
			return;
		}
		com.mapbox.maps.CameraState state = mapboxMap.getCameraState();
		double zoom = state.getZoom();
		Point center = Point.fromLngLat(lng, lat);
		mapboxMap.setCamera(new CameraOptions.Builder().center(center).zoom(zoom).build());
		Log.d(TAG, "Centered map on user: lat=" + lat + ", lng=" + lng);
	}

	private void setupLayerToggleFab() {
		if (fabLayerToggle == null || mapboxMapHolder == null) return;
		fabLayerToggle.setOnClickListener(v -> showLayerSelectionDialog());
	}

	private void setupRefreshButton(View rootView) {
		FloatingActionButton refreshB = rootView.findViewById(R.id.fab_refresh);
		if (refreshB == null) return;
		if (teamStatusViewModel == null) {
			teamStatusViewModel = new ViewModelProvider(requireActivity()).get(TeamStatusViewModel.class);
		}
		Animation wiggleAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.refresh_wiggle);
		// Trigger server status check when map is shown (so refresh button reflects current state)
		teamStatusViewModel.sendAndReceiveTeamPositions();
		teamStatusViewModel.serverPendingUpdate.observe(getViewLifecycleOwner(), hasNewVersion -> {
			if (hasNewVersion != null && hasNewVersion) {
				refreshB.setImageResource(R.drawable.ic_refresh_alert);
				refreshB.startAnimation(wiggleAnimation);
			} else {
				refreshB.setImageResource(R.drawable.ic_refresh_black);
				refreshB.clearAnimation();
			}
		});
		refreshB.setOnClickListener(v -> {
			Log.d(TAG, "Refresh map layers clicked");
			refreshB.clearAnimation();
			refreshB.setImageResource(R.drawable.ic_refresh_black);
			refreshB.setClickable(false);
			if (mapboxMapHolder != null) {
				mapboxMapHolder.refreshLayers(() -> {
					if (getActivity() != null) {
						refreshB.setClickable(true);
						if (teamStatusViewModel != null) {
							teamStatusViewModel.acknowledgeConfigUpdate();
						}
						Log.d(TAG, "Map layers refreshed");
					}
					return Unit.INSTANCE;
				});
			} else {
				refreshB.setClickable(true);
			}
		});
	}

	private void setupZoomButtons(View rootView) {
		FloatingActionButton zoomPlus = rootView.findViewById(R.id.fab_zoom_plus);
		FloatingActionButton zoomMinus = rootView.findViewById(R.id.fab_zoom_minus);
		if (zoomPlus == null || zoomMinus == null) return;
		zoomPlus.setOnClickListener(v -> adjustZoom(1));
		zoomMinus.setOnClickListener(v -> adjustZoom(-1));
	}

	private void adjustZoom(int delta) {
		if (mapboxMap == null) return;
		com.mapbox.maps.CameraState state = mapboxMap.getCameraState();
		double currentZoom = state.getZoom();
		double newZoom = Math.max(1.0, Math.min(22.0, currentZoom + delta));
		Point center = state.getCenter();
		CameraOptions options = new CameraOptions.Builder()
				.center(center)
				.zoom(newZoom)
				.build();
		mapboxMap.setCamera(options);
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
		int screenHeightPx = requireContext().getResources().getDisplayMetrics().heightPixels;
		// Keep the whole popup more compact.
		int dialogHeightPx = (int) (screenHeightPx / 2f);
		int maxListHeightPx = dialogHeightPx;

		ScrollView scrollView = new ScrollView(requireContext());
		LinearLayout container = new LinearLayout(requireContext());
		container.setOrientation(LinearLayout.VERTICAL);
		container.setPadding(50, 40, 50, 40);

		// Indices of layers that participate in the "toggle all" logic (exclude Team).
		final ArrayList<Integer> nonTeamLayerIndices = new ArrayList<>();
		for (int i = 0; i < layerNames.size(); i++) {
			String name = layerNames.get(i);
			if (!MapboxMapHolder.TEAM_LAYER_DISPLAY_NAME.equals(name)) {
				nonTeamLayerIndices.add(i);
			}
		}

		// "Toggle all" header checkbox (only for non-team layers).
		CheckBox cbAll = new CheckBox(requireContext());
		cbAll.setText(R.string.toggle_all_layers);
		container.addView(cbAll);

		List<CheckBox> layerCbs = new ArrayList<>();
		final boolean[] suppressCallbacks = new boolean[] { false };

		// Create per-layer checkboxes.
		for (String name : layerNames) {
			CheckBox cb = new CheckBox(requireContext());
			cb.setText(mapboxMapHolder.getLayerDisplayName(name));
			cb.setChecked(mapboxMapHolder.isLayerVisible(name));
			cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
				if (suppressCallbacks[0]) return;
				mapboxMapHolder.setLayerVisibility(name, isChecked);
				boolean allChecked = true;
				for (Integer idx : nonTeamLayerIndices) {
					if (idx >= 0 && idx < layerCbs.size()) {
						CheckBox existing = layerCbs.get(idx);
						if (!existing.isChecked()) {
							allChecked = false;
							break;
						}
					}
				}
				if (nonTeamLayerIndices.isEmpty()) {
					allChecked = false;
				}
				suppressCallbacks[0] = true;
				cbAll.setChecked(allChecked);
				suppressCallbacks[0] = false;
			});
			layerCbs.add(cb);
			container.addView(cb);
		}

		// Initialize "toggle all" from current state.
		boolean allChecked = true;
		for (Integer idx : nonTeamLayerIndices) {
			if (idx >= 0 && idx < layerCbs.size()) {
				if (!layerCbs.get(idx).isChecked()) {
					allChecked = false;
					break;
				}
			}
		}
		cbAll.setChecked(!nonTeamLayerIndices.isEmpty() && allChecked);

		cbAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
			if (suppressCallbacks[0]) return;
			suppressCallbacks[0] = true;
			for (Integer idx : nonTeamLayerIndices) {
				if (idx < 0 || idx >= layerNames.size() || idx >= layerCbs.size()) continue;
				String name = layerNames.get(idx);
				CheckBox cb = layerCbs.get(idx);
				cb.setChecked(isChecked);
				mapboxMapHolder.setLayerVisibility(name, isChecked);
			}
			suppressCallbacks[0] = false;
		});

		scrollView.addView(container, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		scrollView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, maxListHeightPx));

		AlertDialog dialog = new AlertDialog.Builder(requireContext())
				.setTitle(R.string.select_layers_title)
				.setView(scrollView)
				.setPositiveButton(android.R.string.ok, null)
				.show();
		if (dialog != null && dialog.getWindow() != null) {
			dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, dialogHeightPx);
		}
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
	public void onStop() {
		if (mapView != null) {
			mapView.onStop();
		}
		super.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		persistMapSessionState();
		super.onSaveInstanceState(outState);
	}

	private MapSessionViewModel getMapSessionViewModel() {
		if (mapSessionViewModel == null) {
			mapSessionViewModel = new ViewModelProvider(requireActivity()).get(MapSessionViewModel.class);
		}
		return mapSessionViewModel;
	}

	private void syncWalkNavigationToSession(MapboxMapHolder.WalkNavigationState nav) {
		MapSessionViewModel session = getMapSessionViewModel();
		if (nav == null) {
			session.clearWalkNavigation();
			return;
		}
		session.walkNavActive = true;
		session.walkNavTargetLat = nav.getTargetLat();
		session.walkNavTargetLng = nav.getTargetLng();
		if (nav.getStartLat() != null && nav.getStartLng() != null) {
			session.hasWalkNavStart = true;
			session.walkNavStartLat = nav.getStartLat();
			session.walkNavStartLng = nav.getStartLng();
		} else {
			session.hasWalkNavStart = false;
		}
		session.walkNavTargetLabel = nav.getTargetLabel();
		session.walkNavStartActionType = nav.getStartActionType();
		session.walkNavFeatureJson = nav.getFeatureJson();
		session.walkNavObjContext = nav.getObjContext();
		session.walkNavOnClick = nav.getOnClick();
		session.walkNavTraktName = nav.getTraktName();
	}

	private void persistMapSessionState() {
		if (mapboxMap != null) {
			CameraState cam = mapboxMap.getCameraState();
			MapSessionViewModel session = getMapSessionViewModel();
			session.hasSavedCamera = true;
			session.savedCenterLat = cam.getCenter().latitude();
			session.savedCenterLng = cam.getCenter().longitude();
			session.savedZoom = cam.getZoom();
			session.savedPitch = cam.getPitch();
			session.savedBearing = cam.getBearing();
			session.mapTypeSatellite = mapTypeSatellite;
		}
		if (mapboxMapHolder != null) {
			syncWalkNavigationToSession(mapboxMapHolder.getWalkNavigationState());
		}
	}

	private void restoreSavedCamera(MapSessionViewModel session) {
		if (mapboxMap == null || !session.hasSavedCamera) return;
		mapTypeSatellite = session.mapTypeSatellite;
		Point center = Point.fromLngLat(session.savedCenterLng, session.savedCenterLat);
		mapboxMap.setCamera(new CameraOptions.Builder()
				.center(center)
				.zoom(session.savedZoom)
				.pitch(session.savedPitch)
				.bearing(session.savedBearing)
				.build());
	}

	private void restoreWalkNavigationIfAny(MapSessionViewModel session) {
		if (!session.walkNavActive || mapboxMapHolder == null) return;
		Double startLat = session.hasWalkNavStart ? session.walkNavStartLat : null;
		Double startLng = session.hasWalkNavStart ? session.walkNavStartLng : null;
		MapboxMapHolder.WalkNavigationState state = new MapboxMapHolder.WalkNavigationState(
				session.walkNavTargetLat,
				session.walkNavTargetLng,
				startLat,
				startLng,
				session.walkNavTargetLabel,
				session.walkNavStartActionType,
				session.walkNavFeatureJson,
				session.walkNavObjContext,
				session.walkNavOnClick,
				session.walkNavTraktName
		);
		mapboxMapHolder.restoreWalkNavigation(state);
		Log.d(TAG, "Restored walk navigation after view recreation");
	}

	@Override
	public void onResume() {
		super.onResume();
		android.app.Activity activity = getActivity();
		if (activity instanceof com.teraim.fieldapp.Start) {
			((com.teraim.fieldapp.Start) activity).setToolbarTransparent(true);
			activity.setTitle("");
		}
		if (mapView != null) {
			// After transparent toolbar + layout: compass margin = measured action bar height.
			mapView.post(() -> {
				if (mapView != null) {
					MapboxOrnamentsKt.updateFieldAppCompassPlacement(mapView);
				}
			});
		}
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
		persistMapSessionState();
		if (teamUpdateRunnable != null) {
			teamUpdateHandler.removeCallbacks(teamUpdateRunnable);
			teamUpdateRunnable = null;
		}
		if (meUpdateRunnable != null) {
			teamUpdateHandler.removeCallbacks(meUpdateRunnable);
			meUpdateRunnable = null;
		}
		if (view != null) {
			View refreshB = view.findViewById(R.id.fab_refresh);
			if (refreshB != null) refreshB.clearAnimation();
		}
		super.onDestroyView();
		if (mapboxMapHolder != null) {
			mapboxMapHolder.release();
		}
		if (mapView != null) {
			mapView.onDestroy();
			mapView = null;
			mapboxMap = null;
			mapReady = false;
		}
		// mapboxMapHolder and layers are recreated in onCreateView; session state is in MapSessionViewModel.
		view = null;
		mapboxMapHolder = null;
	}

}
