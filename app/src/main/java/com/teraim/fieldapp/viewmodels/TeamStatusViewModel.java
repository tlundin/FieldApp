package com.teraim.fieldapp.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.Log;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;


import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;


import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Objects; // Added for Objects.equals
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;


import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.gis.TrackerListener;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.Connectivity;
import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.gis.TrackerListener.GPS_State;
import com.teraim.fieldapp.dynamic.types.LatLong;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.utils.Geomatte;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.FullGisObjectConfiguration;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPointObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.StaticGisPoint;
import com.teraim.fieldapp.R;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.ui.MapNeedlePreference;


public class TeamStatusViewModel extends AndroidViewModel implements TrackerListener {

    private static final String TAG = "TeamStatusViewModel";

    private final MutableLiveData<String> _teamPositionsRaw = new MutableLiveData<>();
    private final MutableLiveData<Set<GisPointObject>> _teamMemberGisObjects = new MutableLiveData<>();
    public LiveData<Set<GisPointObject>> teamMemberGisObjects = _teamMemberGisObjects;

    private final MutableLiveData<String> _serverStatus = new MutableLiveData<>();
    public LiveData<String> serverStatus = _serverStatus;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _isUpdating = new MutableLiveData<>();
    public LiveData<Boolean> isUpdating = _isUpdating;

    private final MutableLiveData<Boolean> _serverPendingUpdate = new MutableLiveData<>();
    public LiveData<Boolean> serverPendingUpdate = _serverPendingUpdate;

    private RequestQueue requestQueue;
    private final AtomicInteger activeRequestCount = new AtomicInteger(0);
    private GlobalState gs;
    private PersistenceHelper globalPh;
    private GPS_State latestSignal;

    // List to hold all available custom map needle bitmaps
    private List<Bitmap> allAvailableCustomNeedles;
    // Cache for individual team member specific needles (loaded from server parameter)
    // Key: user UUID, Value: Bitmap for their icon
    private Map<String, Bitmap> teamMemberSpecificNeedleCache;

    public TeamStatusViewModel(Application application) {
        super(application);
        this.gs = GlobalState.getInstance();
        this.requestQueue = gs.getRequestQueue();
        this.globalPh = gs.getGlobalPreferences();

        _isUpdating.setValue(false);
        _serverPendingUpdate.setValue(globalPh.getB(PersistenceHelper.SERVER_PENDING_UPDATE));

        // Initialize custom needle collections
        allAvailableCustomNeedles = new ArrayList<>();
        teamMemberSpecificNeedleCache = new HashMap<>();
        loadAllCustomNeedles(); // Load all custom needles once during ViewModel init
        gs.registerListener(this, TrackerListener.Type.USER);
    }

    // Method to load all 12 (or more) individual custom map needle icons
    private void loadAllCustomNeedles() {
        try {
            TypedArray ta = getApplication().getResources().obtainTypedArray(R.array.map_needle_image_sets);
            for (int i = 0; i < ta.length(); i++) {
                int resourceId = ta.getResourceId(i, 0);
                if (resourceId != 0) {
                    // Use the static helper method from MapNeedlePreference to crop
                    allAvailableCustomNeedles.addAll(MapNeedlePreference.cropAllNeedlesFromSet(getApplication(), resourceId));
                }
            }
            ta.recycle(); // Important: Recycle TypedArray
           // Log.d(TAG, "Loaded " + allAvailableCustomNeedles.size() + " custom map needles.");
        } catch (Exception e) {
            Log.e(TAG, "Error loading all custom map needles: " + e.getMessage(), e);
        }
    }

    /** Read current user's map needle index from the same SharedPreferences as the settings screen (GlobalPrefs). */
    private int getCurrentUserNeedleIndex() {
        try {
            return getApplication().getSharedPreferences(Constants.GLOBAL_PREFS, Context.MODE_PRIVATE)
                    .getInt(PersistenceHelper.MAP_NEEDLE_INDEX, 0);
        } catch (Exception e) {
            Log.e(TAG, "Error reading map_needle_set preference: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public void gpsStateChanged(GPS_State signal) {
        this.latestSignal = signal;
    }

    /**
     * POSTs only the current user's position and updates LiveData with local "me" position.
     * Use for 1-second "me" updates. Does not fetch team positions.
     */
    public void sendMyPositionOnly() {
        if (!Connectivity.isConnected(getApplication())) {
            return;
        }
        if (latestSignal == null || latestSignal.state == GPS_State.State.disabled) {
            return;
        }
        double lat = -1, lng = -1;
        if (latestSignal.lat != -1 && latestSignal.lng != -1) {
            lat = latestSignal.lat;
            lng = latestSignal.lng;
        } else if (latestSignal.x != -1 && latestSignal.y != -1) {
            LatLong wgs84 = Geomatte.convertToLatLong(latestSignal.y, latestSignal.x);
            lat = wgs84.getX();
            lng = wgs84.getY();
        }
        if (lat == -1 || lng == -1) return;

        try {
            JSONObject positionObject = new JSONObject();
            positionObject.put("lat", lat);
            positionObject.put("long", lng);
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("uuid", gs.getUserUUID());
            jsonBody.put("name", gs.getGlobalPreferences().get(PersistenceHelper.USER_ID_KEY));
            jsonBody.put("timestamp", latestSignal.time);
            jsonBody.put("icon", getCurrentUserNeedleIndex());
            jsonBody.put("position", positionObject);

            final String requestBody = jsonBody.toString();
            StringRequest postRequest = new StringRequest(Request.Method.POST, Constants.SynkStatusURI + "/position",
                    response -> { },
                    error -> Log.e(TAG, "Error posting my position: " + getVolleyErrorString(error))) {
                @Override
                public String getBodyContentType() { return "application/json; charset=utf-8"; }
                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException e) {
                        throw new AuthFailureError("Encoding error", e);
                    }
                }
                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    return Response.success("", HttpHeaderParser.parseCacheHeaders(response));
                }
            };
            requestQueue.add(postRequest);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for my position: " + e.getMessage());
            return;
        }

        // Update LiveData with local "me" position so map refreshes immediately
        String teamName = gs.getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
        String currentUserUUID = globalPh.get(PersistenceHelper.USERUUID_KEY);
        String nameFromPref = globalPh.get(PersistenceHelper.USER_ID_KEY);
        final String myName = (nameFromPref != null && !nameFromPref.isEmpty()) ? nameFromPref : "me";
        Location myLocation = new LatLong(lat, lng);
        int needleIndex = getCurrentUserNeedleIndex();
        if (allAvailableCustomNeedles == null || allAvailableCustomNeedles.isEmpty()) loadAllCustomNeedles();
        boolean usingCustom = allAvailableCustomNeedles != null && !allAvailableCustomNeedles.isEmpty() && needleIndex >= 0 && needleIndex < allAvailableCustomNeedles.size();
        Bitmap myIcon = usingCustom ? allAvailableCustomNeedles.get(needleIndex) : getDefaultTeamMemberIcon(latestSignal.time);
        final Map<String, String> myKeychain = new HashMap<>();
        myKeychain.put(DbHelper.YEAR, Constants.getYear());
        myKeychain.put("lag", teamName != null ? teamName : "");
        myKeychain.put("author", myName);
        myKeychain.put("uuid", currentUserUUID);
        myKeychain.put("timestamp", String.valueOf(latestSignal.time));
        GisPointObject myGisObject = new StaticGisPoint(new FullGisObjectConfiguration() {
            @Override public float getLineWidth() { return 2.0f; }
            @Override public float getRadius() { return 4.0f; }
            @Override public String getColor() { return "black"; }
            @Override public String getBorderColor() { return "red"; }
            @Override public GisObjectType getGisPolyType() { return GisObjectType.Point; }
            @Override public android.graphics.Bitmap getIcon() { return myIcon; }
            @Override public Paint.Style getStyle() { return Paint.Style.FILL_AND_STROKE; }
            @Override public PolyType getShape() { return PolyType.circle; }
            @Override public String getClickFlow() { return "wf_teammember"; }
            @Override public DB_Context getObjectKeyHash() { return new DB_Context("år=[getCurrentYear()], lag = [getTeamName()], author ", myKeychain); }
            @Override public String getStatusVariable() { return null; }
            @Override public boolean isUser() { return true; }
            @Override public String getName() { return myName; }
            @Override public String getRawLabel() { return myName; }
            @Override public String getCreator() { return ""; }
            @Override public boolean useIconOnMap() { return true; }
            @Override public boolean isVisible() { return true; }
            @Override public List<Expressor.EvalExpr> getLabelExpression() { return Expressor.preCompileExpression(myName); }
        }, myKeychain, myLocation, null, null);
        myGisObject.setLabel(myName + " (me)");

        Set<GisPointObject> current = _teamMemberGisObjects.getValue();
        Set<GisPointObject> updated = new HashSet<>(current != null ? current : java.util.Collections.emptySet());
        updated.removeIf(g -> {
            Map<String, String> kh = g.getKeyHash();
            return kh != null && currentUserUUID != null && currentUserUUID.equals(kh.get("uuid"));
        });
        updated.add(myGisObject);
        _teamMemberGisObjects.postValue(updated);
    }

    public void sendAndReceiveTeamPositions() {
        if (!Connectivity.isConnected(getApplication())) {
            Log.d(TAG, "No internet connection, skipping sync.");
            _errorMessage.postValue("No internet connection.");
            if (activeRequestCount.get() == 0) {
                _isUpdating.setValue(false);
            }
            return;
        }

        if (activeRequestCount.get() > 0) {
            Log.d(TAG, "Skipping new sync cycle, " + activeRequestCount.get() + " requests already active.");
            return;
        }

       // Log.d(TAG, "Initiating network calls...");
        _isUpdating.setValue(true);
        _errorMessage.postValue(null);

        boolean updateMyPosition = true;
        if (latestSignal == null || latestSignal.state == GPS_State.State.disabled) {
            Log.d(TAG, "No valid GPS signal available, skipping position update.");
            updateMyPosition = false;
        }

        String team = gs.getMyTeam();
        String project = globalPh.get(PersistenceHelper.BUNDLE_NAME);
        String useruuid = globalPh.get(PersistenceHelper.USERUUID_KEY);

        Runnable decrementAndCheck = () -> {
            if (activeRequestCount.decrementAndGet() == 0) {
                _isUpdating.postValue(false);
            }
        };

        // --- 1. POST My Position (WGS84 lat/long - Android native format, no SWEREF conversion) ---
        if (updateMyPosition) {
            JSONObject jsonBody = new JSONObject();
            try {
                double lat = -1, lng = -1;
                if (latestSignal.lat != -1 && latestSignal.lng != -1) {
                    lat = latestSignal.lat;
                    lng = latestSignal.lng;
                } else if (latestSignal.x != -1 && latestSignal.y != -1) {
                    LatLong wgs84 = Geomatte.convertToLatLong(latestSignal.y, latestSignal.x);
                    lat = wgs84.getX();
                    lng = wgs84.getY();
                } else {
                    Log.d(TAG, "No valid coordinates (lat,lng or x,y), skipping position update.");
                    updateMyPosition = false;
                }
                if (updateMyPosition && lat != -1 && lng != -1) {
                    JSONObject positionObject = new JSONObject();
                    positionObject.put("lat", lat);
                    positionObject.put("long", lng);

                    jsonBody.put("uuid", gs.getUserUUID());
                    jsonBody.put("name", gs.getGlobalPreferences().get(PersistenceHelper.USER_ID_KEY));
                    jsonBody.put("timestamp", latestSignal.time);
                    jsonBody.put("icon", getCurrentUserNeedleIndex());
                    jsonBody.put("position", positionObject);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error creating JSON for my position: " + e.getMessage());
                _errorMessage.postValue("Internal error: " + e.getMessage());
                _isUpdating.postValue(false);
                return;
            }
            if (updateMyPosition) {
            final String requestBody = jsonBody.toString();
            final String SendMyPoisition = Constants.SynkStatusURI + "/position";

            activeRequestCount.incrementAndGet();
            StringRequest postMyPositionRequest = new StringRequest(Request.Method.POST, SendMyPoisition,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
    //                        Log.d(TAG, "My position posted successfully: " + response);
                            decrementAndCheck.run();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String errorMessage = getVolleyErrorString(error);
                    Log.e(TAG, "Error posting my position: " + errorMessage);
                    _errorMessage.postValue("Error sending position: " + errorMessage);
                    decrementAndCheck.run();
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        Log.e(TAG, String.format("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8"));
                        _errorMessage.postValue("Encoding error for position data.");
                        decrementAndCheck.run();
                        throw new AuthFailureError("Encoding error", uee);
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    if (response != null) {
                        return Response.success(String.valueOf(response.statusCode), HttpHeaderParser.parseCacheHeaders(response));
                    }
                    return Response.success("", HttpHeaderParser.parseCacheHeaders(response));
                }
            };
            requestQueue.add(postMyPositionRequest);
            }
        }

        // --- 2. GET Team Positions ---
        final String GetPoisitions = Constants.SynkStatusURI + "/positions";
        activeRequestCount.incrementAndGet();
        StringRequest getTeamStatusRequest = new StringRequest(Request.Method.GET, GetPoisitions,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
  //                      Log.d(TAG, "Team positions received: " + response);
                        _teamPositionsRaw.postValue(response);
                        processTeamPositionsResponse(response);
                        decrementAndCheck.run();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                String errorMessage = getVolleyErrorString(error);
                Log.e(TAG, "Error getting team positions: " + errorMessage);
                _errorMessage.postValue("Error getting team positions: " + errorMessage);
                decrementAndCheck.run();
            }
        });
        requestQueue.add(getTeamStatusRequest);

        // --- 3. GET Server Status ---
        final String exportServerURL = gs.getGlobalPreferences().get(PersistenceHelper.EXPORT_SERVER_URL);
        if (exportServerURL == null || exportServerURL.isEmpty()) {
            Log.w(TAG, "EXPORT_SERVER_URL is not configured, skipping server status check.");
        } else {
            final String GetServerStatus = exportServerURL + "/server";
            activeRequestCount.incrementAndGet();
            StringRequest getServerStatusRequest = new StringRequest(Request.Method.GET, GetServerStatus,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
      //                      Log.d(TAG, "Server status received: " + response.substring(0, Math.min(response.length(), 100)) + "...");
                            processServerStatusResponse(response);
                            decrementAndCheck.run();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    String errorMessage = getVolleyErrorString(error);
                    Log.e(TAG, "Error getting server status: " + errorMessage);
                    Log.d(TAG, "server "+GetServerStatus);
                    _errorMessage.postValue("Error getting server status: " + errorMessage);
                    decrementAndCheck.run();
                }
            });
            requestQueue.add(getServerStatusRequest);
        }
    }

    // Method to process the raw team positions JSON into a Set of GisPointObjects.
    // Uses 'name' as unique key: only the LATEST position (by timestamp) per name is shown.
    // For the current user ("me"), map needle always comes from device preference (mapNeedlePref), not from server.
    private void processTeamPositionsResponse(String jsonString) {
        Set<GisPointObject> teamMembers = new HashSet<>();
        boolean addedMeFromResponse = false; // true if we added "me" from server response with local icon
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            String teamName = gs.getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
            String currentUserUUID = globalPh.get(PersistenceHelper.USERUUID_KEY); // Get current user's UUID

            // Keep only the latest entry per name (by timestamp). Always keep current user ("me") separate
            // so we never drop "me" when another user has the same display name with a newer timestamp.
            java.util.Map<String, JSONObject> latestByName = new HashMap<>();
            JSONObject meEntry = null; // latest API entry for current user (by uuid)
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject ob = jsonArray.getJSONObject(i);
                String n = ob.optString("name", "");
                String u = ob.optString("uuid", "");
                long ts = ob.optLong("timestamp", 0L);
                boolean isCurrentUser = currentUserUUID != null && !currentUserUUID.isEmpty() && u != null
                        && u.trim().equalsIgnoreCase(currentUserUUID.trim());
                if (isCurrentUser) {
                    if (meEntry == null || meEntry.optLong("timestamp", 0L) < ts) {
                        meEntry = ob;
                    }
                } else {
                    if (!latestByName.containsKey(n) || latestByName.get(n).optLong("timestamp", 0L) < ts) {
                        latestByName.put(n, ob);
                    }
                }
            }

            int deviceNeedleIndex = getCurrentUserNeedleIndex();
            // Process current user from API first (so "me" is always shown with preference needle and API position when available)
            if (meEntry != null) {
                JSONObject memberJson = meEntry;
                String name = memberJson.getString("name");
                String uuid = memberJson.getString("uuid");
                long timestamp = memberJson.getLong("timestamp");
                JSONObject positionJson = memberJson.getJSONObject("position");
                double lat = positionJson.getDouble("lat");
                double lng = positionJson.getDouble("long");

                addedMeFromResponse = true;
                if (allAvailableCustomNeedles == null || allAvailableCustomNeedles.isEmpty()) {
                    loadAllCustomNeedles();
                }
                int needleIndex = getCurrentUserNeedleIndex(); // from Preference (MAP_NEEDLE_INDEX)
                boolean useCustom = allAvailableCustomNeedles != null && !allAvailableCustomNeedles.isEmpty() && needleIndex >= 0 && needleIndex < allAvailableCustomNeedles.size();
                Bitmap myIcon = useCustom ? allAvailableCustomNeedles.get(needleIndex) : getDefaultTeamMemberIcon(timestamp);
                final Map<String, String> myKeychain = new HashMap<>();
                myKeychain.put(DbHelper.YEAR, Constants.getYear());
                myKeychain.put("lag", teamName != null ? teamName : "");
                myKeychain.put("author", name);
                myKeychain.put("uuid", uuid);
                myKeychain.put("timestamp", String.valueOf(timestamp));
                Location myLocation = new LatLong(lat, lng);
                final Bitmap iconForMe = myIcon;
                GisPointObject myGisObject = new StaticGisPoint(new FullGisObjectConfiguration() {
                    @Override public float getLineWidth() { return 2.0f; }
                    @Override public float getRadius() { return 4.0f; }
                    @Override public String getColor() { return "black"; }
                    @Override public String getBorderColor() { return "red"; }
                    @Override public GisObjectType getGisPolyType() { return GisObjectType.Point; }
                    @Override public android.graphics.Bitmap getIcon() { return iconForMe; }
                    @Override public Paint.Style getStyle() { return Paint.Style.FILL_AND_STROKE; }
                    @Override public PolyType getShape() { return PolyType.circle; }
                    @Override public String getClickFlow() { return "wf_teammember"; }
                    @Override public DB_Context getObjectKeyHash() { return new DB_Context("år=[getCurrentYear()], lag = [getTeamName()], author ", myKeychain); }
                    @Override public String getStatusVariable() { return null; }
                    @Override public boolean isUser() { return true; }
                    @Override public String getName() { return name; }
                    @Override public String getRawLabel() { return name; }
                    @Override public String getCreator() { return ""; }
                    @Override public boolean useIconOnMap() { return true; }
                    @Override public boolean isVisible() { return true; }
                    @Override public List<Expressor.EvalExpr> getLabelExpression() { return Expressor.preCompileExpression(name); }
                }, myKeychain, myLocation, null, null);
                myGisObject.setLabel(name + " (me)");
                teamMembers.add(myGisObject);
            }

            for (JSONObject memberJson : latestByName.values()) {
                String name = memberJson.getString("name");
                String uuid = memberJson.getString("uuid");
                long timestamp = memberJson.getLong("timestamp");
                JSONObject positionJson = memberJson.getJSONObject("position");
                double lat = positionJson.getDouble("lat");
                double lng = positionJson.getDouble("long");

                // Optional map_needle_id from server (may be number or string) — ignored for current user; we always use device preference
                String mapNeedleIdStr = null;
                if (memberJson.has("icon")) {
                    Object iconObj = memberJson.get("icon");
                    mapNeedleIdStr = iconObj instanceof Number ? String.valueOf(((Number) iconObj).intValue()) : String.valueOf(iconObj);
                }

                // (Current user already processed above from meEntry; latestByName contains only other users.)

                // Create a key for the workflow (as in GisImageView's original findMyTeam)
                final Map<String, String> keychain = new HashMap<>();
                keychain.put(DbHelper.YEAR, Constants.getYear());
                keychain.put("lag", teamName != null ? teamName : "");
                keychain.put("author", name);
                // Also add UUID to keychain for potential use by GisPointObject itself for identification
                keychain.put("uuid", uuid);
                keychain.put("timestamp", String.valueOf(timestamp));

                Location memberLocation = new LatLong(lat, lng);

                // --- Determine the correct icon for this team member (keyed by name for cache/persist) ---
                Bitmap finalIconBitmap = null;

                // Get the currently persisted icon ID for this user (by name), if any.
                String persistedNeedleIdStr = globalPh.get("user_map_needle_" + name);

                // Determine the effective needle ID to use for this update cycle.
                // Server-provided ID takes precedence. If absent, use the persisted one.
                String effectiveNeedleIdStr = mapNeedleIdStr;
                if (effectiveNeedleIdStr == null || effectiveNeedleIdStr.isEmpty()) {
                    effectiveNeedleIdStr = persistedNeedleIdStr;
                }

                // Check if we can use the cached bitmap (cache keyed by name).
                if (teamMemberSpecificNeedleCache.containsKey(name) && Objects.equals(persistedNeedleIdStr, effectiveNeedleIdStr)) {
                    finalIconBitmap = teamMemberSpecificNeedleCache.get(name);
                }

                // If not found in cache (or cache was stale), load/derive the bitmap
                if (finalIconBitmap == null) {
                    if (effectiveNeedleIdStr != null && !effectiveNeedleIdStr.isEmpty()) {
                        try {
                            int needleIndex = Integer.parseInt(effectiveNeedleIdStr);
                            if (allAvailableCustomNeedles != null && needleIndex >= 0 && needleIndex < allAvailableCustomNeedles.size()) {
                                finalIconBitmap = allAvailableCustomNeedles.get(needleIndex);
                                teamMemberSpecificNeedleCache.put(name, finalIconBitmap);
                                globalPh.put("user_map_needle_" + name, effectiveNeedleIdStr);
                            } else {
                                Log.w(TAG, "Icon ID " + effectiveNeedleIdStr + " for user " + name + " is out of bounds or invalid. Falling back.");
                                finalIconBitmap = getDefaultTeamMemberIcon(timestamp);
                                teamMemberSpecificNeedleCache.put(name, finalIconBitmap);
                                globalPh.remove("user_map_needle_" + name);
                            }
                        } catch (NumberFormatException e) {
                            finalIconBitmap = getDefaultTeamMemberIcon(timestamp);
                            teamMemberSpecificNeedleCache.put(name, finalIconBitmap);
                            globalPh.remove("user_map_needle_" + name);
                        }
                    } else {
                        finalIconBitmap = getDefaultTeamMemberIcon(timestamp);
                        teamMemberSpecificNeedleCache.put(name, finalIconBitmap);
                        globalPh.remove("user_map_needle_" + name);
                    }
                }

                // Sanity check: if finalIconBitmap is still null (shouldn't happen with the logic above, but good practice)
                if (finalIconBitmap == null) {
                    Log.e(TAG, "Failed to determine icon for team member " + name + ", using generic fallback.");
                    finalIconBitmap = getDefaultTeamMemberIcon(timestamp);
                    teamMemberSpecificNeedleCache.put(name, finalIconBitmap);
                }

                // Create GisPointObject (StaticGisPoint)
                Bitmap iconForGisObject = finalIconBitmap; // Store the resolved bitmap
                GisPointObject memberGisObject = new StaticGisPoint(new FullGisObjectConfiguration() {
                    @Override public float getLineWidth() { return 2.0f; }
                    @Override public float getRadius() { return 4.0f; }
                    @Override public String getColor() { return "black"; }
                    @Override public String getBorderColor() { return "red"; }
                    @Override public GisObjectType getGisPolyType() { return GisObjectType.Point; }
                    @Override public android.graphics.Bitmap getIcon() { return iconForGisObject; } // Provide the resolved bitmap
                    @Override public Paint.Style getStyle() { return Paint.Style.FILL_AND_STROKE; }
                    @Override public PolyType getShape() { return PolyType.circle; }
                    @Override public String getClickFlow() { return "wf_teammember"; }
                    @Override public DB_Context getObjectKeyHash() { return new DB_Context("år=[getCurrentYear()], lag = [getTeamName()], author ", keychain); }
                    @Override public String getStatusVariable() { return null; }
                    @Override public boolean isUser() { return false; }
                    @Override public String getName() { return name; }
                    @Override public String getRawLabel() { return name; }
                    @Override public String getCreator() { return ""; }
                    @Override public boolean useIconOnMap() { return true; }
                    @Override public boolean isVisible() { return true; }
                    @Override public List<Expressor.EvalExpr> getLabelExpression() { return Expressor.preCompileExpression(name); }
                }, keychain, memberLocation, null, null);

                // Update label with timestamp details
                memberGisObject.setLabel(name + "[" + Tools.getTimeStampDetails(timestamp, true) + "]");

                teamMembers.add(memberGisObject);
            }

            // Add current user to the team layer with local position and current needle preference when not already added from API response.
            if (!addedMeFromResponse && currentUserUUID != null && !currentUserUUID.isEmpty() && latestSignal != null && latestSignal.state != GPS_State.State.disabled) {
                String nameFromPref = globalPh.get(PersistenceHelper.USER_ID_KEY);
                final String myName = (nameFromPref != null && !nameFromPref.isEmpty()) ? nameFromPref : "me";
                int needleIndex = getCurrentUserNeedleIndex();
                if (allAvailableCustomNeedles == null || allAvailableCustomNeedles.isEmpty()) {
                    loadAllCustomNeedles();
                }
                boolean usingCustom = allAvailableCustomNeedles != null && !allAvailableCustomNeedles.isEmpty() && needleIndex >= 0 && needleIndex < allAvailableCustomNeedles.size();
                Bitmap myIcon = usingCustom ? allAvailableCustomNeedles.get(needleIndex) : getDefaultTeamMemberIcon(latestSignal.time);
                final Map<String, String> myKeychain = new HashMap<>();
                myKeychain.put(DbHelper.YEAR, Constants.getYear());
                myKeychain.put("lag", teamName != null ? teamName : "");
                myKeychain.put("author", myName);
                myKeychain.put("uuid", currentUserUUID);
                myKeychain.put("timestamp", String.valueOf(latestSignal.time));
                Location myLocation = (latestSignal.lat != -1 && latestSignal.lng != -1)
                        ? new LatLong(latestSignal.lat, latestSignal.lng)
                        : Geomatte.convertToLatLong(latestSignal.y, latestSignal.x);
                GisPointObject myGisObject = new StaticGisPoint(new FullGisObjectConfiguration() {
                    @Override public float getLineWidth() { return 2.0f; }
                    @Override public float getRadius() { return 4.0f; }
                    @Override public String getColor() { return "black"; }
                    @Override public String getBorderColor() { return "red"; }
                    @Override public GisObjectType getGisPolyType() { return GisObjectType.Point; }
                    @Override public android.graphics.Bitmap getIcon() { return myIcon; }
                    @Override public Paint.Style getStyle() { return Paint.Style.FILL_AND_STROKE; }
                    @Override public PolyType getShape() { return PolyType.circle; }
                    @Override public String getClickFlow() { return "wf_teammember"; }
                    @Override public DB_Context getObjectKeyHash() { return new DB_Context("år=[getCurrentYear()], lag = [getTeamName()], author ", myKeychain); }
                    @Override public String getStatusVariable() { return null; }
                    @Override public boolean isUser() { return true; }
                    @Override public String getName() { return myName; }
                    @Override public String getRawLabel() { return myName; }
                    @Override public String getCreator() { return ""; }
                    @Override public boolean useIconOnMap() { return true; }
                    @Override public boolean isVisible() { return true; }
                    @Override public List<Expressor.EvalExpr> getLabelExpression() { return Expressor.preCompileExpression(myName); }
                }, myKeychain, myLocation, null, null);
                myGisObject.setLabel(myName + " (me)");
                teamMembers.add(myGisObject);
            }

            _teamMemberGisObjects.postValue(teamMembers);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing team positions JSON: " + e.getMessage());
            _errorMessage.postValue("Error parsing team positions: " + e.getMessage());
            e.printStackTrace();
            _teamMemberGisObjects.postValue(new HashSet<>());
        }
    }

    // Helper method to get default icon based on timestamp. Never returns null (uses drawable->bitmap if decodeResource fails, e.g. for vectors).
    private Bitmap getDefaultTeamMemberIcon(long timestamp) {
        boolean anHourOld = Tools.isOverAnHourOld(System.currentTimeMillis() - timestamp);
        int resId = anHourOld ? R.drawable.person_away : R.drawable.person_active;
        String resName = (resId == R.drawable.person_active) ? "person_active" : "person_away";
        Bitmap b = BitmapFactory.decodeResource(getApplication().getResources(), resId);
        if (b != null) {
            return b;
        }
        android.graphics.drawable.Drawable d = ContextCompat.getDrawable(getApplication(), resId);
        if (d != null) {
            b = Tools.drawableToBitmap(d);
            if (b != null) {
                return b;
            }
        }
        // Last resort: try the other drawable
        resId = anHourOld ? R.drawable.person_active : R.drawable.person_away;
        resName = (resId == R.drawable.person_active) ? "person_active" : "person_away";
        b = BitmapFactory.decodeResource(getApplication().getResources(), resId);
        if (b != null) {
            return b;
        }
        d = ContextCompat.getDrawable(getApplication(), resId);
        if (d != null) b = Tools.drawableToBitmap(d);
        if (b != null) {
            return b;
        }
        Log.e(TAG, "getDefaultTeamMemberIcon: could not load any default icon");
        return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
    }


    private void processServerStatusResponse(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            int version = jsonObject.getInt("version");

            int storedVersion = globalPh.getI(PersistenceHelper.SERVER_VERSION_KEY);
            if (version != storedVersion) {
               // Log.d(TAG, "New version found: " + version + " stored version: " + storedVersion);
                globalPh.put(PersistenceHelper.SERVER_VERSION_KEY, version);
                globalPh.put(PersistenceHelper.SERVER_PENDING_UPDATE, true);
                _serverPendingUpdate.postValue(true);
            } else {
               // Log.d(TAG, "Server version not changed. Stored: " + storedVersion);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing server status JSON: " + e.getMessage());
            _errorMessage.postValue("Error parsing server status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void acknowledgeConfigUpdate() {
        Log.d(TAG, "Acknowledging configuration update: setting SERVER_PENDING_UPDATE to false.");
        globalPh.put(PersistenceHelper.SERVER_PENDING_UPDATE, false);
        _serverPendingUpdate.postValue(false);
    }

    private String getVolleyErrorString(VolleyError error) {
        if (error == null) return "Unknown Volley error.";
        if (error.networkResponse != null) {
            String statusCode = String.valueOf(error.networkResponse.statusCode);
            String responseBody = "";
            try {
                responseBody = new String(error.networkResponse.data, "utf-8");
            } catch (UnsupportedEncodingException e) {
                Log.e(TAG, "Failed to parse error response body: " + e.getMessage());
            }
            return "HTTP " + statusCode + (responseBody.isEmpty() ? "" : ": " + responseBody);
        }
        if (error.getMessage() != null) {
            return error.getMessage();
        }
        return "Unknown network error.";
    }

    public void clearErrorMessage() {
        _errorMessage.postValue(null);
    }

    @Override
    protected void onCleared() {
        if (gs != null) {
            gs.unregisterListener(TrackerListener.Type.USER);
        }
        super.onCleared();
    }
}
