package com.teraim.fieldapp;

import android.util.Log;

import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.gis.TrackerListener;
import com.teraim.fieldapp.ui.DrawerMenu;

/**
 * Default implementation of {@link GlobalUiBinding} that encapsulates all
 * Activity- and UI-specific behavior that was previously hosted directly
 * in {@link GlobalState}. For now this class is not yet wired into the
 * existing call sites; it simply mirrors the current behavior so we can
 * gradually migrate without changing semantics.
 */
public class GlobalUiBindingImpl implements GlobalUiBinding {

    private static final String TAG = "GlobalUiBinding";

    private DrawerMenu drawerMenu;

    private TrackerListener mapListener;
    private TrackerListener menuListener;
    private TrackerListener userListener;

    // Hash used to guard against late GPS disable events from stale listeners.
    private int oHash = -1;

    public GlobalUiBindingImpl(Start startActivity, DrawerMenu drawerMenu) {
        // The Start reference is intentionally not stored long-term; navigation
        // will always resolve the current live Start instance via the static
        // accessor on the Start class. We only keep the DrawerMenu reference.
        this.drawerMenu = drawerMenu;
    }

    @Override
    public void attachActivity(Start startActivity, DrawerMenu drawerMenu) {
        this.drawerMenu = drawerMenu;
    }

    @Override
    public void detachActivity() {
        this.drawerMenu = null;
    }

    @Override
    public void setTitle(String wfLabel) {
        Start current = Start.getCurrentInstance();
        if (current != null && !current.isFinishing() && !current.isDestroyed()) {
            current.setTitle(wfLabel);
        } else {
            Log.w(TAG, "setTitle called but no active Start instance is available");
        }
    }

    @Override
    public void changePage(Workflow wf, String statusVar) {
        Start current = Start.getCurrentInstance();
        if (current == null || current.isFinishing() || current.isDestroyed()) {
            Log.w(TAG, "changePage: no active Start instance available for navigation");
            return;
        }
        try {
            current.changePage(wf, statusVar);
        } catch (IllegalStateException e) {
            Log.e(TAG, "changePage failed due to FragmentManager state", e);
        }
    }

    @Override
    public void registerListener(TrackerListener listener, TrackerListener.Type type) {
        switch (type) {
            case MAP:
                mapListener = listener;
                break;
            case MENU:
                menuListener = listener;
                break;
            case USER:
                userListener = listener;
                break;
        }
    }

    @Override
    public void unregisterListener(TrackerListener.Type type) {
        switch (type) {
            case MAP:
                mapListener = null;
                break;
            case MENU:
                menuListener = null;
                break;
            case USER:
                userListener = null;
                break;
        }
    }

    @Override
    public void updateCurrentPosition(TrackerListener.GPS_State newState, int hash) {
        // Preserve the same semantics as GlobalState.updateCurrentPosition.
        if (newState.state == TrackerListener.GPS_State.State.enabled) {
            oHash = hash;
        } else if (newState.state == TrackerListener.GPS_State.State.disabled && hash != oHash) {
            return;
        }
        if (newState.state == TrackerListener.GPS_State.State.newValueReceived && hash != oHash) {
            Log.e("GPS", "received location from previous map");
        }

        if (menuListener != null) {
            menuListener.gpsStateChanged(newState);
        }
        if (userListener != null) {
            userListener.gpsStateChanged(newState);
        }
        if (mapListener != null) {
            mapListener.gpsStateChanged(newState);
        }
    }

    // Convenience helpers that mirror the previous GlobalState API; these can
    // be used during migration and eventually removed or narrowed.
    public DrawerMenu getDrawerMenu() {
        return drawerMenu;
    }

    public void setDrawerMenu(DrawerMenu drawerMenu) {
        this.drawerMenu = drawerMenu;
    }

    public Start getStartActivity() {
        return Start.getCurrentInstance();
    }
}

