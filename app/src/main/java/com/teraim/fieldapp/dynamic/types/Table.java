package com.teraim.fieldapp.dynamic.types;

import android.annotation.SuppressLint;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.log.LogRepository;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

//SparseArray is not serializable so we cannot use it...so turn off warning.
@SuppressLint("UseSparseArrays")

public class Table implements Serializable {
	private static final String TAG = "Table";


	private static final long serialVersionUID = 1183209171210448314L;
	//The table is a Map of key=Header,value=List of Data.
	private final Map<String,List<String>> colTable=new TreeMap<String,List<String>>(String.CASE_INSENSITIVE_ORDER);
	private final Map<Integer,List<String>> rowTable=new HashMap<Integer,List<String>>();
	private final Map<String,List<String>> nameToRowMap=new TreeMap<String,List<String>>(String.CASE_INSENSITIVE_ORDER);
	private final ArrayList<String> keyParts = new ArrayList<String>();
	//Immutable list of Required columns.
	private int rowCount=0,keyChainIndex =-1;
	private List<String> myColumns;
	private String previousKeyChain = null;
	private int variableIdIndex=-1;


	
	
	//Keychain + name = primary key.
	public Table (List<String> columnNames,int keyChainIndex,int nameIndex) {
		assert(columnNames!=null);
		for(String key:columnNames) 
			colTable.put(key, new ArrayList<String>());		
		this.keyChainIndex = keyChainIndex;
		this.variableIdIndex = nameIndex;
		myColumns = columnNames;
		Log.d(TAG,"Created table with column names: "+myColumns.toString());
	}

	public enum ErrCode {
		tooManyColumns,
		tooFewColumns,
		keyError,
		ok
	}

    public List<String> getColumnHeaders() {
		return myColumns;
	}
	
	
	public ErrCode addRow(List<String> rowEntries) {
		int index=0;
		if (rowEntries == null||rowEntries.size()==0)
			return ErrCode.tooFewColumns;
		int size = rowEntries.size();		
		if (size > myColumns.size()) {
			Log.e("nils","TOO MANY: ");
			Log.e("nils","RowEntries: "+rowEntries.toString());
			Log.e("nils","myColumns: "+myColumns.toString());
			Log.e("nils","RowEntries s: "+rowEntries.size());
			Log.e("nils","myColumns s: "+myColumns.size());
			
			return ErrCode.tooManyColumns;		
		}
		//columnmap
		for(String entry:rowEntries) 
			colTable.get(myColumns.get(index++)).add(entry);
		//rowmap
		rowTable.put(rowCount++, rowEntries);	
		//keymap.
		nameToRowMap.put(rowEntries.get(variableIdIndex),rowEntries);
		//Check keychain and add 
		if (rowEntries.size()<keyChainIndex) {
			Log.e("nils","row length shorter than key index");
			return ErrCode.tooFewColumns;
		}
		String keyChain = rowEntries.get(keyChainIndex);
		
		
		//check if any new key
		//if equal to previous, skip
		if (!keyChain.equals(previousKeyChain)) {
		String[] keys = keyChain.split("\\|");
		if (keys == null) {
			Log.e("nils","KeyChain null after split");
			return ErrCode.keyError;
		}
		for (String key:keys) {
			if (!keyParts.contains(key)&&key.trim().length()>0) {
				//Log.d(TAG,"found new key part: "+key);
				//Add to existing Database model.
				keyParts.add(key.trim());
			}
        }
		//no need to check this one again.
		previousKeyChain = keyChain;
		} 
		
		return ErrCode.ok;
	}
	//TODO: Change for more complex keys.
	final static String VAR_KEY_COL = VariableConfiguration.Col_Variable_Name;


	public List<String> getColumn(String columnName) {
		return colTable.get(columnName);
	}
	

	public List<String> getRow(Integer row) {
		return rowTable.get(row);
	}

	public List<List<String>> getRowsContaining(String columnName, String pattern) {
		Log.d(TAG,"Trying to find rows matching column "+columnName+" and pattern "+pattern);
	
		List<List<String>> ret = null;
		List<String> column = colTable.get(columnName);
		if(column!=null && pattern!=null && column.size()>0) {
			pattern.trim();
			ret = new ArrayList<List<String>>();
			for(int i = 0;i<column.size();i++) {
				//Log.d(TAG,"i: "+i+" col: "+column.get(i));
				if (column.get(i).equals(pattern)||column.get(i).matches(pattern)) {
					ret.add(rowTable.get(i));
				}
			}
			if (ret!=null)
				Log.d(TAG,"Returning "+ret.size()+" rows in getRows(Table)");
				
		} 
		return ret;
	}
	
	public List<List<String>> getRowsStartingWith(String columnName, String prefix) {
		//Log.d(TAG,"Trying to find rows matching column "+columnName+" and pattern "+pattern);
	
		List<List<String>> ret = null;
		List<String> column = colTable.get(columnName);
		if(column!=null && prefix!=null) {
			prefix.trim();
			for(int i = 0;i<column.size();i++) {
				//Log.d(TAG,"i: "+i+" col: "+column.get(i));
				if (column.get(i).startsWith(prefix)) {
					if (ret == null)
						ret = new ArrayList<List<String>>();
					ret.add(rowTable.get(i));
				}
			}
//			if (ret!=null)
//				Log.d(TAG,"Returning "+ret.size()+" rows in getRows(Table)");
		} 
		return ret;
	}

	public int getColumnIndex(String c) {
		//Log.d(TAG,"My columns: "+myColumns.toString());
		for (int i=0;i<myColumns.size();i++)
			if (c.equalsIgnoreCase(myColumns.get(i)))
				return i;
		return -1;
	}
	
	public List<String> getRowFromKey(String key) {
		if (key == null) {
			Log.e("nils","key was null in getRowFromKey (Table.java)");
			return null;
		}
		return nameToRowMap.get(key.trim());
	}

	public List<String> getRowContaining(String columnName,
			String key) {
		if (key==null||columnName==null) {
			Log.e("nils","key or column was null in getRowContaining (Table.java)");
			return null;
		}
		List<String> column = colTable.get(columnName.trim());

		for(int i = 0;i<column.size();i++) {
			if (column.get(i).equals(key.trim())) {
				//Log.d(TAG,"found master variable "+key+" in Artlista");
				return rowTable.get(i);
			}
			//Log.d(TAG,"nomatch: "+column.get(i)+" "+key+" l1: "+column.get(i).length()+" "+"l2:"+key.length());
		}
		
		Log.e("nils","Did not find master variable "+key+" in Artlista column: "+columnName);
		return null;
	}

	

	public String getElement(String columnName,List<String> row) {
		String result = null;
		int index = getColumnIndex(columnName);
		if (index !=-1) {	
			if (row.size()>index)
				result = row.get(index);
			//Log.d(TAG,"found field "+columnName+": "+result+" in class Table");
		} else {
			LogRepository.getInstance().addCriticalText("Did not find column named "+columnName);
			Log.e("nils","Did NOT find field ["+columnName+"] in class Table. Columns available:");
			for (int i=0;i<myColumns.size();i++)
				Log.e("vortex","["+myColumns.get(i)+"]");
		}
		return result;
	}

	public ArrayList<String> getKeyParts() {
		return keyParts;
	}

	public void printTable(String varName) {
		for (Integer key:rowTable.keySet()) {
			List<String> l = rowTable.get(key);
			//if (GlobalState.getInstance().getVariableConfiguration().getVarName(l).)
			Log.d(TAG,"Row: "+key+":");
			Log.d(TAG,l.toString());
		}

	}
	public void printTable() {
		Map<Integer, List<String>> x = rowTable;
		Log.d(TAG,myColumns.toString());
		for (Integer key:rowTable.keySet()) {
			List<String> l = rowTable.get(key);
			Log.d(TAG,"Row: "+key+":");
			Log.d(TAG,l.toString());
		}
	}
	
	
	public Map<String,List<String>> getColumnTable() {
		return colTable;
	}

}
