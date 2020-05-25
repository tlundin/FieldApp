package com.teraim.fieldapp.synchronization;


class EnvelopedMessage extends SyncMessage {

	/**
	 * Generic Message that can be used by Customized templates to send messages to other units.
	 * See Example of use in LinjePortalTemplate for events LinjeStarted and LinjeDone.
	 */
	private static final long serialVersionUID = 3247832794025535224L;

	private final SyncMessage aMessage;

	public EnvelopedMessage(SyncMessage aMessage) {
		super();
		this.aMessage = aMessage;
	}
	
	//Template must know what kind of object was sent.
	public SyncMessage getMessage() {
		return aMessage;
	}
}
