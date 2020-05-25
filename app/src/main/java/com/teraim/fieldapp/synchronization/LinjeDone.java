package com.teraim.fieldapp.synchronization;


public class LinjeDone extends SyncMessage {


	private static final long serialVersionUID = 7034268349759371653L;
	public final String linjeId;
	public LinjeDone(String id) {
		this.linjeId = id;
	}
}
