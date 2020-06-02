package com.teraim.fieldapp.dynamic;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class VariableConfiguration implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 942330642338510319L;
	public static final String Col_Variable_Name = "Variable Name";
	private static final String Col_Variable_Label = "Variable Label";
	private static final String Col_Variable_Keys = "Key Chain";
	private static final String Type = "Type";
	public static final String Col_Functional_Group = "Group Name";
	private static final String Col_Variable_Scope = "Scope";
	private static final String Col_Variable_Limits = "Limits";
	private static final String Col_Variable_Dynamic_Limits = "D_Limits";
	private static final String Col_Group_Label = "Member Label";
	private static final String Col_Group_Description = "Member Description";

	public final static String KEY_YEAR = "år";

	private static final List<String>requiredColumns=Arrays.asList(Col_Variable_Keys,Col_Functional_Group,Col_Variable_Name,Col_Variable_Label,Type,"Unit","List Values","Description",Col_Variable_Scope,Col_Variable_Limits,Col_Variable_Dynamic_Limits,Col_Group_Label,Col_Group_Description);
	private static final int KEY_CHAIN=0;
    private static final int FUNCTIONAL_GROUP=1;
    private static final int VARIABLE_NAME=2;
    private static final int VARIABLE_LABEL=3;
    private static final int TYPE=4;
    private static final int UNIT=5;
    private static final int LIST_VALUES=6;
    private static final int DESCRIPTION=7;
    private static final int SCOPE=8;
    private static final int LIMIT=9;
    private static final int D_LIMIT=10;
    private static int GROUP_LABEL=11;
    private static int GROUP_DESCRIPTION = 12;

	public String getColumn(String columnName, List<String> row) {
		int cIndex = getTable().getColumnIndex(columnName);
		if (cIndex!=-1)
			return row.get(cIndex);
		else
			return null;
	}


	public enum Scope {
		local_sync,
		local_nosync,
		global_nosync,
		global_sync
	}
	
	private Map<String,Integer>fromNameToColumn;


	private final Table myTable;
	private final GlobalState gs;


	public VariableConfiguration(GlobalState gs,Table t) {
		this.gs = gs;
		myTable = t;
		//TODO: Call this from parser.
		validateAndInit();
	}

	private void validateAndInit() {
		fromNameToColumn = new HashMap<String,Integer>();
		for (String c:requiredColumns) {
			int tableIndex = myTable.getColumnIndex(c);
			if (tableIndex==-1) {
				Log.e("nils","Missing column: "+c);
				Log.e("nils","Table has "+myTable.getColumnHeaders().toString());
				return;
			}
			else
				//Now we can map a call to a column to the actual implementation.
				//Actual column index is decoupled.
				fromNameToColumn.put(c, tableIndex);
		}

    }

	public Table getTable() {
		return myTable;
	}



	/*
	public String getListEntryName(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(LIST_ENTRY)));
	}
	 */
	public String getAssociatedWorkflow(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(LIST_VALUES)));
	}
	public List<String> getListElements(List<String> row) {
		List<String> el = null;
		String listS = row.get(fromNameToColumn.get(requiredColumns.get(LIST_VALUES)));
		if (listS!=null&&listS.trim().length()>0) {
			String[] x = listS.trim().split("\\|");
			if (x!=null&&x.length>0)
				el = new ArrayList<String>(Arrays.asList(x));
		}
		return el;
	}
	

	public String getVarName(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(VARIABLE_NAME)));
	}

	public String getVarLabel(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(VARIABLE_LABEL)));
	}

	public String getVariableDescription(List<String> row) {		
		return row.get(fromNameToColumn.get(requiredColumns.get(DESCRIPTION)));
	}


	public String getGroupDescription(List<String> row) {
		Integer col = fromNameToColumn.get(Col_Group_Description);
		if (col!=null && col<row.size())
			return row.get(col);
		return null;
	}

	public String getGroupLabel(List<String> row) {
		Integer col = fromNameToColumn.get(Col_Group_Label);
		if (col!=null && col<row.size())
			return row.get(col);
		return null;
	}
	/*
	public boolean isLocal(List<String>row) {
		
	}
	*/
	
	//If the variable should be synchronized between the devices.
	public boolean isSynchronized(List<String> row) {
		String s= row.get(fromNameToColumn.get(requiredColumns.get(SCOPE)));
		return (s==null||s.length()==0||
				s.equals(Scope.global_sync.name())||
						s.equals(Scope.local_sync.name()));

	}
	
	//If the variable shall be exported via JSON to server.
	public boolean isLocal(List<String> row) {
		if (row!=null) {
			String s = row.get(fromNameToColumn.get(requiredColumns.get(SCOPE)));
			//Log.d("nils","getvarislocal uses string "+s);
			return (s != null && s.startsWith("local"));
		}
		Log.e("vortex","row was null...cannot determine if local or global. Will default to global");
		return false;


	}
	
	public String getLimitDescription(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(LIMIT)));	
	}	
	public String getDynamicLimitExpression(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(D_LIMIT)));	
	}

	public String getKeyChain(List<String> row) {
		//Check for null or empty
		if (row==null) {
			Log.d("vortex","row was null in getKeyChain");
			return null;
		}
		//else 
		//	Log.d("vortex","Row is "+row+" length_: "+row.size()+" fromname "+fromNameToColumn);
		Pattern pattern = Pattern.compile("\\s");
		Matcher matcher = pattern.matcher(row.get(0));
		if(matcher.find()) {
			Log.e("vortex","Space char found in keychain: "+row.get(0)+" length : "+row.get(0).length()+" size: "+row.size());
			return null;
		}
			
		else
			return row.get(fromNameToColumn.get(requiredColumns.get(KEY_CHAIN)));		
	}

	public String getFunctionalGroup(List<String> row) {
		return row.get(fromNameToColumn.get(requiredColumns.get(FUNCTIONAL_GROUP)));
	}
	public Variable.DataType getnumType(List<String> row) {
		String type = row.get(fromNameToColumn.get(requiredColumns.get(TYPE)));
		if (type!=null) {		
			if (type.equals("number")||type.equals("numeric"))
				return Variable.DataType.numeric;
			else if (type.equals("boolean"))
				return Variable.DataType.bool;
			else if (type.equals("list"))
				return Variable.DataType.list;
			else if (type.equals("text")||type.equals("string"))
				return Variable.DataType.text;
			else if (type.equals("auto_increment")) 
				return Variable.DataType.auto_increment;
			else if (type.equals("array")) 
				return Variable.DataType.array;
			else if (type.equals("decimal")||type.equals("float"))
				return Variable.DataType.decimal;
			else
				Log.e("nils","TYPE NOT KNOWN: ["+type+"]");
		}
		gs.getLogger().addRow("");
		String myId = getVarName(row);
		gs.getLogger().addRedText("Type parameter not configured for variable "+myId+" Will default to numeric");
		return Variable.DataType.numeric;
	}


	public String getUnit(List<String> row) {
		String unit = row.get(fromNameToColumn.get(requiredColumns.get(UNIT)));
		if (unit == null) {
			gs.getLogger().addYellowText("Unit was null for variable "+getVarName(row));
			unit = "";
		}
		return unit;	
	}

	public List<String> getCompleteVariableDefinition(String varName) {
		return myTable.getRowFromKey(varName);
	}
	
	public String getAction(List<String> row) {
		return null;
	}

	public String getEntryLabel(List<String> row) {
		if (row == null)
			return null;
		String  res= myTable.getElement(Col_Group_Label, row);
		//If this is a non-art variable, use varlabel instead.
		if (res==null) {
			//Log.d("vortex","failed to find value for column "+Col_Group_Label+ ". Will use varlabel "+this.getVarLabel(row)+" instead.");
			//gs.getLogger().addRow("");
			//gs.getLogger().addYellowText("failed to find value for column "+Col_Group_Label+ ". Will use variable label "+this.getVarLabel(row)+" instead.");
			res =this.getVarLabel(row);
		}
		if (res == null)
			Log.e("nils","getEntryLabel failed to find a Label for row: "+row.toString());
		return res;
	}

	public String getDescription(List<String> row) {
		String b = myTable.getElement(Col_Group_Description, row);
		if(b==null) 
			b = this.getVariableDescription(row);

		return (b==null?"":b);
	}



	public String getUrl(List<String> row) {
		return myTable.getElement("Internet link", row);	
	}

	public boolean isDisplayInList(List<String> row) {
		return false;
	}

	//Map<String,Variable>varCache = new ConcurrentHashMap<String,Variable>();



	
	/*
		Variable v = varCache.get(varId);
		if (v!=null) {
			//Log.d("nils","found cached var: "+varId);//+" backing: "+this.getCompleteVariableDefinition(varId));
			return v;
		}
		else {
			//use standard hash. Fetch varLabel header later when needed.
			v= new Variable(varId,null,getCompleteVariableDefinition(varId),gs.getCurrentKeyHash(),gs,vCol,value);
			varCache.put(varId, v);			
		}
		return v;
	}
	
	*/
	
	private final static String vCol = "value";
	

	

	/*
		String varLabel =null;
		String keyChain = null;
		Variable v = varCache.get(varId);
		if (v!=null) {
			//Log.d("nils","found cached var: "+varId);//+" backing: "+this.getCompleteVariableDefinition(varId));
			return v;
		}
		else {
			List<String> row = this.getCompleteVariableDefinition(varId);
			if (row!=null) {
				keyChain = this.getKeyChain(row);				
				varLabel = this.getVarLabel(row);
				v = new Variable(varId,varLabel,row,buildDbKey(keyChain,gs.getCurrentKeyHash()),gs,vCol,null);		
				varCache.put(varId, v);
			} else 
				Log.e("nils","getVariableInstance: Cannot find variable: "+varId);		
			
			return v;
		}
	}
*/

	//Create a variable with the current context and the variable's keychain.
	//public Variable getVariableInstance(String keyChain,String varId,String varLabel,List<String> row,Map<String, String> cMap,String valueColumn,String value) {	
		//find my keys in the current context.
		//Use a cache for faster access.
	//	return new Variable(varId,varLabel,row,buildDbKey(keyChain,cMap),gs,valueColumn,value);
	//} 


	private Map<String, String> buildDbKey(String keyChain,
			Map<String, String> cMap) {
		if (keyChain==null||keyChain.isEmpty()) 
			return null;
		//Log.e("nils","Keys in chain:"+keyChain);
		//Log.e("nils","Keys available: "+cMap.keySet().toString());
		//Log.e("nils","Key values:"+cMap.entrySet().toString());
		String[] keys = keyChain.split("\\|");
		Map<String, String> vMap = new HashMap<String,String>();
		for (String key:keys) {	
			String value = null;
			if(cMap!=null) 
				value = cMap.get(key);
			if (value!=null) {
				vMap.put(key, value);
				//Log.d("nils","Adding keychain key:"+key+" value: "+value);
			}
			else {
				Log.e("nils","Couldn't find key "+key+" in current context");
			}
		}
		return vMap;
	}

	public Map<String, String> createRutaKeyMap() {
		String currentYear = getGlobalVariable(NamedVariables.CURRENT_YEAR);
		String currentRuta = getGlobalVariable(NamedVariables.CURRENT_RUTA);
		if (currentRuta == null)
			return null;
		return Tools.createKeyMap(KEY_YEAR,currentYear,"ruta",currentRuta);
	}
	


	public Map<String,String> createProvytaKeyMap() {
		String currentYear = getGlobalVariable(NamedVariables.CURRENT_YEAR);
		String currentRuta = getGlobalVariable(NamedVariables.CURRENT_RUTA);
		String currentProvyta = getGlobalVariable(NamedVariables.CURRENT_PROVYTA);		
		if (currentRuta == null||currentProvyta==null)
			return null;
		return Tools.createKeyMap(KEY_YEAR,currentYear,"ruta",currentRuta,"provyta",currentProvyta);
	}
	
	public Map<String, String> createSmaprovytaKeyMap() {
		String currentYear = getGlobalVariable(NamedVariables.CURRENT_YEAR);
		String currentRuta = getGlobalVariable(NamedVariables.CURRENT_RUTA);
		String currentProvyta = getGlobalVariable(NamedVariables.CURRENT_PROVYTA);		
		String currentSmayta = getGlobalVariable(NamedVariables.CURRENT_SMAPROVYTA);		
		if (currentYear == null || currentRuta == null||currentProvyta==null||currentSmayta==null)
			return null;
		return Tools.createKeyMap(KEY_YEAR,currentYear,"ruta",currentRuta,"provyta",currentProvyta,"smaprovyta",currentSmayta);
	}

	public Map<String,String> createYearKeyMap() {
		Map<String,String> ar = new HashMap<>();
		ar.put(KEY_YEAR,Constants.getYear());
		return ar;
	}

	public Map<String,String> createGpsKeyMap() {
		Map<String,String> gps_keymap = new HashMap<>();
		gps_keymap.put(KEY_YEAR,Constants.getYear());
		gps_keymap.put(DbHelper.AUTHOR,gs.getGlobalPreferences().get(PersistenceHelper.USER_ID_KEY));
		return gps_keymap;
	}
	public Map<String, String> createDelytaKeyMap() {
		String currentYear = getGlobalVariable(NamedVariables.CURRENT_YEAR);
		String currentRuta = getGlobalVariable(NamedVariables.CURRENT_RUTA);
		String currentProvyta = getGlobalVariable(NamedVariables.CURRENT_PROVYTA);		
		String currentDelyta = getGlobalVariable(NamedVariables.CURRENT_DELYTA);		
		if (currentYear == null || currentRuta == null||currentProvyta==null||currentDelyta==null) {
			Log.e("nils","CreateDelytaKeyMap failed. Missing value");
			return null;
		}
		return Tools.createKeyMap(KEY_YEAR,currentYear,"ruta",currentRuta,"provyta",currentProvyta,"delyta",currentDelyta);
	}
	
	//note that provyta is the key for current linje.
	public Map<String, String> createLinjeKeyMap() {
		String currentYear = getGlobalVariable(NamedVariables.CURRENT_YEAR);
		String currentRuta = getGlobalVariable(NamedVariables.CURRENT_RUTA);		
		String currentLinje = getGlobalVariable(NamedVariables.CURRENT_LINJE);		
		if (currentYear == null || currentRuta == null||currentLinje==null) {
			Log.e("nils","CreateLinjeKeyMap failed. Missing value: CR: "+currentRuta+ "YR: "+currentYear+" CL: "+currentLinje);
			return null;
		}

		return Tools.createKeyMap(VariableConfiguration.KEY_YEAR,currentYear,"ruta",currentRuta,"provyta",currentLinje);
	}

	public String getCurrentRuta() {
		return getGlobalVariable(NamedVariables.CURRENT_RUTA);
	}

	public String getCurrentProvyta() {
		return getGlobalVariable(NamedVariables.CURRENT_PROVYTA);
	}


	private String getGlobalVariable(String varId) {
		return gs.getVariableCache().getVariable(null, varId).getValue();
		
	}
/*
	public void invalidateCacheKeys(String key) {
		gs.refreshKeyHash();
		for (List<Variable> vl:myCache.getVariables()) {
			if (vl!=null)
				for (Variable v:vl)
					if (v.getKeyChain()!=null && v.getKeyChain().get(key)!=null) {
						Log.d("nils","variable "+v.getId()+" contained "+key);
						v.invalidateKey();
					}
		}
	}
	*/




















}

