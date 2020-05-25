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
	private static final long serialVersionUID = -8275181338935274930L;
	LoggerI o;
	String blockId;
	
	
	public String getBlockId() {
		return blockId;
	}



}