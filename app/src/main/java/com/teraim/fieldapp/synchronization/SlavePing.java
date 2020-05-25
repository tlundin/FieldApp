package com.teraim.fieldapp.synchronization;



class SlavePing extends PingMessage {

	
	private static final long serialVersionUID = -8117606619445790131L;
	
	
	public SlavePing(String myName, String myApp, String myTeam,
			float appVersion, float softwareVersion, boolean requestAll, String myTime) {
		super(myName,myApp,myTeam,appVersion,softwareVersion,requestAll,myTime);
	}
}
