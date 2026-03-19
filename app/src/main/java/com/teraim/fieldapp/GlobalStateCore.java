package com.teraim.fieldapp;

import android.content.Context;
import android.content.Intent;

import com.android.volley.RequestQueue;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.SpinnerDefinition;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.synchronization.ConnectionManager;
import com.teraim.fieldapp.synchronization.SyncMessage;
import com.teraim.fieldapp.non_generics.StatusHandler;
import com.teraim.fieldapp.utils.BackupManager;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;

import java.io.File;
import java.util.Map;
import java.util.Set;

import okhttp3.OkHttpClient;

/**
 * Abstraction over the application-wide, non-UI-dependent parts of GlobalState.
 *
 * This interface is intentionally conservative: it only exposes state and
 * operations that should be safe to keep for the entire application process
 * lifetime and that do not depend on Activity, Fragment or View instances.
 *
 * Initial implementation will be provided by the existing GlobalState class.
 */
public interface GlobalStateCore {

    // Environment / context
    Context getContext();
    PersistenceHelper getGlobalPreferences();
    PersistenceHelper getPreferences();
    DbHelper getDb();

    // Configuration / model wiring
    VariableConfiguration getVariableConfiguration();
    SpinnerDefinition getSpinnerDefinitions();
    Map<String, Workflow> getWfs();
    String[] getWorkflowNames();
    String[] getWorkflowLabels();
    Workflow getWorkflow(String id);
    Workflow getWorkflowFromLabel(String label);

    // Caches & context
    VariableCache getVariableCache();
    void setDBContext(DB_Context context);

    Set<String> getProvYtaTypes();
    void setProvYtaTypes(Set<String> provYtaTypes);

    // User / device identity
    String getUserUUID();
    String getMyTeam();
    boolean isMaster();
    boolean isSolo();
    boolean isSlave();
    GlobalState.ErrorCode checkSyncPreconditions();
    String getImgMetaFormat();
    String getMyPartner();
    void setMyPartner(String partner);

    // Networking / sync / backup
    ConnectionManager getConnectionManager();
    BackupManager getBackupManager();
    StatusHandler getStatusHandler();
    RequestQueue getRequestQueue();
    OkHttpClient getHTTPClient();
    SyncMessage getOriginalMessage();
    void setSyncMessage(SyncMessage message);

    // GIS & domain selections
    com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject getSelectedGop();
    void setSelectedGop(com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject go);

    Map<String, GlobalState.TeamPosition> getTeamPositions();
    boolean insertTeamPositions(String jsonResponse);

    // Map-related hints
    void setPendingMapCenter(double lat, double lng);
    double[] getAndClearPendingMapCenter();

    // Logging & files
    LogRepository getLogger();
    CharSequence getLogTxt();
    File getCachedFileFromUrl(String fileName);

    // Event dispatch (non-UI-specific)
    void sendSyncEvent(Intent intent);
    void sendEvent(String action);
}

