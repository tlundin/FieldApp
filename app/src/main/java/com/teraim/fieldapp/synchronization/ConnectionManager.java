package com.teraim.fieldapp.synchronization;

import android.content.Context;

import com.teraim.fieldapp.GlobalState;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Terje
 * 
 * ConnectionManager handles connections and provides one on request.
 * 
 */
public class ConnectionManager {

    private final Map<ConnectionType,ConnectionProvider> activeConnections = new HashMap<ConnectionType,ConnectionProvider>();
	
	
	public enum ConnectionType{
		bluetooth,
		mobilenet,
		wlan
	}
	
	//This is a singleton owned by GlobalState.
	public ConnectionManager(GlobalState gs) {

    }
	
	
	/**
	 * 
	 * @Connection Listener l
	 * 
	 * A request for a connection that does not specify type. If one is available, return first. Otherwise, create.
	 * Currently, only bluetooth is supported.
	 */
	public ConnectionProvider requestConnection(Context ctx) {
		if (activeConnections.isEmpty()) {
			activeConnections.put(ConnectionType.bluetooth,new BluetoothConnectionProvider(ctx));
			
		} 
		return activeConnections.get(ConnectionType.bluetooth);
	}

	public ConnectionProvider requestConnection(Context ctx, ConnectionType cType) {
		// Check if we already have an active connection of this type
		ConnectionProvider existingProvider = activeConnections.get(cType);
		if (existingProvider != null) {
			return existingProvider;
		}
		
		// Create a new provider for the requested type
		ConnectionProvider provider = null;
		switch (cType) {
		case bluetooth:
			provider = new BluetoothConnectionProvider(ctx); 
			break;
		case mobilenet:
			provider = new MobileRadioConnectionProvider(ctx); 
			break;
		case wlan:
			// WLAN not yet implemented
			break;
		default:
			// Unknown connection type
			break;
		}
		
		// Store the provider if it was created successfully
		if (provider != null) {
			activeConnections.put(cType, provider);
		}
		
		return provider;
	}
	
	

	public void releaseConnection(ConnectionProvider mConnection) {
		if (activeConnections != null && mConnection != null) {
			// Find and remove the connection by iterating through entries
			activeConnections.entrySet().removeIf(entry -> entry.getValue() == mConnection);
		}
	}
}
