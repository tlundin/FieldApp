package com.teraim.fieldapp.dynamic.types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpinnerDefinition implements Serializable {
	private static final long serialVersionUID = -3915632410406656481L;
	private final Map<String,List<SpinnerElement>> myElements = new HashMap<String,List<SpinnerElement>>();


	public class SpinnerElement implements Serializable {
		private static final long serialVersionUID = 9162426573700197032L;
		public final String value;
        public final String opt;
        public final String descr;
		public final List<String> varMapping = new ArrayList<String>();
		public SpinnerElement(String val,String opt,String vars,String descr) {
			this.value = val;
			this.opt = opt.replace("\"", "");
			this.descr=descr.replace("\"", "");
			//this.opt = opt;
			//this.descr=descr;
			if (vars!=null&&!vars.isEmpty()) {
				String[] v = vars.split("\\|");
				Collections.addAll(varMapping, v);
			}
			//Log.e("vortex","SPINNER val "+val+" OPT: "+opt+" VARS "+vars+" DESC "+descr);
		}
	}
	
	public List<SpinnerElement> get(String spinnerId){
		return myElements.get(spinnerId.toLowerCase());
	}
	public void add(String id,List<SpinnerElement> l) {
		myElements.put(id.toLowerCase(), l);
	}
	public int size() {
		return myElements.size();
	}
}
