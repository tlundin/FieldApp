package com.teraim.fieldapp.synchronization;


class MasterPing extends PingMessage  {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7192503893294648765L;

	public MasterPing (String myName, String myApp, String myTeam,
	float appVersion, float softwareVersion, boolean requestAll, String myTime) {
		super(myName,myApp,myTeam,appVersion,softwareVersion,requestAll,myTime);
}

	
}

