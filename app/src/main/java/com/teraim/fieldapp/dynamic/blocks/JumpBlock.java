package com.teraim.fieldapp.dynamic.blocks;

public class JumpBlock extends Block {


	private final String nextBlockId;
	public JumpBlock(String id, String nextBlockId) {
		blockId=id;
		this.nextBlockId=nextBlockId;
	}
	public String getJumpTo() {
		return nextBlockId;
	}
}

