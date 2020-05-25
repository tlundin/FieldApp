package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;
import android.view.View;

import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Listable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;


public abstract class WF_ListEntry extends WF_Widget implements Listable,Comparable<Listable> {

	//String keyVariable=null;
    private List<String> keyRow =null;
	String label = "";
	Variable myVar = null;
	private String spec;

	public abstract void refresh();
//	public abstract void refreshInputFields();

	WF_ListEntry(String id, View v, WF_Context ctx, boolean isVisible) {
		super(id,v,isVisible,ctx);
	}

	void setKey(Variable var) {
			myVar = var;
			keyRow = myVar.getBackingDataSet();
			//Log.d("nils","Calling setKeyRow for "+keyRow.toString());
			label = al.getEntryLabel(keyRow);
			spec = myVar.getId();
			//Log.d("vortex","Keyvar label: "+label);
	}
	@Override
	public String toString() {
		return "label: "+getLabel()+", ID: "+getId()+" myVar: "+myVar+" varID: "+spec;
	}

	@Override
	public String getSortableField(String columnId) {
		Log.d("filterz","In get sortable field. Keyrow:\n "+keyRow+" columnId: "+columnId+" res: "+al.getTable().getElement(columnId, keyRow));
		if (keyRow!=null && columnId!=null)
			return al.getTable().getElement(columnId, keyRow);
		else 
			return null;
	}

	@Override
	public String getKey() { 
		if (myVar == null) {
			Log.e("taxx","id: "+getId()+" label: "+label);
			return null;
		}
		else return myVar.getId();
	}

	
	@Override
	public boolean hasValue() {
		if (myVar == null)
			return false;
		else
			return myVar.getValue()!=null;
	}
	
	@Override
	public long getTimeStamp() {
		if (myVar == null)
			return -1;
		else {
			Long t = myVar.getTimeOfInsert();
			if (t == null)
				return -1;
			else 
				return t;		
		}
		
			
	}
		
	
	public String getLabel() {
		return label;
	}
	
	@Override
	public int compareTo(Listable other) {
		return this.getLabel().compareTo(other.getLabel());
	}

	public Map<String,String> getKeyChain() {
		return myVar.getKeyChain();
	}
	
	 public static class Comparators {

	        public static final Comparator<Listable> Alphabetic = new Comparator<Listable>() {
	            @Override
	            public int compare(Listable o1,Listable o2) {
	                return o1.getLabel().compareTo(o2.getLabel());
	            }
	        };
	        public static final Comparator<Listable> Time = new Comparator<Listable>() {
	            @Override
	            public int compare(Listable o1, Listable o2) {
	                return (int)(o2.getTimeStamp() - o1.getTimeStamp());
	            }
	        };
	        
	        public static final Comparator<Listable> Index = new Comparator<Listable>() {
	            @Override
	            public int compare(Listable o1, Listable o2) {
	            	String i1 = o1.getKeyChain().get("index");
	            	String i2 = o2.getKeyChain().get("index");
	            	if (i1 == null || i2==null)
	            		return -1;
	                return (int)( Float.parseFloat(i2)-Float.parseFloat(i1));
	            	
	            }
	        };
	     
	   }
	

}

