package com.teraim.fieldapp.dynamic.templates;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.core.app.ActivityCompat;

import com.google.android.libraries.maps.CameraUpdateFactory;
import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.MapView;
import com.google.android.libraries.maps.OnMapReadyCallback;
import com.google.android.libraries.maps.model.LatLng;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Container;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Terje
 * Activity that runs a workflow that has a user interface.
 * Pressing Back button will return flow to parent workflow.
 */

public class MapTemplate extends Executor implements OnMapReadyCallback {

	private View view;
	private LinearLayout my_root;
	private MapView mView;
	private GoogleMap map;



	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("nils", "In onCreate");

	}




	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		Log.d("nils", "onCreateView method for MapTemplate");
		if (view == null) {

			view = inflater.inflate(R.layout.template_gis_map, container, false);
			// Gets the MapView from the XML layout and creates it
			mView = (MapView) view.findViewById(R.id.myMap);
			mView.onCreate(savedInstanceState);
			my_root = view.findViewById(R.id.myRoot);
			if (myContext != null)
				myContext.addContainers(getContainers());
			else
				Log.e("brexit", "mycontext was null! Couldnt add containers");

			mView.getMapAsync(this);


		}
		return view;
	}


	@Override
	protected List<WF_Container> getContainers() {
		ArrayList<WF_Container> ret = new ArrayList<WF_Container>();
		ret.add(new WF_Container("root", my_root, null));
		return ret;
	}

	@Override
	public boolean execute(String function, String target) {
		return true;
	}


	@Override
	public void onStart() {
		Log.d("nils", "I'm in the onStart method");
		super.onStart();


	}

	public GoogleMap getMap() {
		return map;
	}
	public View getMapView() {
		return mView;
	}

	@Override
	public void onMapReady(GoogleMap gmap) {
		map = gmap;

		if (wf!=null) {
			Log.d("slime","Executing workflow!!");
			run();

		} else
			Log.d("vortex","No workflow found in oncreate default!!!!");

	}

	@Override
	public void onResume() {
		mView.onResume();
		super.onResume();
	}


	@Override
	public void onPause() {
		super.onPause();
		Log.d("mapTemplate", "I'm in the onPause method");
		mView.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mView.onDestroy();
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		mView.onLowMemory();
	}
}
