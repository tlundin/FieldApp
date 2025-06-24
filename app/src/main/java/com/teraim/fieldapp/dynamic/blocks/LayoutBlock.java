package com.teraim.fieldapp.dynamic.blocks;


/**
 * Layoutblock
 * @author Terje
 *
 */
public  class LayoutBlock extends Block {

	private final String layoutDirection;
    private final String alignment;
	public String getLayoutDirection() {
		return layoutDirection;
	}
	public String getAlignment() {
		return alignment;
	}
	public LayoutBlock(String id,String lbl, String layoutDirection, String alignment) {
		this.layoutDirection = layoutDirection;
		this.alignment = alignment;
		this.blockId=id;
	}
}