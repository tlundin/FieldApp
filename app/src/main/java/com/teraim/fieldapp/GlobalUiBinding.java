package com.teraim.fieldapp;

import com.teraim.fieldapp.dynamic.types.Workflow;
import com.teraim.fieldapp.gis.TrackerListener;
import com.teraim.fieldapp.ui.DrawerMenu;

/**
 * Bridge interface for wiring GlobalState/Core data into Activity/Fragment UI.
 *
 * Implementations of this interface are responsible for holding references to
 * the currently active UI components (such as the Start/MenuActivity and its
 * DrawerMenu) and for exposing a stable, Activity-agnostic API that the rest
 * of the codebase can call to trigger UI-related actions.
 *
 * By funnelling all Activity-coupled behavior through this interface, we can
 * keep GlobalStateCore free from UI dependencies and handle rotation by
 * re-attaching the current Activity instance when it is recreated.
 */
public interface GlobalUiBinding {

    // Lifecycle binding for the current foreground Activity and its UI chrome.
    void attachActivity(Start startActivity, DrawerMenu drawerMenu);

    void detachActivity();

    // High-level UI actions that used to be driven via GlobalState.getActivity().
    void setTitle(String wfLabel);

    void changePage(Workflow wf, String statusVar);

    // GPS / tracker listeners that are implemented by UI components.
    void registerListener(TrackerListener listener, TrackerListener.Type type);

    void unregisterListener(TrackerListener.Type type);

    void updateCurrentPosition(TrackerListener.GPS_State newState, int hash);
}

