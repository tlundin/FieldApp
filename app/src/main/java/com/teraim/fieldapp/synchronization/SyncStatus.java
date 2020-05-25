package com.teraim.fieldapp.synchronization;

public class SyncStatus extends SyncMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8715750047335168313L;

	private String myStatus;
	
public String getStatus() {
	return myStatus;
}
public void setStatus(String status) { 	myStatus = status;}

}

