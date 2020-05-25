package com.teraim.fieldapp.dynamic.blocks;

/**
 * Created by Terje on 2017-04-23.
 */


public class BlockGoSub extends Block {

    private static final long serialVersionUID = -8381560302516157092L;
    private final String target;
    public BlockGoSub(String id, String target) {
        blockId=id;
        this.target=target;
    }

    public String getTarget() {
        return target;
    }
}
