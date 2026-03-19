package com.teraim.fieldapp.synchronization;

import java.io.Serializable;

public record SyncFailed(String reason) implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -9068482556518812383L;

}
