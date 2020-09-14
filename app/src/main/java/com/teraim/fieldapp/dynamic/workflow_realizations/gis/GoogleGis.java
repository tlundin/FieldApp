package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.View;

import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.ht.zzg;
import com.google.android.libraries.maps.model.BitmapDescriptorFactory;
import com.google.android.libraries.maps.model.Circle;
import com.google.android.libraries.maps.model.CircleOptions;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.Marker;
import com.google.android.libraries.maps.model.MarkerOptions;
import com.google.maps.android.ui.IconGenerator;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.LatLong;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;


public class GoogleGis extends GIS implements EventListener {
    private GoogleMap mMap;
    WF_Context mContext;
    public GoogleGis(String id, View v, GisViewImplementation mMap, boolean isVisible, WF_Context mContext,boolean isTeamVisible) {
        super(id, v, isVisible, mContext,mMap);
        this.mContext = mContext;
        mContext.registerEventListener(this, Event.EventType.onFlowExecuted);
        myLayers = new ArrayList<>();
        if (isTeamVisible) {
            String team = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
            if (team != null && !team.isEmpty()) {
                Log.d("bortex", "team is visible! Adding layer for team " + team);
                final GisLayer teamLayer = new GisLayer("Team", "Team", true, true, true);
                myLayers.add(teamLayer);
            }
        }
        this.mMap=mMap.getGoogleMap();
    }

    private void init() {
        Log.d("google","Layers: ");
        for(GisLayer layer:myLayers) {
            Log.d("google",layer.getLabel());
            Map<String, Set<GisObject>> bags = layer.getGisBags();
            if(bags !=null) {
                for (String bag : bags.keySet()) {
                    Log.d("google", "bag:" + bag);
                    Set<GisObject> gops = bags.get(bag);
                    for (GisObject gop:gops) {
                        Log.d("wf",gop.getWorkflow());
                        Log.d("lbl",gop.getLabel());
                        Location l = gop.getLocation();
                        LatLong ll = Geomatte.convertToLatLong(l.getX(),l.getY());
                        /*
                        CircleOptions co = new CircleOptions().center(ll.ll()).radius(2000).
                                strokeColor(Color.parseColor(gop.getColor())).
                                clickable(true);

                        mMap.setOnCircleClickListener(new GoogleMap.OnCircleClickListener() {
                            @Override
                            public void onCircleClick(Circle circle) {
                                // Flip the r, g and b components of the circle's
                                // stroke color.
                                int strokeColor = circle.getStrokeColor() ^ 0x00ffffff;
                                circle.setStrokeColor(strokeColor);
                            }
                            });
                        mMap.addCircle(co);

                         */

                        IconGenerator icg = new IconGenerator(this.mContext.getContext());
                        icg.setStyle(IconGenerator.STYLE_WHITE);
                        Bitmap bmp = icg.makeIcon(gop.getLabel());
                        MarkerOptions mo = new MarkerOptions().position(ll.ll()).title("TRAKT").snippet(gop.getStatus()).
                                icon(BitmapDescriptorFactory.fromBitmap(bmp)).snippet(gop.getWorkflow());
                        Marker marker = mMap.addMarker(mo);
                        marker.setTag(gop);
                        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
                            @Override
                            public void onInfoWindowClick(Marker marker) {
                                Log.d("vortex",marker.getSnippet());
                                GoogleGis.this.runSelectedWf((GisObject)marker.getTag());
                            }
                        });
                    }
                }
            } else
                Log.d("google","No bags today");


        }

    }


    @Override
    public void onEvent(Event e) {
        if (e.getType() == Event.EventType.onFlowExecuted) {
            Log.d("google","GoogleGis: FLOW EXECUTED");
            init();
        }
    }

    @Override
    public String getName() {
        return "GoogleGis";
    }
}
