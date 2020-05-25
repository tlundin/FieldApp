package com.teraim.fieldapp.dynamic.workflow_realizations;

import com.teraim.fieldapp.dynamic.types.Variable;

import java.util.List;

public abstract class WF_Static_List extends WF_List {


	List<List<String>> myRows;
	//protected String group;
	//How about using the Container's panel?? TODO
    WF_Static_List(String id, WF_Context ctx, List<List<String>> rows, boolean isVisible) {
		super(id,isVisible,ctx);	
		myRows = rows;
		//group = al.getFunctionalGroup(myRows.get(0));
		
	}

	public abstract boolean addVariableToEveryListEntry(String varSuffix,boolean displayOut,String format,boolean isVisible,boolean showHistorical,String initialValue);
	public abstract void addFieldListEntry(String listEntryID, 
			String label, String description);
	public abstract Variable addVariableToListEntry(String varNameSuffix,boolean displayOut,String targetField,
			String format, boolean isVisible,boolean showHistorical,String initialValue);

	public abstract void setRows(List<List<String>> rows);

    public List<List<String>> getRows() {return myRows;
    }
}
