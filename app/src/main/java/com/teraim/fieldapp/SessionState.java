package com.teraim.fieldapp;

import com.teraim.fieldapp.dynamic.types.DB_Context;

/**
 * In-memory representation of the current logical user/session state.
 *
 * This is a richer view than {@link SessionSnapshot}: it can hold direct
 * references to domain objects (such as DB_Context) that should not be
 * serialized directly but are convenient to work with while the process is
 * alive. A SessionState can be projected to and from a {@link SessionSnapshot}.
 *
 * At this stage, SessionState is a simple data holder; construction and usage
 * will be wired in gradually to avoid changing existing behaviour.
 */
public final class SessionState {

    // User / device identity and role
    private String userUUID;
    private String teamId;
    private String partnerId;
    private String deviceRole;

    // Current workflow position
    private String currentWorkflowId;
    private String currentWorkflowStatusVar;

    // Current database context
    private DB_Context currentDbContext;

    // GIS / map-related state
    private String selectedGisObjectKey;
    private Double pendingMapCenterLat;
    private Double pendingMapCenterLng;

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

    public DB_Context getCurrentDbContext() {
        return currentDbContext;
    }

    public void setCurrentDbContext(DB_Context currentDbContext) {
        this.currentDbContext = currentDbContext;
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

    /**
     * Create a lightweight, serialization-friendly snapshot from this state.
     * This method does not perform any I/O or persistence.
     */
    public SessionSnapshot toSnapshot() {
        SessionSnapshot snapshot = new SessionSnapshot();
        snapshot.setUserUUID(userUUID);
        snapshot.setTeamId(teamId);
        snapshot.setPartnerId(partnerId);
        snapshot.setDeviceRole(deviceRole);
        snapshot.setCurrentWorkflowId(currentWorkflowId);
        snapshot.setCurrentWorkflowStatusVar(currentWorkflowStatusVar);
        snapshot.setSelectedGisObjectKey(selectedGisObjectKey);
        snapshot.setPendingMapCenterLat(pendingMapCenterLat);
        snapshot.setPendingMapCenterLng(pendingMapCenterLng);
        snapshot.setTimestampMillis(System.currentTimeMillis());
        return snapshot;
    }

    /**
     * Populate this SessionState instance from a previously captured snapshot.
     * Domain-specific reconstruction (such as turning keys into DB_Context
     * instances) will be handled by higher-level code.
     */
    public void restoreFromSnapshot(SessionSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        this.userUUID = snapshot.getUserUUID();
        this.teamId = snapshot.getTeamId();
        this.partnerId = snapshot.getPartnerId();
        this.deviceRole = snapshot.getDeviceRole();
        this.currentWorkflowId = snapshot.getCurrentWorkflowId();
        this.currentWorkflowStatusVar = snapshot.getCurrentWorkflowStatusVar();
        this.selectedGisObjectKey = snapshot.getSelectedGisObjectKey();
        this.pendingMapCenterLat = snapshot.getPendingMapCenterLat();
        this.pendingMapCenterLng = snapshot.getPendingMapCenterLng();
    }
}

