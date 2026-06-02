package com.teraim.fieldapp;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility class responsible for persisting and restoring {@link SessionSnapshot}
 * instances. It is intentionally limited in scope and side-effect free beyond
 * basic SharedPreferences I/O, so it can be wired into different lifecycle
 * points without changing the behavior of the rest of the system.
 *
 * At this stage it is a helper only; callers decide when to save or load.
 */
public final class SessionPersistence {

    private static final String PREF_NAME = "session_state";
    private static final String KEY_SNAPSHOT_JSON = "snapshot_json";

    private SessionPersistence() {
        // no instances
    }

    public static void save(Context context, SessionSnapshot snapshot) {
        if (context == null || snapshot == null) {
            return;
        }
        JSONObject json = new JSONObject();
        try {
            json.put("schemaVersion", snapshot.getSchemaVersion());
            json.put("userUUID", snapshot.getUserUUID());
            json.put("teamId", snapshot.getTeamId());
            json.put("partnerId", snapshot.getPartnerId());
            json.put("deviceRole", snapshot.getDeviceRole());
            json.put("currentWorkflowId", snapshot.getCurrentWorkflowId());
            json.put("currentWorkflowStatusVar", snapshot.getCurrentWorkflowStatusVar());
            json.put("selectedGisObjectKey", snapshot.getSelectedGisObjectKey());
            json.put("pendingMapCenterLat", snapshot.getPendingMapCenterLat());
            json.put("pendingMapCenterLng", snapshot.getPendingMapCenterLng());
            json.put("timestampMillis", snapshot.getTimestampMillis());
        } catch (JSONException e) {
            // If snapshot can't be serialized, fail silently for now.
            return;
        }

        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SNAPSHOT_JSON, json.toString()).apply();
    }

    public static SessionSnapshot load(Context context) {
        if (context == null) {
            return null;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String jsonString = prefs.getString(KEY_SNAPSHOT_JSON, null);
        if (jsonString == null || jsonString.isEmpty()) {
            return null;
        }

        try {
            JSONObject json = new JSONObject(jsonString);
            SessionSnapshot snapshot = new SessionSnapshot();
            snapshot.setSchemaVersion(json.optInt("schemaVersion", 1));
            snapshot.setUserUUID(json.optString("userUUID", null));
            snapshot.setTeamId(json.optString("teamId", null));
            snapshot.setPartnerId(json.optString("partnerId", null));
            snapshot.setDeviceRole(json.optString("deviceRole", null));
            snapshot.setCurrentWorkflowId(json.optString("currentWorkflowId", null));
            snapshot.setCurrentWorkflowStatusVar(json.optString("currentWorkflowStatusVar", null));
            snapshot.setSelectedGisObjectKey(json.optString("selectedGisObjectKey", null));
            if (json.has("pendingMapCenterLat") && !json.isNull("pendingMapCenterLat")) {
                snapshot.setPendingMapCenterLat(json.getDouble("pendingMapCenterLat"));
            }
            if (json.has("pendingMapCenterLng") && !json.isNull("pendingMapCenterLng")) {
                snapshot.setPendingMapCenterLng(json.getDouble("pendingMapCenterLng"));
            }
            snapshot.setTimestampMillis(json.optLong("timestampMillis", 0L));
            return snapshot;
        } catch (JSONException e) {
            // Corrupted snapshot; ignore and let callers treat as "no snapshot".
            return null;
        }
    }

    public static void clear(Context context) {
        if (context == null) {
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_SNAPSHOT_JSON).apply();
    }
}

