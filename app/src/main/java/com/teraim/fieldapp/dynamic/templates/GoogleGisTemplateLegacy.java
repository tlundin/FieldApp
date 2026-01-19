package com.teraim.fieldapp.dynamic.templates;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location; // Import Location class

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.teraim.fieldapp.R; // Assuming R is your app's resource class
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.types.LatLong;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;
import com.teraim.fieldapp.utils.Geomatte; // Import Geomatte for coordinate conversion

import java.util.ArrayList;
import java.util.List;

public class GoogleGisTemplateLegacy extends Executor implements OnMapReadyCallback {

    private static final String TAG = "GoogleGisTemplateLegacy";
    private MapView mapView;
    private GoogleMap googleMap;
    private LinearLayout rootContainer; // This will be our root container view
    private boolean isUserInteractingWithMap = false; // Flag to track user map interaction

    @Override
    protected List<WF_Container> getContainers() {
        ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
        ret.add(new WF_Container("root",rootContainer,null));
        return ret;
    }

    @Override
    public boolean execute(String function, String target) {
        // This method would handle specific workflow functions.
        // For this basic map, we don't have custom functions yet.
        Log.d(TAG, "Execute called with function: " + function + ", target: " + target);
        return true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        // We will create a simple layout with a MapView and a LinearLayout for other UI elements
        // For this example, we'll create the layout programmatically.
        // In a real app, you'd likely inflate from an XML layout file.

        rootContainer = new LinearLayout(getContext());
        rootContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        rootContainer.setOrientation(LinearLayout.VERTICAL);
        rootContainer.setId(View.generateViewId()); // Assign a unique ID

        mapView = new MapView(getContext());
        LinearLayout.LayoutParams mapParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, // Height will be weighted
                1.0f // Take up all available space
        );
        mapView.setLayoutParams(mapParams);
        mapView.setId(View.generateViewId()); // Assign a unique ID

        rootContainer.addView(mapView);

        return rootContainer;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called.");

        // Initialize Maps SDK
        MapsInitializer.initialize(this.getActivity().getApplicationContext());

        // Call run() if workflow exists, similar to EmptyTemplate
        // Ensure your workflow enables GPS tracking (e.g., via a PageDefineBlock)
        // for myX and myY variables to be populated.
        if (wf != null) {
            Log.d(TAG, "Workflow exists, calling run().");
            run();
        } else {
            Log.d(TAG, "Workflow is null in onCreate.");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called.");
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called.");
        if (mapView != null) {
            mapView.onResume();
            // Get the map asynchronously
            mapView.getMapAsync(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called.");
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called.");
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView called.");
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d(TAG, "onLowMemory called.");
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState called.");
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        Log.d(TAG, "Google Map is ready.");

        // Set map type to satellite
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        Log.d(TAG, "Map type set to SATELLITE.");

        // Enable zoom controls for better user experience
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Set up camera move listener to detect user interaction
        googleMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isUserInteractingWithMap = true;
                Log.d(TAG, "User started interacting with map (gesture). Auto-centering disabled.");
            }
        });

        // Attempt to get initial current location from Executor's variables (myX, myY are SweRef coordinates)
        // This is for initial centering only. Subsequent updates will be handled by onLocationChanged.
        if (myX != null && myY != null && myX.getValue() != null && myY.getValue() != null) {
            try {
                double sweX = Double.parseDouble(myX.getValue());
                double sweY = Double.parseDouble(myY.getValue());
                LatLng userLocation = Geomatte.convertToLatLong(sweX,sweY).from();
                float zoomLevel = 8.0f; // Approx. 50km each direction (100km diameter)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, zoomLevel));
                Log.d(TAG, "Initial camera move to user location: " + userLocation.latitude + ", " + userLocation.longitude + " with zoom: " + zoomLevel);
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing initial location coordinates: " + e.getMessage());
                LatLng defaultLocation = new LatLng(59.3293, 18.0686); // Default: Stockholm, Sweden
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 8.0f));
                Log.d(TAG, "Falling back to default location due to parsing error for initial camera.");
            } catch (Exception e) {
                Log.e(TAG, "Error converting SweRef to LatLng or moving camera initially: " + e.getMessage());
                LatLng defaultLocation = new LatLng(59.3293, 18.0686); // Default: Stockholm, Sweden
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 8.0f));
                Log.d(TAG, "Falling back to default location due to conversion/camera error for initial camera.");
            }
        } else {
            Log.d(TAG, "Current location not available from Executor during onMapReady. Moving to default location.");
            LatLng defaultLocation = new LatLng(59.3293, 18.0686); // Default: Stockholm, Sweden
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 8.0f));
        }

        // Enable my location layer (blue dot) if permissions are granted
        if (getContext() != null &&
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            Log.d(TAG, "My Location layer enabled.");
        } else {
            Log.d(TAG, "Location permission not granted or context is null. My Location layer not enabled.");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "onLocationChanged received. User interaction status: " + isUserInteractingWithMap);
        if (googleMap != null && location != null && !isUserInteractingWithMap) {
            LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLng(newLocation));
            Log.d(TAG, "Camera animated to new location: " + newLocation.latitude + ", " + newLocation.longitude);
        } else if (location == null) {
            Log.d(TAG, "Location object is null in onLocationChanged.");
        } else if (googleMap == null) {
            Log.d(TAG, "GoogleMap not ready in onLocationChanged. Cannot move camera.");
        } else {
            Log.d(TAG, "User has interacted with map. Not auto-centering.");
        }
    }
}
