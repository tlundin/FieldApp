package com.teraim.fieldapp.dynamic.workflow_realizations.gis;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.libraries.maps.GoogleMap;
import com.google.android.libraries.maps.model.BitmapDescriptorFactory;
import com.google.android.libraries.maps.model.LatLng;
import com.google.android.libraries.maps.model.LatLngBounds;
import com.google.android.libraries.maps.model.Marker;
import com.google.android.libraries.maps.model.MarkerOptions;
import com.google.maps.android.collections.GroundOverlayManager;
import com.google.maps.android.collections.MarkerManager;
import com.google.maps.android.collections.PolygonManager;
import com.google.maps.android.collections.PolylineManager;
import com.google.maps.android.data.Feature;
import com.google.maps.android.data.geojson.GeoJsonFeature;
import com.google.maps.android.data.geojson.GeoJsonLayer;
import com.google.maps.android.data.geojson.GeoJsonLineStringStyle;
import com.google.maps.android.data.geojson.GeoJsonPoint;
import com.google.maps.android.data.geojson.GeoJsonPointStyle;
import com.google.maps.android.data.geojson.GeoJsonPolygon;
import com.google.maps.android.data.geojson.GeoJsonPolygonStyle;
import com.google.maps.android.ui.IconGenerator;
import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.dynamic.blocks.CreateGoogleBlock;
import com.teraim.fieldapp.dynamic.types.GisLayer;
import com.teraim.fieldapp.dynamic.types.LatLong;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.utils.GeoJSONLatLngConverter;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.utils.PersistenceHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class GoogleGis extends GIS implements EventListener {
    private GoogleMap mMap;
    WF_Context mContext;
    public GoogleGis(String id, View v, GisViewImplementation mMap, WF_Context mContext, CreateGoogleBlock cfg) {
        super(id, v, cfg.isVisible, mContext,mMap);
        this.mContext = mContext;
        mContext.registerEventListener(this, Event.EventType.onFlowExecuted);
        myLayers = new ArrayList<>();
        if (cfg.showTeam) {
            String team = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
            if (team != null && !team.isEmpty()) {
                Log.d("bortex", "team is visible! Adding layer for team " + team);
                final GisLayer teamLayer = new GisLayer("Team", "Team", true, true, true,null);
                myLayers.add(teamLayer);
            }
        }
        this.mMap=mMap.getGoogleMap();
    }

    private void init() {
        Log.d("google","Layers: ");
        for(GisLayer layer:myLayers) {
            if (layer.isVisible()) {
                if(layer.isGeoJson()) {
                        Log.d("google","IS GEOJSON");
                        retrieveFileFromUrl(layer.getGeoJsonSource());




                }
                Log.d("google", layer.getLabel());
                Map<String, Set<GisObject>> bags = layer.getGisBags();
                if (false) {
                    for (String bag : bags.keySet()) {
                        Log.d("google", "bag:" + bag);
                        Set<GisObject> gops = bags.get(bag);
                        for (GisObject gop : gops) {
                            Log.d("wf", gop.getWorkflow());
                            Log.d("lbl", gop.getLabel());
                            Location l = gop.getLocation();
                            LatLong ll = Geomatte.convertToLatLong(l.getX(), l.getY());
                            FullGisObjectConfiguration.GisPolyType type = gop.foc.getGisPolyType();
                            if (type == FullGisObjectConfiguration.GisPolyType.Point) {
                                FullGisObjectConfiguration.Shape shape = gop.foc.getShape();
                                if (shape == FullGisObjectConfiguration.Shape.marker) {
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
                                            Log.d("vortex", marker.getSnippet());
                                            GoogleGis.this.runSelectedWf((GisObject) marker.getTag());
                                        }
                                    });
                                }
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


                            }
                        }
                    }
                } else
                    Log.d("google", "No bags today");
            }

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

    @Override
    public void postDraw() {
        Log.d("hepp","POSTDRAW");
    }

    private GoogleMap getMap() {
        return mMap;
    }

    private void retrieveFileFromUrl(String geojsonFile) {
        String project = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.BUNDLE_NAME).toLowerCase();
        String server = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.SERVER_URL);
        String url = server+project+"/"+"gis_objects"+"/"+geojsonFile+".json";
        Log.d("google","URL IS "+url);
        new DownloadGeoJsonFile().execute(url);

    }


    private class DownloadGeoJsonFile extends AsyncTask<String, Void, GeoJsonLayer> {

        @Override
        protected GeoJsonLayer doInBackground(String... params) {
            try {
                // Open a stream from the URL
                InputStream stream = new URL(params[0]).openStream();
                String line;
                StringBuilder result = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                boolean geoLines = false;
                String xE=null,Ny=null;
                reader.readLine();
                while ((line = reader.readLine()) != null) {
                    if (line.contains("properties"))
                        geoLines = false;
                    if (geoLines) {
                        if(isCoord(line)) {
                            if (xE == null) {
                                if (line.trim().equals("0.0"))
                                    continue;
                                xE = line;
                            } else if (Ny == null) {
                                Ny = line;
                                double n, e;
                                n = Double.parseDouble(Ny.trim().replace(",", ""));
                                e = Double.parseDouble(xE.trim().replace(",", ""));
                                xE = null;
                                Ny = null;
                                LatLong ll = Geomatte.convertToLatLong(e, n);
                                //Log.d("google", "Latlong: " + ll.getX() + "," + ll.getY());
                                String lng = "           " + ll.getY() + ",\n";
                                String lat = "           " + ll.getX() + "\n";
                                result.append(lng);
                                result.append(lat);
                            }
                        } else
                            result.append(line);

                    } else {
                        if (line.contains("coordinates"))
                            geoLines = true;
                        result.append(line);
                    }
                }

                // Close the stream
                reader.close();
                stream.close();
                String resS = result.toString();
                Log.d("google","RES:"+resS);
                return new GeoJsonLayer(getMap(), new JSONObject(resS));
            } catch (IOException e) {
                Log.e("google", "GeoJSON file could not be read");
            } catch (JSONException e) {
                Log.e("google", "GeoJSON file could not be converted to a JSONObject");
            }
            return null;
        }

        private boolean isCoord(String line) {
            String p = line.trim();
            if (p.length()==0)
                return false;
            boolean isLine = Character.isDigit(p.toCharArray()[0]);
            //Log.d("google",line+" is a coord: "+isLine);
            return Character.isDigit(p.toCharArray()[0]);
        }

        @Override
        protected void onPostExecute(GeoJsonLayer layer) {
            if (layer != null) {
                addGeoJsonLayerToMap(layer);
            }
        }
    }
    private void addGeoJsonLayerToMap(GeoJsonLayer layer) {

        fillPolygons(layer);
        layer.addLayerToMap();
        // Demonstrate receiving features via GeoJsonLayer clicks.
        layer.setOnFeatureClickListener(new GeoJsonLayer.GeoJsonOnFeatureClickListener() {
            @Override
            public void onFeatureClick(Feature feature) {
                Toast.makeText(mContext.getContext(),
                        "Feature clicked: " + feature.getProperty("title"),
                        Toast.LENGTH_SHORT).show();
            }

        });
    }

    private void fillPolygons(GeoJsonLayer layer) {
        // Iterate over all the features stored in the layer
        for (GeoJsonFeature feature : layer.getFeatures()) {
            // Check if the magnitude property exists
            if (feature.getGeometry() instanceof GeoJsonPolygon) {
                if (feature.hasProperty("TYPKOD")) {
                    String typkod = feature.getProperty("TYPKOD");
                    Log.d("google", "TYPkod " + typkod);
                    if (typkod.equals("HP")) {
                        GeoJsonPolygonStyle ps = new GeoJsonPolygonStyle();
                        ps.setFillColor(Color.CYAN);
                        LatLng x = ((GeoJsonPolygon) feature.getGeometry()).getCoordinates().get(0).get(0);
                        addText(x, feature.getProperty("Shape_Area"), 1, 12);
                        feature.setPolygonStyle(ps);
                    }
                } else
                    Log.d("google", "no typ");
            } else if (feature.getGeometry() instanceof GeoJsonPoint) {
                IconGenerator icg = new IconGenerator(this.mContext.getContext());
                icg.setStyle(IconGenerator.STYLE_WHITE);
                Bitmap bmp = icg.makeIcon(feature.getProperty("TRAKT"));
                GeoJsonPointStyle gp = new GeoJsonPointStyle();
                gp.setIcon(BitmapDescriptorFactory.fromBitmap(bmp));
                gp.setTitle("Trakt");
                feature.setPointStyle(gp);
            }
        }
    }
    public Marker addText(final LatLng location, final String text, final int padding,
                          final int fontSize) {
        Marker marker = null;

        if (mContext.getContext() == null || mMap == null || location == null || text == null
                || fontSize <= 0) {
            return marker;
        }

        final TextView textView = new TextView(mContext.getContext());
        textView.setText(text);
        textView.setTextSize(fontSize);

        final Paint paintText = textView.getPaint();

        final Rect boundsText = new Rect();
        paintText.getTextBounds(text, 0, textView.length(), boundsText);
        paintText.setTextAlign(Paint.Align.CENTER);

        final Bitmap.Config conf = Bitmap.Config.ARGB_8888;
        final Bitmap bmpText = Bitmap.createBitmap(boundsText.width() + 2
                * padding, boundsText.height() + 2 * padding, conf);

        final Canvas canvasText = new Canvas(bmpText);
        paintText.setColor(Color.WHITE);

        canvasText.drawText(text, canvasText.getWidth() / 2,
                canvasText.getHeight() - padding - boundsText.bottom, paintText);

        final MarkerOptions markerOptions = new MarkerOptions()
                .position(location)
                .icon(BitmapDescriptorFactory.fromBitmap(bmpText))
                .anchor(0.5f, 1);

        marker = mMap.addMarker(markerOptions);

        return marker;
    }


}
