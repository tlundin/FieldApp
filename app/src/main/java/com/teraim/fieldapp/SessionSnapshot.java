package com.teraim.fieldapp;

/**
 * Lightweight, serialization-friendly representation of the current logical
 * user/session state. This is persisted (via {@link SessionPersistence}) and
 * used to rebuild in-memory {@link SessionState} / {@link GlobalState} after
 * process death or a cold start.
 *
 * The class itself is a pure data container; persistence and reconstruction
 * behaviour are implemented by {@link SessionPersistence} and the startup
 * flow in {@code StartupFragment}     
 */
public final class SessionSnapshot {

    // Simple schema versioning to allow future evolution.
    private int schemaVersion = 1;

    // User / device identity
    private String userUUID;
    private String teamId;
    private String partnerId;
    private String deviceRole;

    // Workflow position
    private String currentWorkflowId;
    private String currentWorkflowStatusVar;

    // GIS / map-related hints
    private String selectedGisObjectKey;
    private Double pendingMapCenterLat;
    private Double pendingMapCenterLng;

    // Timestamp of when this snapshot was taken
    private long timestampMillis;

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(int schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getUserUUID() {
        return userUUID;
    }

    public void setUserUUID(String userUUID) {
        this.userUUID = userUUID;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getPartnerId() {
        return partnerId;
    }

    public void setPartnerId(String partnerId) {
        this.partnerId = partnerId;
    }

    public String getDeviceRole() {
        return deviceRole;
    }

    public void setDeviceRole(String deviceRole) {
        this.deviceRole = deviceRole;
    }

    public String getCurrentWorkflowId() {
        return currentWorkflowId;
    }

    public void setCurrentWorkflowId(String currentWorkflowId) {
        this.currentWorkflowId = currentWorkflowId;
    }

    public String getCurrentWorkflowStatusVar() {
        return currentWorkflowStatusVar;
    }

    public void setCurrentWorkflowStatusVar(String currentWorkflowStatusVar) {
        this.currentWorkflowStatusVar = currentWorkflowStatusVar;
    }

    public String getSelectedGisObjectKey() {
        return selectedGisObjectKey;
    }

    public void setSelectedGisObjectKey(String selectedGisObjectKey) {
        this.selectedGisObjectKey = selectedGisObjectKey;
    }

    public Double getPendingMapCenterLat() {
        return pendingMapCenterLat;
    }

    public void setPendingMapCenterLat(Double pendingMapCenterLat) {
        this.pendingMapCenterLat = pendingMapCenterLat;
    }

    public Double getPendingMapCenterLng() {
        return pendingMapCenterLng;
    }

    public void setPendingMapCenterLng(Double pendingMapCenterLng) {
        this.pendingMapCenterLng = pendingMapCenterLng;
    }

    public long getTimestampMillis() {
        return timestampMillis;
    }

    public void setTimestampMillis(long timestampMillis) {
        this.timestampMillis = timestampMillis;
    }
}

