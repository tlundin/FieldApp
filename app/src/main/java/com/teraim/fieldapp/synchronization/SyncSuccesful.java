package com.teraim.fieldapp.synchronization;

class SyncSuccesful extends SyncMessage {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5372243649670548591L;
	private final SyncReport changesDone;
	
	private long timeStamp =-1;
	public SyncSuccesful(long lastEntrySent, SyncReport changesDone) {
		this.timeStamp=lastEntrySent;
		this.changesDone=changesDone;
	}
	
	public long getLastEntrySent() {
		return timeStamp;
	}
	
	public SyncReport getChangesDone() {
		return changesDone;
	}
}
