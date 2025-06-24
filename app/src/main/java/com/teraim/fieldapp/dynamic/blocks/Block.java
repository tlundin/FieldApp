package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.log.LoggerI;

import java.io.Serializable;

/**
 * Abstract base class Block
 * Marker class.
 * @author Terje
 *
 */
public abstract  class Block implements Serializable {

	protected final String s_type;
	protected transient LoggerI o;
	String blockId;

	protected Block() {
		this.s_type = this.getClass().getSimpleName();
	}

	public String getBlockId() {
		return blockId;
	}
}