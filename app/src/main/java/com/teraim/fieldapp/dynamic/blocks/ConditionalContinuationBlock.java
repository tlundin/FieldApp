package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.utils.Expressor;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

import java.util.List;

public class ConditionalContinuationBlock extends Block {
	private static final String TAG = "ConditionalContinuationBlock";


	private final String elseID;
    private final String expr;
    private final List<EvalExpr>exprE;
	public String getFormula() {
		return expr;
	}
	public String getElseId() {
		return elseID;
	}
	public final static int STOP = 1,JUMP=2,NEXT = 3;
	private Integer lastEval = null;
	public ConditionalContinuationBlock(String id, List<String> varL,
			String expr, String elseBlockId) {
		this.blockId=id;
		this.expr=expr;
		this.exprE = Expressor.preCompileExpression(expr);
		this.elseID=elseBlockId;
	}
    public boolean isExpressionOk() {
    	return exprE!=null;
    }
    
	public boolean evaluate() {
		//assume fail
		int eval = STOP;
		if (exprE != null && exprE.size() == 1) {
			Boolean result = Expressor.analyzeBooleanExpression(exprE.get(0));
			if (result != null) {
				if (result) {
					Log.d(TAG, "Evaluates to true..continue to next block");
					eval = NEXT;
				} else {
					eval = JUMP;
					Log.d(TAG, "Evaluates to false..jump to " + elseID);
				}

			} else {
				Log.e("vortex", "Stopped on block " + this.getBlockId() + " due to null eval");
				eval = STOP;
			}
		}

		
		boolean ret = lastEval != null && eval != lastEval;
		lastEval = eval;
		return ret;
	}
	public Integer getCurrentEval() {
		return lastEval;
	}

	
}
