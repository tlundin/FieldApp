package com.teraim.fieldapp.synchronization;


class PingMessage extends SyncMessage {

	private static final long serialVersionUID = 1595706891719878227L;
	/**
	 * 
	 */

	private final String partnerApp;
	private final String partner;
	private final String partnerTeam;
	private final String myTime;
	private final float appVersion;
    private final float softwareVersion;
	private final boolean requestAll;
		
	PingMessage(String name, String app, String team,
                float appVersion, float softwareVersion, boolean requestAll, String myTime) {
		this.partner = name;
		this.partnerApp = app;
		this.partnerTeam = team;
		this.appVersion=appVersion;
		this.softwareVersion=softwareVersion;
		this.requestAll=requestAll;
		this.myTime=myTime;
		
	}

	public String getPartner() {
		return partner;
	}
	
	public String getTime() {
		return myTime;
	}
	
	public String getPartnerTeam() {
		return partnerTeam;
	}
	
	public float getSoftwareVersion() {
		return softwareVersion;
	}
	
	public float getAppVersion() {
		return appVersion;
	}
	
	public boolean requestAll() {
		return requestAll;
	}

	public String getPartnerAppName() {
		return partnerApp;
	}
}
