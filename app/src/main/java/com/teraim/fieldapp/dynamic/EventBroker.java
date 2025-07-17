package com.teraim.fieldapp.dynamic;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EventBroker {

	private final Map<EventType,List<EventListener>> eventListeners= new ConcurrentHashMap<EventType,List<EventListener>>();


    public EventBroker(Context ctx) {

	}

	public List<EventListener> getEventListeners(EventType et) {
		return eventListeners.get(et);
	}
	public void registerEventListener(EventType et,EventListener el) {

		List<EventListener> els = eventListeners.get(et);
		if (els==null) {
			els = new LinkedList<EventListener>();
			eventListeners.put(et, els);
		}
		if (els.contains(el))
			Log.d("vortex","registerEventListener discarded...listener already exist");
		else {
			els.add(el);
			//Log.d("nils","Added eventlistener for event "+et.name());
		}

	}

	public void onEvent(Event e) {
		Log.d("Frax","Received event "+e.getType().name()+" from "+e.getProvider());
		List<EventListener> els = eventListeners.get(e.getType());
		if (els==null) {
			Log.d("nils","No eventlistener exists for event "+e.getType().name());
		} else {
			//Log.d("grogg","sending event "+e.getType()+" to "+els.size()+" listeners:");
			//int i =0;
			for(EventListener el:els) {

				//Log.d("grogg","LISTENER NUMBER "+i+": "+el.getName());
				el.onEvent(e);
				//i++;
			}
		}

	}

	public void removeAllListeners() {
		Log.d("nils","remove all listeneres called on EVENTBROKER");
		eventListeners.clear();
	}

	public void removeEventListener(EventListener eventListener) {
		eventListeners.remove(eventListener);
	}

}
