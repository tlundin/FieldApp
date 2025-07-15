package com.teraim.fieldapp.viewmodels;

import android.app.Application;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.util.Log;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;


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
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
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
            Log.d(TAG, "Loaded " + allAvailableCustomNeedles.size() + " custom map needles.");
        } catch (Exception e) {
            Log.e(TAG, "Error loading all custom map needles: " + e.getMessage(), e);
        }
    }


    @Override
    public void gpsStateChanged(GPS_State signal) {
        this.latestSignal = signal;
    }

    public void sendAndReceiveTeamPositions() {
        if (!Connectivity.isConnected(getApplication())) {
            Log.d(TAG, "No internet connection, skipping sync.");
            _errorMessage.postValue("No internet connection.");
            if (activeRequestCount.get() == 0) {
                _isUpdating.postValue(false);
            }
            return;
        }

        if (activeRequestCount.get() > 0) {
            Log.d(TAG, "Skipping new sync cycle, " + activeRequestCount.get() + " requests already active.");
            return;
        }

        Log.d(TAG, "Initiating network calls...");
        _isUpdating.postValue(true);
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
                Log.d(TAG, "All requests completed for this cycle.");
                _isUpdating.postValue(false);
            }
        };

        // --- 1. POST My Position ---
        if (updateMyPosition) {
            JSONObject jsonBody = new JSONObject();
            try {
                JSONObject positionObject = new JSONObject();
                positionObject.put("easting", latestSignal.x);
                positionObject.put("northing", latestSignal.y);

                jsonBody.put("uuid", gs.getUserUUID());
                jsonBody.put("name", gs.getGlobalPreferences().get(PersistenceHelper.USER_ID_KEY));
                jsonBody.put("timestamp", latestSignal.time);
                jsonBody.put("icon", gs.getGlobalPreferences().getI(PersistenceHelper.MAP_NEEDLE_INDEX));
                jsonBody.put("position", positionObject);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating JSON for my position: " + e.getMessage());
                _errorMessage.postValue("Internal error: " + e.getMessage());
                _isUpdating.postValue(false);
                return;
            }

            final String requestBody = jsonBody.toString();
            final String SendMyPoisition = Constants.SynkStatusURI + "/position";

            activeRequestCount.incrementAndGet();
            StringRequest postMyPositionRequest = new StringRequest(Request.Method.POST, SendMyPoisition,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String response) {
                            Log.d(TAG, "My position posted successfully: " + response);
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

        // --- 2. GET Team Positions ---
        final String GetPoisitions = Constants.SynkStatusURI + "/positions";
        activeRequestCount.incrementAndGet();
        StringRequest getTeamStatusRequest = new StringRequest(Request.Method.GET, GetPoisitions,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d(TAG, "Team positions received: " + response);
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
                            Log.d(TAG, "Server status received: " + response.substring(0, Math.min(response.length(), 100)) + "...");
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

    // Method to process the raw team positions JSON into a Set of GisPointObjects
    private void processTeamPositionsResponse(String jsonString) {
        Set<GisPointObject> teamMembers = new HashSet<>();
        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            String teamName = gs.getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);
            String currentUserUUID = globalPh.get(PersistenceHelper.USERUUID_KEY); // Get current user's UUID


            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject memberJson = jsonArray.getJSONObject(i);

                String name = memberJson.getString("name");
                String uuid = memberJson.getString("uuid");
                long timestamp = memberJson.getLong("timestamp");
                JSONObject positionJson = memberJson.getJSONObject("position");
                double easting = positionJson.getDouble("easting");
                double northing = positionJson.getDouble("northing");

                // Optional map_needle_id from server
                String mapNeedleIdStr = null;
                if (memberJson.has("icon")) {
                    mapNeedleIdStr = memberJson.getString("icon");
                }

                // Skip yourself if your UUID matches
                if (uuid.equals(currentUserUUID)) {
                    Log.d(TAG, "Skipping myself: " + name);
                    continue;
                }
                // Skip if team name not set or empty
                if (teamName == null || teamName.isEmpty()) {
                    Log.d(TAG, "Skipping team member " + name + " - no team set.");
                    continue;
                }

                // Create a key for the workflow (as in GisImageView's original findMyTeam)
                final Map<String, String> keychain = new HashMap<>();
                keychain.put(DbHelper.YEAR, Constants.getYear());
                keychain.put("lag", teamName);
                keychain.put("author", name);
                // Also add UUID to keychain for potential use by GisPointObject itself for identification
                keychain.put("uuid", uuid);

                Location memberLocation = new SweLocation(easting, northing);

                // --- Determine the correct icon for this team member ---
                Bitmap finalIconBitmap = null;

                // Get the currently persisted icon ID for this user, if any. This represents the last successfully loaded icon ID.
                String persistedNeedleIdStr = globalPh.get("user_map_needle_" + uuid);

                // Determine the effective needle ID to use for this update cycle.
                // Server-provided ID takes precedence. If absent, use the persisted one.
                String effectiveNeedleIdStr = mapNeedleIdStr;
                if (effectiveNeedleIdStr == null || effectiveNeedleIdStr.isEmpty()) {
                    effectiveNeedleIdStr = persistedNeedleIdStr;
                }

                // Check if we can use the cached bitmap.
                // We can use it if:
                // 1. The UUID exists in the cache.
                // 2. The *effective* needle ID for this update matches the *persisted* needle ID (meaning the icon hasn't changed).
                if (teamMemberSpecificNeedleCache.containsKey(uuid) && Objects.equals(persistedNeedleIdStr, effectiveNeedleIdStr)) {
                    finalIconBitmap = teamMemberSpecificNeedleCache.get(uuid);
                    Log.d(TAG, "Using cached needle " + effectiveNeedleIdStr + " for team member " + name);
                }

                // If not found in cache (or cache was stale), load/derive the bitmap
                if (finalIconBitmap == null) {
                    if (effectiveNeedleIdStr != null && !effectiveNeedleIdStr.isEmpty()) {
                        try {
                            int needleIndex = Integer.parseInt(effectiveNeedleIdStr);
                            if (allAvailableCustomNeedles != null && needleIndex >= 0 && needleIndex < allAvailableCustomNeedles.size()) {
                                finalIconBitmap = allAvailableCustomNeedles.get(needleIndex);
                                teamMemberSpecificNeedleCache.put(uuid, finalIconBitmap); // Cache it for future use
                                Log.d(TAG, "Loaded and cached needle " + needleIndex + " for team member " + name);
                                // Persist the ID only if it came from server or was a valid persisted one
                                globalPh.put("user_map_needle_" + uuid, effectiveNeedleIdStr);
                            } else {
                                Log.w(TAG, "Icon ID " + effectiveNeedleIdStr + " for user " + name + " is out of bounds or invalid. Falling back.");
                                finalIconBitmap = getDefaultTeamMemberIcon(timestamp);
                                teamMemberSpecificNeedleCache.put(uuid, finalIconBitmap); // Cache the fallback
                                globalPh.remove("user_map_needle_" + uuid); // Remove potentially invalid persisted ID
                            }
                        } catch (NumberFormatException e) {
                            Log.d(TAG, "Icon ID for user " + name + " is not a valid integer: " + effectiveNeedleIdStr);
                            finalIconBitmap = getDefaultTeamMemberIcon(timestamp);
                            teamMemberSpecificNeedleCache.put(uuid, finalIconBitmap); // Cache the fallback
                            globalPh.remove("user_map_needle_" + uuid); // Remove potentially invalid persisted ID
                        }
                    } else {
                        // No server-provided or previously persisted ID, use default based on timestamp
                        finalIconBitmap = getDefaultTeamMemberIcon(timestamp);
                        teamMemberSpecificNeedleCache.put(uuid, finalIconBitmap); // Cache the fallback
                        globalPh.remove("user_map_needle_" + uuid); // Ensure no old ID is lingering if we default
                    }
                }

                // Sanity check: if finalIconBitmap is still null (shouldn't happen with the logic above, but good practice)
                if (finalIconBitmap == null) {
                    Log.e(TAG, "Failed to determine icon for team member " + name + ", using generic fallback.");
                    finalIconBitmap = getDefaultTeamMemberIcon(timestamp);
                    teamMemberSpecificNeedleCache.put(uuid, finalIconBitmap);
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
            Log.d(TAG, "Processed " + teamMembers.size() + " team members into GisObjects.");
            _teamMemberGisObjects.postValue(teamMembers);

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing team positions JSON: " + e.getMessage());
            _errorMessage.postValue("Error parsing team positions: " + e.getMessage());
            e.printStackTrace();
            _teamMemberGisObjects.postValue(new HashSet<>());
        }
    }

    // Helper method to get default icon based on timestamp
    private Bitmap getDefaultTeamMemberIcon(long timestamp) {
        boolean anHourOld = Tools.isOverAnHourOld(System.currentTimeMillis() - timestamp);
        return BitmapFactory.decodeResource(getApplication().getResources(), anHourOld ? R.drawable.person_away : R.drawable.person_active);
    }


    private void processServerStatusResponse(String jsonString) {
        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            int version = jsonObject.getInt("version");

            int storedVersion = globalPh.getI(PersistenceHelper.SERVER_VERSION_KEY);
            if (version != storedVersion) {
                Log.d(TAG, "New version found: " + version + " stored version: " + storedVersion);
                globalPh.put(PersistenceHelper.SERVER_VERSION_KEY, version);
                globalPh.put(PersistenceHelper.SERVER_PENDING_UPDATE, true);
                _serverPendingUpdate.postValue(true);
            } else {
                Log.d(TAG, "Server version not changed. Stored: " + storedVersion);
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
        super.onCleared();
    }
}
