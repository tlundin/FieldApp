package com.teraim.fieldapp.dynamic.types;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class FixedVariable extends Variable implements Serializable {



	/**
	 * 
	 */
	private static final long serialVersionUID = -1532488393804167809L;
	//A Stored Variable has a key, value + timestamp/team/userId
	//It has a scope 

	//The key is a String[] of up to X separate values.

	//Current columnIds for keypart.

	/**
	 * 
	 */

	//use a private copy of the key chain.
	public FixedVariable(String name,String label,List<String> row,final Map<String,String>keyChain, GlobalState gs,String defaultValue,boolean isPreChecked) {
		super (name,label,row,keyChainCopy(keyChain),gs,"value",defaultValue,isPreChecked,null);
	}


	private static Map<String, String> keyChainCopy(Map<String,String>keyChain) {
		Log.d("vortex","in keychaincopy with "+keyChain.toString());
		if (keyChain == null)
			return null;
		else
			return new HashMap<String,String>(keyChain);
	}


	


}
