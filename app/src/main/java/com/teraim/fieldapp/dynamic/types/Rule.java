package com.teraim.fieldapp.dynamic.types;

import android.content.Context;
import android.util.Log;

import com.teraim.fieldapp.dynamic.blocks.BlockCreateListEntriesFromFieldList;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;
import com.teraim.fieldapp.utils.Expressor;

import java.io.Serializable;
import java.util.List;

public class Rule implements Serializable {
    private static final String TAG = "Rule";


    /**
     *
     */
    private static final long serialVersionUID = -1965204853256767316L;
    private final String target;
    private final String errorMsg;
    private final String label;
    private final String id;
    private Expressor.EvalExpr condition;
    private Type myType;
    private int myTargetBlockId=-1;
    private final String conditionS=null;
    //Old rule engine for back compa.
    private boolean oldStyle = false;

    public Rule(String id, String ruleLabel, String target, String condition,
                String action, String errorMsg) {

        this.label=ruleLabel;
        this.id=id;
        this.target=target;
        //
        List<Expressor.EvalExpr> tmp = Expressor.preCompileExpression(condition);
        if (tmp!=null) {
            this.condition = tmp.get(0);
            //Log.d(TAG, "Bananas rule " + condition);
        } else
            Log.d(TAG, "Condition precompiles to null: "+condition);
        this.errorMsg=errorMsg;
        myType = Type.WARNING;
        if (action!=null && action.equalsIgnoreCase("Error_severity"))
            myType = Type.ERROR;
        try {
            myTargetBlockId = Integer.parseInt(target);
        } catch (NumberFormatException e) {}
    }

    private transient WF_Context ruleContext=null;
    private BlockCreateListEntriesFromFieldList myBlock=null;

    public void setTarget(WF_Context myContext, BlockCreateListEntriesFromFieldList bl) {
        ruleContext=myContext;
        myBlock=bl;

    }

    public String getTargetString() {
        return target;
    }


    public enum Type {
        ERROR,
        WARNING
    }



    //Execute Rule. Target will be colored accordingly.
    public Boolean execute() {
       if (condition!=null) {
    	   System.err.println("BANANA: CALING BOOL ANALYSIS WITH "+condition.toString());
       
        //Log.d(TAG, "Result of rule eval was: " + Expressor.analyzeBooleanExpression(condition));
           Log.d(TAG,"rule context: "+(ruleContext==null?"null":ruleContext)+" myBlock: "+(myBlock==null?"null":myBlock));
           if (ruleContext!=null && myBlock!=null) {
               WF_Static_List list = ruleContext.getList(myBlock.getListId());

               //target list for rule.
               List<List<String>> l = list.getRows();
               if (l!=null)
                   Log.d(TAG,"jaa "+myBlock.getBlockId());
               return Expressor.analyzeBooleanExpression(condition,l);

           }
           return Expressor.analyzeBooleanExpression(condition);
       } 
       return false;
    }

    public String getRuleText() {
        return errorMsg;
    }

    public String getRuleHeader() {
        return label;
    }

    public Type getType() {
        return myType;
    }

    public int getMyTargetBlockId() {
        return myTargetBlockId;
    }

    public String getId() {
        return id;
    }
    public String getCondition() {
        return conditionS;
    }

}
