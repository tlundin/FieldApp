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

public class GoogleGisTemplate extends Executor implements OnMapReadyCallback {

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
        Log.d("GoogleGisTemplate", "Execute called with function: " + function + ", target: " + target);
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
        Log.d("GoogleGisTemplate", "onCreate called.");

        // Initialize Maps SDK
        MapsInitializer.initialize(this.getActivity().getApplicationContext());

        // Call run() if workflow exists, similar to EmptyTemplate
        // Ensure your workflow enables GPS tracking (e.g., via a PageDefineBlock)
        // for myX and myY variables to be populated.
        if (wf != null) {
            Log.d("GoogleGisTemplate", "Workflow exists, calling run().");
            run();
        } else {
            Log.d("GoogleGisTemplate", "Workflow is null in onCreate.");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("GoogleGisTemplate", "onStart called.");
        if (mapView != null) {
            mapView.onStart();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("GoogleGisTemplate", "onResume called.");
        if (mapView != null) {
            mapView.onResume();
            // Get the map asynchronously
            mapView.getMapAsync(this);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("GoogleGisTemplate", "onPause called.");
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("GoogleGisTemplate", "onStop called.");
        if (mapView != null) {
            mapView.onStop();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d("GoogleGisTemplate", "onDestroyView called.");
        if (mapView != null) {
            mapView.onDestroy();
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Log.d("GoogleGisTemplate", "onLowMemory called.");
        if (mapView != null) {
            mapView.onLowMemory();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("GoogleGisTemplate", "onSaveInstanceState called.");
        if (mapView != null) {
            mapView.onSaveInstanceState(outState);
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        Log.d("GoogleGisTemplate", "Google Map is ready.");

        // Set map type to satellite
        googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
        Log.d("GoogleGisTemplate", "Map type set to SATELLITE.");

        // Enable zoom controls for better user experience
        googleMap.getUiSettings().setZoomControlsEnabled(true);

        // Set up camera move listener to detect user interaction
        googleMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isUserInteractingWithMap = true;
                Log.d("GoogleGisTemplate", "User started interacting with map (gesture). Auto-centering disabled.");
            }
        });

        // Attempt to get initial current location from Executor's variables (myX, myY are SweRef coordinates)
        // This is for initial centering only. Subsequent updates will be handled by onLocationChanged.
        if (myX != null && myY != null && myX.getValue() != null && myY.getValue() != null) {
            try {
                double sweX = Double.parseDouble(myX.getValue());
                double sweY = Double.parseDouble(myY.getValue());
                ;
                LatLng userLocation = Geomatte.convertToLatLong(sweX,sweY).from();
                float zoomLevel = 8.0f; // Approx. 50km each direction (100km diameter)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, zoomLevel));
                Log.d("GoogleGisTemplate", "Initial camera move to user location: " + userLocation.latitude + ", " + userLocation.longitude + " with zoom: " + zoomLevel);
            } catch (NumberFormatException e) {
                Log.e("GoogleGisTemplate", "Error parsing initial location coordinates: " + e.getMessage());
                LatLng defaultLocation = new LatLng(59.3293, 18.0686); // Default: Stockholm, Sweden
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 8.0f));
                Log.d("GoogleGisTemplate", "Falling back to default location due to parsing error for initial camera.");
            } catch (Exception e) {
                Log.e("GoogleGisTemplate", "Error converting SweRef to LatLng or moving camera initially: " + e.getMessage());
                LatLng defaultLocation = new LatLng(59.3293, 18.0686); // Default: Stockholm, Sweden
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 8.0f));
                Log.d("GoogleGisTemplate", "Falling back to default location due to conversion/camera error for initial camera.");
            }
        } else {
            Log.d("GoogleGisTemplate", "Current location not available from Executor during onMapReady. Moving to default location.");
            LatLng defaultLocation = new LatLng(59.3293, 18.0686); // Default: Stockholm, Sweden
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 8.0f));
        }

        // Enable my location layer (blue dot) if permissions are granted
        if (getContext() != null &&
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            Log.d("GoogleGisTemplate", "My Location layer enabled.");
        } else {
            Log.d("GoogleGisTemplate", "Location permission not granted or context is null. My Location layer not enabled.");
            // You might want to prompt the user for permission here if it's critical for your app.
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        super.onLocationChanged(location); // Call the super method to update myX, myY, myAcc

        Log.d("GoogleGisTemplate", "onLocationChanged received. User interaction status: " + isUserInteractingWithMap);

        // Only move camera if the map is ready and user has not manually interacted with it
        if (googleMap != null && !isUserInteractingWithMap) {
            if (location != null) {
                // Use the new location's latitude and longitude directly for map centering
                LatLng newLocation = new LatLng(location.getLatitude(), location.getLongitude());

                // Zoom level for ~50km in each direction (approx. 100km diameter).
                float zoomLevel = 8.0f; // Keep consistent with initial zoom

                // Animate camera to the new location
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLocation, zoomLevel));
                Log.d("GoogleGisTemplate", "Camera animated to new location: " + newLocation.latitude + ", " + newLocation.longitude);
            } else {
                Log.d("GoogleGisTemplate", "Location object is null in onLocationChanged.");
            }
        } else if (googleMap == null) {
            Log.d("GoogleGisTemplate", "GoogleMap not ready in onLocationChanged. Cannot move camera.");
        } else {
            Log.d("GoogleGisTemplate", "User has interacted with map. Not auto-centering.");
        }
    }
}
