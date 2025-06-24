package com.teraim.fieldapp.dynamic.blocks;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Context;
import com.teraim.fieldapp.dynamic.workflow_realizations.WF_Static_List;




public class AddEntryToFieldListBlock extends Block {
	private String target= null;
    private String namn= null;
    private String label=null;
    private String description=null;

	
	public AddEntryToFieldListBlock(String id,String namn,String target,
			String label, String description) {
		super();
		this.target = target;
		this.namn = namn;	
		this.label = label;
		this.description = description;
		this.blockId=id;
	}
	
	
	public void create(WF_Context myContext) {
		o = GlobalState.getInstance().getLogger();
		WF_Static_List myList = myContext.getList(target);

		if (myList==null) {
			o.addRow("");
			o.addRedText("List with name "+target+" was not found in AddEntryToFieldListBlock. Skipping");
		} else 
			
			myList.addFieldListEntry(namn,label,description);
	
	}
}
