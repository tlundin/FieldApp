package com.teraim.fieldapp.synchronization;

public interface ConnectionListener {

	enum ConnectionEvent {
		connectionGained,
		connectionBroken,
		connectionFailed,
		connectionClosedGracefully,
		connectionStatus, 
		connectionAttemptFailed, 
		restartRequired, 
		connectionFailedNoPartner, 
		connectionFailedNamedPartnerMissing
	}
	
	void handleMessage(Object o);
	void handleEvent(ConnectionEvent e);
	
	
}
