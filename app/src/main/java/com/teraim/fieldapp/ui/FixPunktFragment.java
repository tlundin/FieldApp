package com.teraim.fieldapp.ui;

import android.app.Fragment;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.gesture.Prediction;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.Marker;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.non_generics.NamedVariables;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class FixPunktFragment extends Fragment implements OnGesturePerformedListener {

	
	
	private final Set<FixPunkt>fixPunkter=new HashSet<FixPunkt>();
    private GestureLibrary gestureLib;
	private List<Marker> markers;
	
	private class FixPunkt {
		FixPunkt(Variable avst, Variable rikt) {
			this.avst=avst;
			this.rikt=rikt;
		}
		final Variable avst;
		final Variable rikt;
	}
	
	private final int[] png = new int[] {R.drawable.fixpunkt,R.drawable.fixpunkt,R.drawable.fixpunkt,R.drawable.fixpunkt};
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
        GlobalState gs = GlobalState.getInstance();
		VariableCache varCache = gs.getVariableCache();
		VariableConfiguration al = gs.getVariableConfiguration();
		Log.d("nils","in onCreateView of fixpunkt_fragment");
		View v = inflater.inflate(R.layout.template_fixpunkt_right, container, false);	
		
	
		final FrameLayout fl = v.findViewById(R.id.circle);
		
		final Button backB = v.findViewById(R.id.createBackB);
		backB.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				//GlobalState.getInstance().getCurrentWorkflowContext().resetState();
				getFragmentManager().popBackStackImmediate();
			}
		});
		FixytaView fyv = new FixytaView(getActivity(),null);		
		
		//Create markers
		Bitmap bm;
		Bitmap scaled;		
		int h = 48; // height in pixels
		int w = 48; // width in pixels  
		
		markers = new ArrayList<Marker>();
		
		for(int i=0;i<4;i++) {
			bm = BitmapFactory.decodeResource(getResources(), png[i]);
			scaled = Bitmap.createScaledBitmap(bm, h, w, true);
			markers.add(new Marker(scaled));
			Variable avst,rikt;
			String avstKey,riktKey;
			avstKey = NamedVariables.FIXPUNKT_VARS[i*2];
			riktKey = NamedVariables.FIXPUNKT_VARS[i*2+1];
			avst = varCache.getVariable(avstKey);
			rikt = varCache.getVariable(riktKey);
			if (avst!=null && rikt !=null) 
				fixPunkter.add(new FixPunkt(avst,rikt));			
			
		}
		
		//Add profil if it exists.
		Variable tmp;
		tmp = varCache.getVariable(NamedVariables.ProfilAvst);
		String avst;
		if ((avst = tmp.getHistoricalValue())!=null) {
			tmp = varCache.getVariable(NamedVariables.ProfilRikt);
			String rikt;
			if ((rikt = tmp.getHistoricalValue())!=null) {
				bm = BitmapFactory.decodeResource(getResources(), R.drawable.profil);
				scaled = Bitmap.createScaledBitmap(bm, 64, 64, true);
				Marker m = new Marker(scaled);
				m.setValue(avst, rikt,true);
				markers.add(m);
			}
				
		}
		
		Log.d("nils","Markers has "+markers.size()+" elements");
		fyv.setFixedMarkers(markers);
		fl.addView(fyv);
		
		
	    GestureOverlayView gestureOverlayView = v.findViewById(R.id.gesture_overlay);
	    gestureOverlayView.setGestureVisible(false);
	    gestureOverlayView.addOnGesturePerformedListener(this);
	    gestureLib = GestureLibraries.fromRawResource(this.getActivity(), R.raw.gestures);
	    if (!gestureLib.load()) {      	
	    	        Log.i("nils", "Load gesture libraries failed.");  
	    	    }  
	
	    
		
		return v;
	}
	/* (non-Javadoc)
	 * @see android.app.Fragment#onStart()
	 */
	@Override
	public void onStart() {
		int itS=0;
		if (fixPunkter.size()>markers.size()) {
			Log.e("nils", "number of fixpunkter larger than number of markers!! "+markers.size());
			itS = markers.size();
		} else
			itS = fixPunkter.size();
		Iterator<FixPunkt> it = fixPunkter.iterator();
		FixPunkt f;
		for (int j=0;j<itS;j++) {
			 f = it.next();
			markers.get(j).setValue(f.avst.getValue(),f.rikt.getValue(),false);	
		}
		super.onStart();
	}
	
	
	@Override
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
	    Log.d("nils","Number of gestures available: "+gestureLib.getGestureEntries().size());
	    ArrayList<Prediction> predictions = gestureLib.recognize(gesture);
	    Log.d("nils","Number of predictions: "+predictions.size());
	    for (Prediction prediction : predictions) {
	      if (prediction.score > .5) {
	  		if (prediction.name.equals("right")) {
	  			//GlobalState.getInstance().getCurrentWorkflowContext().resetState();
	  			getFragmentManager().popBackStackImmediate();

	  		} else 
				Toast.makeText(getActivity(), "vänster till höger", Toast.LENGTH_SHORT).show();
	  			
	      }
	    }		
	}
	
}
