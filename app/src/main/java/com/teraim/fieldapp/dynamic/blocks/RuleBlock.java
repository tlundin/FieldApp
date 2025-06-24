package com.teraim.fieldapp.dynamic.blocks;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Rule;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;

import java.util.List;

public class RuleBlock extends Block {


	private final String ruleName, target, condition, action, errorMsg;
	private final Scope myScope;

	// The Rule object is a runtime object and MUST be transient.
	// It will be lazily created when first needed.
	private transient Rule r;

	private enum Scope {
		block,
		flow,
		both
	}

	public RuleBlock(String id, String ruleName, String target, String condition, String action, String errorMsg, String scopeStr) {
		this.blockId = id;
		this.ruleName = ruleName;
		this.target = target;
		this.condition = condition;
		this.action = action;
		this.errorMsg = errorMsg;

		Scope tempScope = Scope.flow;
		if (scopeStr != null && !scopeStr.isEmpty()) {
			try {
				tempScope = Scope.valueOf(scopeStr);
			} catch (IllegalArgumentException e) {
				Log.e("vortex", "Argument " + scopeStr + " not recognized. Defaults to scope flow");
			}
		}
		this.myScope = tempScope;
		// Do NOT initialize 'r' here. It will be null after construction.
	}

	// Lazy initializer for the transient Rule object.
	// This creates the Rule object on-demand the first time it's needed at runtime,
	// completely outside the serialization/deserialization process.
	private Rule getRule() {
		if (r == null) {
			Log.d("RuleBlock", "Lazily creating Rule object for block " + blockId);
			r = new Rule(blockId, ruleName, target, condition, action, errorMsg);
		}
		return r;
	}

	public void create(WF_Context myContext, List<Block> blocks) {
		// Use the getter to ensure the rule object is instantiated.
		Rule currentRule = getRule();

		Log.d("nils", "Create called in addRuleBlock, id " + blockId + " Target name: " + currentRule.getTargetString() + " my scope: " + myScope + " Target Block: " + currentRule.getMyTargetBlockId());
		o = GlobalState.getInstance().getLogger();

		if (myScope == Scope.flow || myScope == Scope.both) {
			myContext.addRule(currentRule);
		}

		if (currentRule.getMyTargetBlockId() != -1) {
			int index = findBlockIndex(currentRule.getTargetString(), blocks);
			if (index == -1) {
				o.addRow("");
				o.addRedText("target block for rule " + blockId + " not found (" + currentRule.getMyTargetBlockId() + ")");
				return;
			}
			Block b = blocks.get(index);
			if (b instanceof CreateEntryFieldBlock) {
				Log.d("vortex", "target ok");
				((CreateEntryFieldBlock) b).attachRule(currentRule);
			} else if (b instanceof BlockCreateListEntriesFromFieldList) {
				BlockCreateListEntriesFromFieldList bl = (BlockCreateListEntriesFromFieldList) b;
				currentRule.setTarget(myContext, bl);
			} else {
				Log.e("vortex", "target for rule doesnt seem correct: " + b.getClass() + " blId: " + currentRule.getTargetString());
				o = GlobalState.getInstance().getLogger();
				o.addRow("");
				o.addRedText("target for rule doesnt seem correct: " + b.getClass() + " blId: " + currentRule.getTargetString());
			}
		}
	}

	private int findBlockIndex(String tid, List<Block> blocks) {
		if (tid == null)
			return -1;
		for (int i = 0; i < blocks.size(); i++) {
			String id = blocks.get(i).getBlockId();
			if (id.equals(tid)) {
				return i;
			}
		}
		return -1;
	}
}
