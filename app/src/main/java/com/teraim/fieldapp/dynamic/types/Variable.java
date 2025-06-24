package com.teraim.fieldapp.dynamic.types;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.CombinedRangeAndListFilter;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.DbHelper.Selection;
import com.teraim.fieldapp.utils.FilterFactory;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;
import com.teraim.fieldapp.utils.Tools.Unit;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Terje
 * Variable Class. Part of Vortex core.
 * Copyright Teraim Holding. 
 * No modification and use allowed before prior permission. 
 */

public class Variable implements Serializable {

	private static final long serialVersionUID = 6239650487891494128L;
	//private static final String DEFAULT_LIMIT_FOR_INTEGER = Integer.MIN_VALUE+"-"+Integer.MAX_VALUE;


	private Map<String, String> keyChain = new HashMap <String,String>();
	private Map<String,String> histKeyChain=null;
	//String value=null;
	protected String name=null;
	private DataType myType=null;
	private String myValue=null;
	protected final String[] myValueColumn = new String[1];
	protected Selection mySelection=null;
	private String myLabel = null;
	private Set<String> myRules = null;
	final DbHelper myDb;
	private List<String> myRow;
	private String myStringUnit;
	private boolean isSynchronized = true;
	boolean unknown=true;
	private boolean isKeyVariable = false;
	private final String realValueColumnName;
	private Selection histSelection;
	private final GlobalState gs;
	private boolean iAmOutOfRange=false;
	private String myHistory = null;
	private boolean historyChecked = false;
	Long timeStamp=null;
	private final VariableConfiguration al;
	private String myDefaultValue=null;
	private CombinedRangeAndListFilter myFilter=null;
	private boolean usingDefault = false;

	public enum DataType {
		numeric,bool,list,text,existence,auto_increment, array, decimal
	}

	public String getValue() {
		if (unknown) {
			//update keyhash.
			Log.d("nils","fetching: "+System.currentTimeMillis()+" var: "+this.getId());
			myValue = myDb.getValue(name,mySelection,myValueColumn);
			Log.d("zzzz","myValue in get "+myValue);
			//Variable doesnt exist in database
			if (myType == DataType.auto_increment && myValue==null) {
				//find value for global auto_inc counter in persistent memory.
				//increment global counter.
				int globalAC = gs.getPreferences().getI(PersistenceHelper.GLOBAL_AUTO_INC_COUNTER);
				Log.d("vortex","global AC was "+globalAC);
				gs.getPreferences().put(PersistenceHelper.GLOBAL_AUTO_INC_COUNTER, globalAC+1);
				//set the Value 				
				setValue(globalAC+1+"");
			}
			else if (myValue==null && myDefaultValue !=null) {
				Log.d("nils","Value null! Using default: "+myDefaultValue);
				//use default value.
				myValue = myDefaultValue;
				usingDefault = true;
				//Log.d("brox","GetValue: Default now true for "+this.getId()+" Value: "+myDefaultValue);

			}
			unknown = false;
//			Log.d("nils","done:     "+System.currentTimeMillis()+" var: "+this.getId());
			//refreshRuleState();
		}
		//Log.d("nils","Getvalue returns "+myValue+" for "+this.getId());
		if (myType == DataType.bool && myValue !=null )
			return boolValue(myValue);
		return myValue;
	}

	public String getHistoricalValue() {
		if (!historyChecked) {

			if (keyChain == null || keyChain.get(VariableConfiguration.KEY_YEAR)==null) {
				Log.d("nils","historical keychain is null. Should contain year at least.");
				return null;
			}
			if (histKeyChain == null) {
				histKeyChain = new HashMap<String,String>(keyChain);
				histKeyChain.put(VariableConfiguration.KEY_YEAR, Constants.HISTORICAL_TOKEN_IN_DATABASE);
				Log.d("nils","My historical keychain: "+histKeyChain.toString()+" my name: "+name);
				histSelection = myDb.createSelection(histKeyChain,name);
			}

			myHistory= myDb.getValue(name,histSelection,myValueColumn);

			historyChecked = true;

		}
		if (myHistory !=null && myType == DataType.bool)
			return boolValue(myHistory);
		if (myHistory !=null)
			Log.d("vortex","getHistoricalValue returns "+myHistory+" for "+this.getId());
		return myHistory;
	}


	public String getLabel() {
		if (myLabel==null && myRow!=null)
			myLabel = al.getVarLabel(myRow);
		return myLabel;
	}

	public Set<String> getRules() {
		return myRules;
	}
	/*
	public StoredVariableData getAllFields() {
		return myDb.getVariable(name,mySelection);
	}
	 */

	//return true if change.

	private String boolValue(String myValue) {
		if (myValue.equals("1"))
			return "true";
		else if (myValue.equals("0"))
			return "false";

		return myValue;
	}

	public boolean setValue(String value) {
		//Log.d("nils","In SetValue for variable "+this.getId()+" New val: "+value+" existing val: "+myValue+" unknown? "+unknown+" using default? "+usingDefault);
		//Null values are not allowed in db.
		if (value==null)
			return false;
		if (!usingDefault && myValue != null) {
			if (myValue.equals(value))
				return false;
			if (this.getType()==DataType.bool) {
				if (value.equals("true")&&myValue.equals("1"))
					return false;
				if (value.equals("false")&&myValue.equals("0"))
					return false;
			}
		}
		if (this.iAmOutOfRange) {
			Log.d("vortex","Out of range. Value not stored!");
			return false;
		}

		//Log.e("nils","Var: "+this.getId()+" old Val: "+myValue+" new Val: "+value+" this var hash#"+this.hashCode()+" this hash:"+this.getKeyChain()+" current hash: "+gs.getVariableCache().getContext()+"using default: "+usingDefault);
		value = Tools.removeStartingZeroes(value);
		myValue = value;
		//Log.d("zzzz","myValue in setValue "+myValue);
		//Remove any .xx if numeric or list
		if ((this.getType()==DataType.numeric || this.getType()==DataType.list) && value.endsWith(".0")) {
			value = (int)(Float.parseFloat(value))+"";
			Log.d("vortex","chopped of .0 in setvalue: "+value);
		}
		if (this.getType()==DataType.bool) {

			if (value.equals("true"))
				value = "1";
			else if (value.equals("false"))
				value = "0";
			else {
				Log.e("vortex","This is not a boolean value: "+value);
				return false;
			}

		}
		//will change keyset as side effect if valueKey variable.
		//reason for changing indirect is that old variable need to be erased. 
		insert(value,isSynchronized);
		//If rules attached, reevaluate.

		unknown=false;
		//In the case the variable was previously displaying a default value different from the DB value.
		usingDefault = false;
		return true;
	}

	public void setOnlyCached(String value) {
		unknown=false;
		value = Tools.removeStartingZeroes(value);
		myValue=value;
		Log.d("zzzz","myValue in setOnlyCached "+myValue);

	}
	//Force fetch from db next get.
	public void revert() {
		unknown=true;
		myValue=null;

	}
	boolean isSynchronizedNext = false;



	private String who;
	public boolean isSyncNext() {
		return isSynchronizedNext;
	}
	void insert(final String value,
                final boolean isSynchronized) {
		//Insert into database at some point in time.
		this.isSynchronizedNext= isSynchronized;
		//gs.getVariableCache().save(this);
		myDb.insertVariable(Variable.this,value,isSynchronized);
		timeStamp = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

		/*threadPool.execute( new Thread(new Runnable() {
	        public void run() {
	        	myDb.insertVariable(Variable.this,value,isSynchronized);
	        }
	    }));
	    */
		//GlobalState.getInstance().getLogger().addRow("Insert db time used "+(System.currentTimeMillis()-mil)+"");
	}

	public void setValueNoSync(String value) {
		if (value==null) {
			Log.e("vortex","attempt to set null value for "+this.getId());
			return;
		}
		myValue = value;
		insert(value, false);
		unknown=false;
		//In the case the variable was previously displaying a default value different from the DB value.
		usingDefault = false;
	}


	public String getId() {
		return name;
	}
	public void setId(String name) {
		this.name = name;
	}

	public Map<String, String> getKeyChain() {
		return keyChain;
	}


	public Unit getUnit() {
		return Tools.convertToUnit(myStringUnit);
	}


	public String getPrintedUnit() {
		return myStringUnit;
	}

	public DataType getType() {
		return myType;
	}

	public List<String> getBackingDataSet() {
		return myRow;
	}

	public Selection getSelection() {
		return mySelection;
	}

	public String getValueColumnName() {
		return realValueColumnName;
	}


	public Variable(String name,String label,List<String> row,Map<String,String>keyChain, GlobalState gs,String valueColumn, String defaultOrExistingValue, Boolean valueIsPersisted, String historicalValue) {
		//Log.d("zaxx","Creating variable ["+name+"] with keychain "+((keyChain==null)?"null":keyChain.toString())+"\nvalueIsPersisted?"+valueIsPersisted+" default value: "+defaultOrExistingValue);
		this.gs=gs;
		al=gs.getVariableConfiguration();

		if (name==null)
			Log.e("zaxx","NULL NAME FOR "+label);
		if (row!=null) {
			String oldName = name;
			name = al.getVarName(row);
			//Log.d("plax","name now: "+name+" previous: "+oldName);
			myRow = row;
			myType = al.getnumType(row);
			myStringUnit = al.getUnit(row);
			isSynchronized = al.isSynchronized(row);
			//check for rules on type level.
			addRules(al.getDynamicLimitExpression(row));
			String limitDesc = al.getLimitDescription(row);
			if (limitDesc!=null&&limitDesc.length()>0)
				myFilter = FilterFactory.getInstance(gs.getContext()).createLimitFilter(this,limitDesc);
//			else
//				if (myType == DataType.numeric)
//					myFilter = FilterFactory.getInstance(gs.getContext()).createLimitFilter(this,DEFAULT_LIMIT_FOR_INTEGER);
		} else
			Log.d("nils","Parameter ROW was null!!");
		this.name = name;
		this.keyChain=keyChain;
		myDb = gs.getDb();
		mySelection = myDb.createSelection(keyChain,name);
		myLabel = label;
		realValueColumnName = valueColumn;
		myValueColumn[0]=myDb.getDatabaseColumnName(valueColumn);

		myValue = null;
		//Log.d("zzzz","myValue is set to Null in New()");
		if (historicalValue!=null) {
			//Log.e("vortex","Historicalvaluefor "+this.getId()+" is "+historicalValue+" varObj: "+this.toString());
			historyChecked=true;
			myHistory = historicalValue;

			if (historicalValue.equals("*NULL*")) {
				//Log.e("vortex","Historicalvaluefor "+this.getId()+" is "+historicalValue+" varObj: "+this.toString());
				myHistory = null;
			}
		} else
			historyChecked = false;
		//Defaultvalue is either a default or the current value in DB.
		setDefault(defaultOrExistingValue);
		//No information if this variable exists or not.
		if (valueIsPersisted == null) {
			unknown = true;
			usingDefault = false;
		}
		else {
			unknown = false;
			myValue = myDefaultValue;
			//Log.d("zzzz","myValue in New() def: "+myValue);
			if (!valueIsPersisted) {
				setValue(myDefaultValue);
				//Log.d("nils","Creating variable "+this.getId()+". Variable is not persisted: "+myValue);
				//Log.d("brox","Variable: Default now true for "+this.getId()+" Value: "+this.getValue());
				usingDefault = true;
			}
		}

		if (keyChain!=null && keyChain.containsKey(valueColumn)) {
			Log.e("vortex","Variable value column in keyset for valcol "+valueColumn+" varid "+name);
			isKeyVariable=true;
		}

		//Log.d("vortex","unknown? "+unknown);
	}




	public void deleteValue() {
		myDb.deleteVariable(name,mySelection,isSynchronized);
		myValue=null;
		unknown = false;
		usingDefault = false;
	}

	public void deleteValueNoSync() {
		myDb.deleteVariable(name,mySelection,false);
		myValue=null;
		unknown = false;
		usingDefault = false;

	}

	public void setType(DataType type) {
		myType = type;
	}

	public void invalidate() {
		unknown=true;
		usingDefault = false;
		timeStamp=null;
		myValue=null;
	}

	public boolean isKeyVariable() {
		return isKeyVariable;
	}


	private void addRules(String rules) {
		if (rules == null||rules.length()==0)
			return;
		String[] ruleA = rules.split(",");
		Log.d("nils","In addrules with "+rules+". Rules found: "+(ruleA==null?"NULL":ruleA.length));
		for (String rule:ruleA) {
			if (myRules==null)
				myRules = new HashSet<String>();
			myRules.add(rule);
		}
	}



	public boolean hasBrokenRules() {
		/*
		if (iAmIllegal==null)
			refreshRuleState();
		if (currentRuleState!=null)
		for(String key:currentRuleState.keySet()) {
			Log.d("nils","Rule: "+key+" state: "+currentRuleState.get(key));
		}
		return iAmIllegal;
		*/
		return false;
	}

	public boolean hasValueOutOfRange() {
		return iAmOutOfRange;
	}

	public void setOutOfRange(boolean oor) {
		iAmOutOfRange = oor;
	}

	private final static String[] timeStampS = new String[] {"timestamp"};
	private final static String[] authorS = new String[] {"author"};

	public Long getTimeOfInsert() {
		if (timeStamp!=null) {
			//Log.d("vortex","cached Timestamp for "+this.getId()+" is "+timeStamp);
			return timeStamp;
		}
		//Log.d("vortex","Timestamp for "+this.getId()+" name: "+name+" mySelection: "+mySelection.selection+" selArgs:"+Tools.printSelectionArgs(mySelection.selectionArgs));
		String tmp = myDb.getValue(name, mySelection,Variable.timeStampS);
		if (tmp!=null) {
			//Log.d("vortex","Timestamp for "+this.getId()+" is "+tmp);
			timeStamp = Long.parseLong(tmp);
			return timeStamp;
		}
		Log.e("vortex","returning null in gettimeofinsert");
		return null;

	}

	public String getWhoGaveThisValue() {
		if (who!=null) {
			Log.d("vortex","cached who for "+this.getId()+" is "+who);
			return who;
		}
		who = myDb.getValue(name, mySelection,Variable.authorS);
		if (who!=null) {
			//Log.d("vortex","Who for "+this.getId()+" is "+who);
			return who;
		}
		//Log.e("vortex","returning null in whogavethisvalue");
		return null;

	}


	/*
        public void setKeyChainVariable(String key) {
            Log.d("nils","SetKeyChain called for "+this.getId());
            iAmPartOfKeyChain = key;
        }

        public String getPartOfKeyChain() {
            return iAmPartOfKeyChain;
        }
    */
	public boolean isInvalidated() {
		return unknown;
	}

	public boolean isUsingDefault() {
		return usingDefault ;
	}

	public CombinedRangeAndListFilter getLimitFilter() {
		return myFilter;
	}

	//Cut out the instance part of a variable name.
	public static String getVarInstancePart(String varId) {
		final String sep = Constants.VariableSeparator;
		if (varId == null)
			return null;
		int start=varId.indexOf(sep);
		if (start!=-1 && (start+1<varId.length())) {
			int end= varId.indexOf(sep, start+1);
			if (end!=-1) {
				Log.d("vortex","getVarInstancePart returns: "+varId.substring(start+1,end));
				return varId.substring(start+1, end);
			}
		}
		return null;
	}

	public static String getVarSuffixPart(String varId) {
		if (varId == null)
			return null;
		int c = varId.lastIndexOf(Constants.VariableSeparator);
		if (c!=-1 && c<(varId.length()-1))
			return varId.substring(c+1,varId.length());
		else {
			Log.d("vortex","getVarSuffix returns null for "+varId);
			return null;
		}
	}

	public void setDefaultValue(String defaultValue) {

		this.setDefault(defaultValue);
		//Log.d("brox","setDefValue inParam: "+defaultValue+" Defaultvalue for "+this.getId()+" set to "+myDefaultValue);
		setValue(myDefaultValue);
		//Log.d("brox","Default now true for "+this.getId()+" Value: "+this.getValue());
		usingDefault=true;
	}


	private void setDefault(String defaultValue) {
		//Log.d("vortex","Setting defaultvalue : "+defaultValue+" for "+this.getId());
		if (defaultValue == null) {

			myDefaultValue = null;
		}
		else if (defaultValue.equals(Constants.HISTORICAL_TOKEN_IN_XML)) {

			myDefaultValue = this.getHistoricalValue();
			Log.d("vortex","Setting default from historical: "+myDefaultValue+" for "+this.getId());
		}
		else {
			//Log.d("vortex","Setting default from default value: "+defaultValue+" for "+this.getId());
			myDefaultValue = defaultValue.equals(Constants.NO_DEFAULT_VALUE)?null:defaultValue;
		}
	}






}
