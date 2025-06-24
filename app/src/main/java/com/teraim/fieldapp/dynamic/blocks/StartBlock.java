package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.utils.Expressor.EvalExpr;

import java.util.List;

import static com.teraim.fieldapp.utils.Expressor.preCompileExpression;

/**
 * Startblock.
 * @author Terje
 *
 */
public  class StartBlock extends Block {

	final private String workflowName;
	final private String[] args;
    private List<EvalExpr> contextE=null;

	public StartBlock(String id,String[] args,String wfn, String context) {
		workflowName = wfn;
		this.args = args;
        this.blockId=id;
		if (context !=null)
			contextE = preCompileExpression(context);
		//System.err.println("Bananas: "+((contextE == null)?"null":contextE.toString()));

	}

	public String getName() {
		return workflowName;
	}

	public String[] getArgs() {
		return args;
	}
	
	public List<EvalExpr> getWorkFlowContext() {
		return contextE;
	}
}