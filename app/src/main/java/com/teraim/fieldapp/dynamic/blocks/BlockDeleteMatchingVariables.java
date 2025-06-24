package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.utils.Expressor.EvalExpr;

import java.util.List;
import java.util.Map;

import static com.teraim.fieldapp.utils.Expressor.preCompileExpression;

public class BlockDeleteMatchingVariables extends Block {

    private final String context;
    private String pattern;
	private List<EvalExpr> contextE;
	
	public BlockDeleteMatchingVariables(String id, String label, String target, String pattern) {
		this.blockId=id;
        this.context=target;
		this.pattern=pattern;
		if (pattern!=null && pattern.isEmpty())
			this.pattern = null;
		if (context !=null)
			contextE = preCompileExpression(context);
		System.err.println("Bananas: "+((contextE == null)?"null":contextE.toString()));
		//Log.d("vortex","BLOCKDELETEEXIT");
	}

	
	
	
	public void create(WF_Context ctx) {
		o = GlobalState.getInstance().getLogger();
		o.addRow("Now deleting all variables under Context: ["+context+"] and pattern "+pattern);
		//Log.d("papp","Now deleting all variables under Context: ["+context+"] and pattern "+pattern);
		DB_Context evaluatedContext = DB_Context.evaluate(contextE);
		if (evaluatedContext.isOk()) {
			Map<String, String> hash = evaluatedContext.getContext();
			if (hash == null) {
				Log.d("vortex","failed to build context..exiting delete matching variables");
				return;
			}
			//Delete database entries.
			//int entriesDeleted = GlobalState.getInstance().getDb().deleteAllVariablesUsingKey(hash);
			StringBuilder keyBuilder = new StringBuilder();
			boolean last = false;
			int i = 0;

			for (String key:hash.keySet()) {

				last = (i == hash.keySet().size()-1);
				keyBuilder.append(key).append("=").append(hash.get(key));
				if (!last)
					keyBuilder.append(",");

				i++;
			}
			int rowsAffected = GlobalState.getInstance().getDb().erase(keyBuilder.toString(),pattern);
			if (rowsAffected>0) {
				Log.d("vortex",("Deleteblock " + this.getBlockId() + " erased " + rowsAffected + " entries for [" + keyBuilder + "] and pattern [" + pattern + "]"));
				o.addRow("Deleteblock " + this.getBlockId() + " erased " + rowsAffected + " entries for [" + hash + "] and pattern [" + keyBuilder.toString() + "]");
				//Create sync entry.
				Log.d("vortex","Creating Erase sync entry for "+keyBuilder.toString());
				GlobalState.getInstance().getDb().insertEraseAuditEntry(keyBuilder.toString(),pattern);
			}
			else {
				Log.d("vortex","Deleteblock "+this.getBlockId()+" erased no entries for ["+hash+"] and pattern ["+keyBuilder.toString()+"]");
				o.addRow("");
				o.addYellowText("Deleteblock "+this.getBlockId()+" erased no entries for ["+hash+"] and pattern ["+keyBuilder.toString()+"]");

			}


			
		} else {
			o.addRow("");
			o.addRedText("The target context in Delete block contains an error. Error: "+evaluatedContext);
		}
		
	}	
	
			
		
		
}


