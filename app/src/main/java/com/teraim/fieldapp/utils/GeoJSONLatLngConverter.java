package com.teraim.fieldapp.utils;

import android.content.Context;
import android.util.Log;

import com.google.maps.android.data.geojson.GeoJsonParser;
import com.teraim.fieldapp.dynamic.types.LatLong;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GeoJSONLatLngConverter {

    public static JSONObject generate(int resourceId, Context ctx)
            throws IOException, JSONException {

        InputStream stream = ctx.getResources().openRawResource(resourceId);
        String line;
        StringBuilder result = new StringBuilder();
        // Reads from stream
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        // Read each line of the GeoJSON file into a string
        boolean geoLines = false;
        String xE=null,Ny=null;
        while ((line = reader.readLine()) != null) {
            if (geoLines) {
                if (xE == null) {
                    xE = line;
                } else if(Ny==null) {
                    Ny = line;
                    geoLines=false;
                    double n,e;
                    n = Double.parseDouble(Ny.trim());
                    e = Double.parseDouble(xE.trim().replace(",",""));
                    xE=null;Ny=null;
                    LatLong ll = Geomatte.convertToLatLong(e, n);
                    Log.d("google","Latlong: "+ll.getX()+","+ll.getY());
                    String lng = "           "+ll.getY()+",\n";
                    String lat = "           "+ll.getX()+"\n";
                    result.append(lng);
                    result.append(lat);
                }


            } else {
                if (line.contains("coordinates"))
                    geoLines = true;
                result.append(line);
            }
        }
        reader.close();

        // Converts the result string into a JSONObject
        return new JSONObject(result.toString());
    }
}
