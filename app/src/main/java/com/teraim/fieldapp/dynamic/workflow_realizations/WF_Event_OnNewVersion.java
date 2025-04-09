package com.teraim.fieldapp.dynamic.workflow_realizations;

import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;

public class WF_Event_OnNewVersion extends Event {
    public WF_Event_OnNewVersion(String serverUpdate) {
        super(serverUpdate, EventType.onNewMapData);
    }
}
