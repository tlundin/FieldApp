package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;

import com.teraim.fieldapp.dynamic.VariableConfiguration;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;
import com.teraim.fieldapp.dynamic.types.ColumnDescriptor;
import com.teraim.fieldapp.dynamic.types.DB_Context;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.Variable.DataType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.ui.Linje;
import com.teraim.fieldapp.utils.DbHelper.Selection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class WF_Linje_Meter_List extends WF_List implements EventListener {
	private static final String TAG = "WF_Linje_Meter_List";

	//final String variableName;
	private final Selection s;
	private final String[] columnNames;
	private final List<ColumnDescriptor> cd;
	private final String varId;
	private int myHeaderCol = 0;
	private final Map<String,String> listElemSelector;
	private final Linje linjeV;
	private final String[] avgrT;
    private final String[] avgrV;

	//Variablename should always be first element of columns. Will be used as header.
	public WF_Linje_Meter_List(String id, boolean isVisible, WF_Context ctx, List<ColumnDescriptor> columns, Selection selection, 
			String varId, Map<String, String> keySet,Linje linje, String[] avgrValueA, String[] avgrTyper) {
		super(id, isVisible, ctx);
		
		//this.variableName= variableName; 
		this.s = selection;
		columnNames = new String[columns.size()];
		int i=0;
		for (ColumnDescriptor cd:columns) {
			columnNames[i]=cd.colName;
			if (cd.isHeader)
				myHeaderCol = i;
			i++;
		}
		this.linjeV=linje;

		cd=columns;
		this.varId = varId;
		this.listElemSelector = keySet;
		avgrV =avgrValueA;
		avgrT =avgrTyper;
		ctx.registerEventListener(this, EventType.onSave);
	}





	@Override
	public void onEvent(Event e) {

		Log.d(TAG,"Got event from "+(e==null?"null":e.getProvider()));
		if (e instanceof WF_Event_OnSave) {
			boolean insert = true;
			WF_Event_OnSave onS = (WF_Event_OnSave)e;
			Map<Variable, String> x = onS.varsAffected;
			if (x!=null) {
				Entry<Variable, String> eset = x.entrySet().iterator().next();
				Variable v = eset.getKey();
				String oldValue = eset.getValue();
				String[] sel = v.getSelection().selectionArgs;
				Log.d(TAG,"Variable has selector Args "+ Arrays.toString(sel));
				for (int i=0;i<sel.length;i++)
					Log.d(TAG,sel[i]);
				Log.d(TAG,"VALUE: "+v.getValue());
				Log.d(TAG,"OLD VALUE:"+oldValue);
                //empty?
				if (oldValue !=null) {
					if (!oldValue.equals(v.getValue())) {
						if (v.getValue()==null) {
							Log.d(TAG,"This is a DELETE event");
							insert = false;
						} else
							//if not delete, replace meter with old meter to remove existing values.
							v.getKeyChain().put("meter", oldValue);


						String name = v.getKeyChain().get("value");

						Log.d(TAG,"Name is"+name);
						v.getKeyChain().remove("value");

						//Try to delete this key from cache.
						varCache.deleteCacheEntry(v.getKeyChain());

						Set<Entry<String, String>> xx = v.getKeyChain().entrySet();
						for (Entry<String, String> ee:xx) 
							Log.d(TAG,"key, value: "+ee.getKey()+","+ee.getValue());



						//keys
						gs.setDBContext(new DB_Context(null,v.getKeyChain()));
						List<List<String>>rows =gs.getVariableConfiguration().getTable().getRowsStartingWith(VariableConfiguration.Col_Functional_Group, name);
						if (rows!=null) {
							Log.d(TAG,"Got "+rows.size()+" results");
							Map<String,String> deletedVariables = 
									deleteAllDependants(rows);
							if (insert && deletedVariables!=null) {
								Log.d(TAG,"reinserting variables with new key.");
								v.getKeyChain().put("meter", v.getValue());
								Log.d(TAG,"meter set to "+v.getValue());

								//Delete any existing values on same meter.
								//deleteAllDependants(rows);
								Set<Entry<String, String>> es = deletedVariables.entrySet();
								Variable var;
								for (Entry<String, String> en:es) {
									var = varCache.getVariable(v.getKeyChain(),en.getKey());
									if (var!=null) {
										if (var.getValue()!=null) 
											Log.e("nils","This variable already has a value...should not happen!");
										var.setValue(en.getValue());
									} else
										Log.e("nils","Variable null...should not happen. VarID: "+en.getKey());
								}
							}
						}

					} else
						Log.e("nils","Do nothing, value didn't change");
				}
			}
		}

		refreshList();
		draw();
		myContext.registerEvent(new WF_Event_OnRedraw(this.getId()));
	}

	@Override
	public String getName() {
		return "LINJE METER LIST "+this.getId();
	}


	private Map<String, String> deleteAllDependants(List<List<String>> rows) {
		Map<String,String> ret = null;
		Variable v;
		for (List<String>row:rows) {

			v = varCache.getVariable(al.getVarName(row));
			if (v!=null && v.getValue()!=null) {
				Log.d(TAG,"Deleting: "+v.getId()+"with value "+v.getValue());
				if (ret==null)
					ret = new HashMap<String,String>();
				ret.put(v.getId(), v.getValue());
				v.deleteValue();

			}
		}
		return ret;
	}





	private void refreshList() {
		Log.d(TAG,"In refereshlist..");

		clear();
		linjeV.removeAllMarkers();
		List<String[]> rows = gs.getDb().getUniqueValues(columnNames,s);
		if (rows!=null) {
			Log.d(TAG,"Got "+rows.size()+" results in refreshList, WF_Instance");
			int rowC=0;

			for (String[] colVals:rows) {
				Log.d(TAG,"colVals header: "+colVals[myHeaderCol]);
				if (colVals!=null) {	
					Map<String,String> bonnlapp = new HashMap<String,String>(listElemSelector);				
					for (int colC=0;colC<colVals.length;colC++) {
						bonnlapp.put(cd.get(colC).colName, colVals[colC]);
						Log.d(TAG,"Bonnlapp - Adding key "+cd.get(colC).colName+" with value "+colVals[colC]);
					}
					String header = colVals[myHeaderCol];
					WF_ClickableField entryF = new WF_ClickableField_Selection(header,"",myContext,this.getId()+rowC,true,new DisplayFieldBlock());
					if (colVals[myHeaderCol]!=null && colVals[myHeaderCol].equals("Avgränsning")) {
						Log.d(TAG,"In refreshlist for avgränsning");						
						//(String name,String label,List<String> row,Map<String,String>keyChain, GlobalState gs,String valueColumn, String defaultOrExistingValue, Boolean valueIsPersisted)
						Variable v = new Variable(varId,"Start",al.getCompleteVariableDefinition(varId),bonnlapp,gs,"meter",null,null,null);
						v.setType(DataType.numeric);
						varCache.put(v);
						String start = v.getValue();
						entryF.addVariable(v, true, null, true,false);
						Map<String,String> slutKey = new HashMap<String,String>(listElemSelector);
						slutKey.put("meter", start);
						gs.setDBContext(new DB_Context(null,slutKey));
						v = varCache.getVariable(NamedVariables.AVGRANSSLUT);						
						entryF.addVariable(v, true, null, true,false);
						String slut = v.getValue();
						v = varCache.getVariable(NamedVariables.AVGRTYP);
						entryF.addVariable(v, false, null, true,false);
						if (slut!=null)
							linjeV.addMarker(start, slut, getCorrectLabel(v.getValue()));
						else
							Log.d(TAG,"slut null!");
					} else {
						Variable v = new Variable(varId,"Avstånd",al.getCompleteVariableDefinition(varId),bonnlapp,gs,"meter",null,null,null);
						varCache.put(v);
						v.setType(DataType.numeric);						
						String start = v.getValue();
						entryF.addVariable(v, true, null, true,false);
						linjeV.addMarker(start, colVals[myHeaderCol]);
					}

					get().add(entryF);
				}
				rowC++;
			}
		}
		linjeV.invalidate();
	}




	private String getCorrectLabel(String value) {
		int i=0;
		for (String avgrS:avgrV) {
			if (avgrS.equals(value))
				return avgrT[i];
			i++;
		}
		return "?";
	}
}
