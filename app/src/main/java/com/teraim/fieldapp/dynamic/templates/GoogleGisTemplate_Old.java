//package com.teraim.fieldapp.dynamic.templates;
//
//import android.Manifest;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.location.Location;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.LinearLayout;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.core.content.ContextCompat;
//
//import com.google.android.gms.maps.model.LatLng;
//import com.mapbox.geojson.FeatureCollection;
//import com.mapbox.geojson.Point;
//import com.mapbox.maps.CameraOptions;
//import com.mapbox.maps.EdgeInsets;
//import com.mapbox.maps.MapView;
//import com.mapbox.maps.MapboxMap;
//import com.mapbox.maps.Style;
//import com.mapbox.maps.extension.style.layers.generated.CircleLayer;
//import com.mapbox.maps.extension.style.layers.generated.FillLayer;
//import com.mapbox.maps.extension.style.layers.properties.generated.Visibility;
//import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource;
//import com.mapbox.maps.plugin.Plugin;
//import com.mapbox.maps.plugin.animation.CameraAnimationsPlugin;
//import com.mapbox.maps.plugin.animation.MapAnimationOptions;
//import com.mapbox.maps.plugin.gestures.GesturesPlugin;
//import com.mapbox.maps.plugin.gestures.OnMoveListener;
//import com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin;
//import com.teraim.fieldapp.dynamic.Executor;
//import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
//import com.teraim.fieldapp.utils.Geomatte;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.stream.Collectors;
//
//public class GoogleGisTemplate_Old extends Executor {
//
//    // --- Constants ---
//    private static final String TAG = "GoogleGisTemplate";
//    private static final String GEOJSON_CONTENT_LIST_URL = "http://52.19.13.203/vortex/gis_objects/content.txt";
//    private static final String GEOJSON_BASE_URL = "http://52.19.13.203/vortex/gis_objects_wgs84/";
//    private static final double INITIAL_ZOOM = 8.0;
//    private static final double USER_LOCATION_ZOOM = 14.0;
//
//    // --- UI and Map Components ---
//    private MapView mapView;
//    private MapboxMap mapboxMap;
//    private LinearLayout rootContainer;
//
//    // --- State Management & Listeners ---
//    private boolean isUserInteractingWithMap = false;
//    private ExecutorService networkExecutor;
//    private Handler mainHandler;
//    private OnMoveListener onMoveListener; // Member variable for the listener
//
//    @Override
//    protected List<WF_Container> getContainers() {
//        ArrayList<WF_Container> ret = new ArrayList<>();
//        ret.add(new WF_Container("root", rootContainer, null));
//        return ret;
//    }
//
//    @Override
//    public boolean execute(String function, String target) {
//        Log.d(TAG, "Execute called with function: " + function + ", target: " + target);
//        return true;
//    }
//
//    @Override
//    public void onCreate(@Nullable Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        Log.d(TAG, "onCreate called.");
//
//        // The Mapbox SDK is initialized automatically when the MapView is inflated.
//        // For this to work, you MUST add your access token to the AndroidManifest.xml inside the <application> tag:
//        // <meta-data
//        //     android:name="com.mapbox.maps.AccessToken"
//        //     android:value="YOUR_MAPBOX_ACCESS_TOKEN" />
//
//        networkExecutor = Executors.newFixedThreadPool(5);
//        mainHandler = new Handler(Looper.getMainLooper());
//
//        if (wf != null) {
//            Log.d(TAG, "Workflow exists, calling run().");
//            run();
//        } else {
//            Log.d(TAG, "Workflow is null in onCreate.");
//        }
//    }
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        rootContainer = new LinearLayout(getContext());
//        rootContainer.setLayoutParams(new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT));
//        rootContainer.setOrientation(LinearLayout.VERTICAL);
//        rootContainer.setId(View.generateViewId());
//
//        mapView = new MapView(getContext());
//        mapView.setLayoutParams(new LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT));
//        mapView.setId(View.generateViewId());
//
//        rootContainer.addView(mapView);
//        return rootContainer;
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//        if (mapView != null) {
//            mapboxMap = mapView.getMapboxMap();
//            mapboxMap.loadStyleUri(Style.SATELLITE_STREETS, this::onMapReady);
//        }
//    }
//
//    private void onMapReady(@NonNull Style style) {
//        Log.d(TAG, "Mapbox Style loaded. Initializing map components.");
//        setInitialCameraPosition();
//        enableUserLocation();
//        setupGestureListeners();
//        loadGeoJsonLayers();
//    }
//
//    private void setInitialCameraPosition() {
//        Point initialPoint;
//        if (myX != null && myY != null && myX.getValue() != null && myY.getValue() != null) {
//            try {
//                double sweX = Double.parseDouble(myX.getValue());
//                double sweY = Double.parseDouble(myY.getValue());
//                LatLng userLocation = Geomatte.convertToLatLong(sweX, sweY).from();
//                initialPoint = Point.fromLngLat(userLocation.longitude, userLocation.latitude);
//                Log.d(TAG, "Initial camera position set to user's location: " + initialPoint.coordinates());
//            } catch (Exception e) {
//                Log.e(TAG, "Error parsing or converting initial location. Using default.", e);
//                initialPoint = Point.fromLngLat(18.0686, 59.3293); // Default: Stockholm
//            }
//        } else {
//            Log.d(TAG, "User location not available from Executor. Using default.");
//            initialPoint = Point.fromLngLat(18.0686, 59.3293); // Default: Stockholm
//        }
//
//        mapboxMap.setCamera(new CameraOptions.Builder()
//                .center(initialPoint)
//                .zoom(INITIAL_ZOOM)
//                .build());
//    }
//
//    private void enableUserLocation() {
//        if (getContext() != null && ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//            LocationComponentPlugin locationComponent = mapView.getPlugin(Plugin.MAPBOX_LOCATION_COMPONENT_PLUGIN_ID);
//            if (locationComponent != null) {
//                locationComponent.updateSettings(settings -> {
//                    settings.setEnabled(true);
//                    settings.setPulsingEnabled(true);
//                    return null;
//                });
//                Log.d(TAG, "Mapbox Location Component enabled.");
//            } else {
//                Log.w(TAG, "Mapbox LocationComponentPlugin not found.");
//            }
//        } else {
//            Log.d(TAG, "Location permission not granted. My Location layer not enabled.");
//        }
//    }
//
//    private void setupGestureListeners() {
//        GesturesPlugin gesturesPlugin = mapView.getPlugin(Plugin.MAPBOX_GESTURES_PLUGIN_ID);
//        if (gesturesPlugin != null) {
//            onMoveListener = new OnMoveListener() {
//                @Override
//                public void onMoveBegin(@NonNull com.mapbox.android.gestures.MoveGestureDetector detector) {
//                    if (!isUserInteractingWithMap) {
//                        isUserInteractingWithMap = true;
//                        Log.d(TAG, "User started interacting with map. Auto-centering disabled.");
//                    }
//                }
//                @Override
//                public boolean onMove(@NonNull com.mapbox.android.gestures.MoveGestureDetector detector) { return false; }
//                @Override
//                public void onMoveEnd(@NonNull com.mapbox.android.gestures.MoveGestureDetector detector) {}
//            };
//            gesturesPlugin.addOnMoveListener(onMoveListener);
//        }
//    }
//
//    @Override
//    public void onLocationChanged(Location location) {
//        super.onLocationChanged(location);
//        if (mapboxMap != null && location != null && !isUserInteractingWithMap) {
//            Point newLocation = Point.fromLngLat(location.getLongitude(), location.getLatitude());
//            CameraOptions cameraOptions = new CameraOptions.Builder()
//                    .center(newLocation)
//                    .zoom(USER_LOCATION_ZOOM)
//                    .build();
//            CameraAnimationsPlugin cameraPlugin = mapView.getPlugin(Plugin.MAPBOX_CAMERA_PLUGIN_ID);
//            if (cameraPlugin != null) {
//                cameraPlugin.easeTo(cameraOptions, new MapAnimationOptions.Builder().duration(1500L).build(), null);
//            }
//            Log.d(TAG, "Camera animated to new user location: " + newLocation.coordinates());
//        }
//    }
//
//    private void loadGeoJsonLayers() {
//        Log.d(TAG, "Starting background task to fetch filename list from: " + GEOJSON_CONTENT_LIST_URL);
//        networkExecutor.execute(() -> {
//            try {
//                URL url = new URL(GEOJSON_CONTENT_LIST_URL);
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.setRequestMethod("GET");
//
//                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
//                        List<String> geoJsonFileNames = in.lines()
//                                .map(String::trim)
//                                .filter(name -> !name.isEmpty())
//                                .collect(Collectors.toList());
//                        Log.d(TAG, "Found " + geoJsonFileNames.size() + " GeoJSON layers to load: " + geoJsonFileNames);
//                        for (String fileName : geoJsonFileNames) {
//                            String geoJsonUrl = GEOJSON_BASE_URL + fileName + ".json";
//                            downloadAndAddGeoJsonLayer(geoJsonUrl, fileName);
//                        }
//                    }
//                } else {
//                    Log.e(TAG, "Failed to fetch filename list. HTTP Response: " + conn.getResponseCode());
//                }
//                conn.disconnect();
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching or processing filename list.", e);
//            }
//        });
//    }
//
//    private void downloadAndAddGeoJsonLayer(String geoJsonUrl, String layerName) {
//        networkExecutor.execute(() -> {
//            try {
//                URL url = new URL(geoJsonUrl);
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.setRequestMethod("GET");
//                String geoJsonData = null;
//
//                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
//                    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
//                        geoJsonData = in.lines().collect(Collectors.joining());
//                        Log.d(TAG, "Successfully downloaded " + layerName + ".json.");
//                    }
//                } else {
//                    Log.e(TAG, "Failed to download " + layerName + ".json. HTTP Response: " + conn.getResponseCode());
//                }
//                conn.disconnect();
//
//                if (geoJsonData != null) {
//                    final String finalGeoJsonData = geoJsonData;
//                    mainHandler.post(() -> addLayerToMap(finalGeoJsonData, layerName));
//                }
//            } catch (Exception e) {
//                Log.e(TAG, "Error downloading GeoJSON from " + geoJsonUrl, e);
//            }
//        });
//    }
//
//    private void addLayerToMap(String geoJsonData, String layerName) {
//        if (mapboxMap == null || mapboxMap.getStyle() == null) {
//            Log.w(TAG, "Map or Style is not ready, cannot add layer: " + layerName);
//            return;
//        }
//
//        try {
//            FeatureCollection featureCollection = FeatureCollection.fromJson(geoJsonData);
//            if (featureCollection.features() == null || featureCollection.features().isEmpty()) {
//                Log.w(TAG, "GeoJSON file " + layerName + " contains no features. Skipping.");
//                return;
//            }
//
//            Style style = mapboxMap.getStyle();
//            String sourceId = "source-" + layerName;
//            String fillLayerId = "fill-" + layerName;
//            String pointLayerId = "point-" + layerName;
//
//            // Remove old layers and source if they exist
//            if (style.styleSourceExists(sourceId)) {
//                style.removeStyleSource(sourceId);
//            }
//            if (style.styleLayerExists(fillLayerId)) {
//                style.removeStyleLayer(fillLayerId);
//            }
//            if (style.styleLayerExists(pointLayerId)) {
//                style.removeStyleLayer(pointLayerId);
//            }
//
//            // Create the source object using its builder
//            GeoJsonSource geoJsonSource = new GeoJsonSource.Builder(sourceId)
//                    .featureCollection(featureCollection)
//                    .build();
//
//
//            // Create the fill layer object
//            FillLayer fillLayer = new FillLayer(fillLayerId, sourceId);
//            fillLayer.fillColor(Color.parseColor("#880000FF"));
//            fillLayer.fillOutlineColor(Color.BLUE);
//            fillLayer.visibility(Visibility.VISIBLE);
//
//            // Add the fill layer to the style
//            style.addLayer(fillLayer);
//
//            // Create the circle layer object
//            CircleLayer circleLayer = new CircleLayer(pointLayerId, sourceId);
//            circleLayer.circleColor(Color.YELLOW);
//            circleLayer.circleRadius(8.0);
//            circleLayer.circleStrokeColor(Color.BLACK);
//            circleLayer.circleStrokeWidth(1.5);
//            circleLayer.visibility(Visibility.VISIBLE);
//
//            // Add the circle layer to the style
//            style.addLayer(circleLayer);
//
//            Log.d(TAG, "Added GeoJSON source and layers for: " + layerName);
//            zoomToFeatureCollection(featureCollection);
//
//        } catch (Exception e) {
//            Log.e(TAG, "Error parsing or adding GeoJSON layer: " + layerName, e);
//        }
//    }
//
//    private void zoomToFeatureCollection(FeatureCollection featureCollection) {
//        if (mapboxMap == null) return;
//        EdgeInsets padding = new EdgeInsets(100.0, 100.0, 100.0, 100.0);
//        CameraOptions cameraOptions = mapboxMap.cameraForGeometry(
//                featureCollection, padding, null, null
//        );
//        CameraAnimationsPlugin cameraPlugin = mapView.getPlugin(Plugin.MAPBOX_CAMERA_PLUGIN_ID);
//        if (cameraPlugin != null) {
//            cameraPlugin.easeTo(cameraOptions, new MapAnimationOptions.Builder().duration(1500L).build(), null);
//        }
//        Log.d(TAG, "Zoomed camera to bounds of the new layer.");
//    }
//
//    // --- Lifecycle Methods ---
//
//    @Override
//    public void onStart() {
//        super.onStart();
//        if (mapView != null) mapView.onStart();
//    }
//
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        if (networkExecutor != null) networkExecutor.shutdownNow();
//        if (mapView != null) {
//            GesturesPlugin gesturesPlugin = mapView.getPlugin(Plugin.MAPBOX_GESTURES_PLUGIN_ID);
//            if (gesturesPlugin != null && onMoveListener != null) {
//                gesturesPlugin.removeOnMoveListener(onMoveListener);
//            }
//        }
//        Log.d(TAG, "onDestroyView called, MapView destroyed and Executor shutdown.");
//    }
//}
