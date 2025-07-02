package com.teraim.fieldapp.dynamic.workflow_realizations;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.blocks.DisplayFieldBlock;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event;
import com.teraim.fieldapp.dynamic.workflow_abstracts.Event.EventType;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventGenerator;
import com.teraim.fieldapp.dynamic.workflow_abstracts.EventListener;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.Constants;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WF_List_UpdateOnSaveEvent extends WF_Static_List implements EventListener,EventGenerator{


	private final DisplayFieldBlock myEntryFieldFormat;
	//Precached variables.
	private Map<String,String> varValueMap;

	private class EntryField {
		WF_ClickableField cfs;
		final Set<String> varIDs;

		EntryField() {
			varIDs = new HashSet<String>();
		}
	}


	private final Map<String,EntryField> entryFields = new HashMap<String,EntryField>();
	private int index = 0;

	public WF_List_UpdateOnSaveEvent(String id, WF_Context ctx, boolean isVisible, final DisplayFieldBlock format) {
		super(id, ctx,null,isVisible);
		ctx.registerEventListener(this, EventType.onSave);
		o = LogRepository.getInstance();
		long t1 = System.currentTimeMillis();
		this.myEntryFieldFormat = format;
	}
	private int cr=0;

	public void setRows(List<List<String>> rows) {
		//if (myRows==null && entryFields==null) {
		myRows = rows;
		String namePrefix = al.getFunctionalGroup(rows.get(0));
		Log.d("baza","prefetch for "+gs.getVariableCache().getContext().getContext()+" , "+ namePrefix);
		varValueMap = gs.getDb().preFetchValuesForAllMatchingKey(gs.getVariableCache().getContext().getContext(), namePrefix);

		for (List<String> row : rows) {
			addEntryField(row, myEntryFieldFormat);
		}
		Log.d("baza","entryFields has "+entryFields.size()+" elements");

		//}
	}
	private void addEntryField(List<String> r,DisplayFieldBlock format) {

		EntryField ef;
		String entryLabel = al.getEntryLabel(r);
		if (entryLabel==null||entryLabel.length()==0) {
			Log.d("nils","Skipping empty entrylabel");
			return;
		}
		//Log.d("nils","ADD EntryField with label "+entryLabel);
		ef = entryFields.get(entryLabel);
		if (ef==null) 	{
			cr++;
			//Log.d("baza",entryLabel);
			WF_ClickableField entryF = new WF_ClickableField_Selection(entryLabel,al.getDescription(r),myContext,this.getId()+"_"+index++,true,format);
			get().add(entryF);
			ef = new EntryField();
			entryFields.put(entryLabel, ef);
			ef.cfs = entryF;
		}
		ef.varIDs.add(al.getVarName(r));
		//Log.d("vortex","added "+al.getVarName(r)+" to varIDs");
	}



	@Override
	public boolean addVariableToEveryListEntry(String varSuffix,boolean displayOut,String format,boolean isVisible, boolean showHistorical,String initialValue) {

		Log.d("nils","in AddVariableToEveryListEntry for "+varSuffix);
		EntryField ef;
		Map<String,EntryField> mapmap = new HashMap<String,EntryField>();
		boolean success;
		for (String key:entryFields.keySet()) {
			ef = entryFields.get(key);

			success=false;

			//Log.d("vortex","varIDs contain: "+ef.varIDs);
			for (String varID:ef.varIDs) {
				//Log.d("vortex",varID);
				if (varID.endsWith(varSuffix)) {
					mapmap.put(varID,ef);
					success=true;
					//Log.e("nils","Found Match for suffix: "+varSuffix+" Match: "+varID);
					break;
				}

			}

			if (!success) {
				//This variable is either wrong or global.
				Log.d("nils","DID NOT FIND MATCH for suffix: "+varSuffix);
				Variable v= varCache.getVariable(varSuffix,initialValue,-1);
				//add global variable
				if (v!=null)
					ef.cfs.addVariable(v, displayOut,format,isVisible,showHistorical);
				else {
					o.addCriticalText("Variable with suffix "+varSuffix+" was not found when creating list "+this.getId());
					o.addText("context: ["+gs.getVariableCache().getContext().toString()+"]");
					String namePrefix = al.getFunctionalGroup(myRows.get(0));
					o.addText("Group: "+namePrefix);
					return true;
				}

			}
		}
		if (!mapmap.isEmpty()) {
			createAsync(mapmap, displayOut, format, isVisible, showHistorical, initialValue);
			return false;
		}
		//If mapmap is empty of variables, no work to be done. Return true = done.
		return true;
	}





	/*
        private class CreateListAsyncTask extends AsyncTask<Void,String,Void> {

            ProgressDialog pDialog;

            @Override
            protected Void doInBackground(Void... params) {
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                pDialog.dismiss();
                WF_List_UpdateOnSaveEvent.listReady();
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                pDialog = ProgressDialog.show(myContext.getContext(), "",
                        "Loading. Please wait...", true);

            }

            @Override
            protected void onProgressUpdate(String ...value) {
                super.onProgressUpdate(null);
                pDialog.setMessage(value[0]);
            }
        }

    */
	private void createAsync(final Map<String, EntryField> mapmap,final  boolean displayOut,final  String format,final  boolean isVisible,final boolean showHistorical,final String initialValue) {

		Log.d("beezo", myContext.getContext() + "");
		Variable v;
		long t = System.currentTimeMillis(), t2 = 0;
		int i = 0;
		int tot = mapmap.keySet().size();
		WF_ClickableField.clearStaticGlobals();
		for (String vs : mapmap.keySet()) {
			long t1 = System.currentTimeMillis();
			//If variabel has value in db, use it.
			if (varValueMap != null && varValueMap.containsKey(vs))
				v = varCache.getCheckedVariable(vs, varValueMap.get(vs), true);
			else
				//Otherwise use default.
				v = varCache.getCheckedVariable(vs, initialValue, false);
			i++;

			//Defaultvalue is either the historical value, null or the current value.
			//Historical value will be set if the variable does not exist already. If it exists, the current value is used, even if it is null.
			t2 += (System.currentTimeMillis() - t1);
			if (v != null) {
				//Log.d("vortex","CreateAsync. Adding variable "+v.getId()+" to "+mapmap.get(vs).cfs.label);
				mapmap.get(vs).cfs.addStaticVariable(v, displayOut, format, isVisible, showHistorical);
			} else {
				o.addText("");
				o.addCriticalText("Variable with suffix " + vs + " was not found when creating list with id " + getId());
				Log.e("nils", "Variable with suffix " + vs + " was not found when creating list with id " + getId());
			}
			//if (i % 10 == 0) {
			//		publishProgress("Adding variables (" + i + "/" + tot + ")");
		}

		//clear static opt-val variables.
		Log.d("beezo", "Time: " + (System.currentTimeMillis() - t) + " t2: " + t2);

	}





		/*

        (new CreateListAsyncTask() {

            @Override
            protected Void doInBackground(Void... params) {
                super.doInBackground(params);
                Variable v;
                long t = System.currentTimeMillis(), t2 = 0;
                int i = 0;
                int tot = mapmap.keySet().size();
				WF_ClickableField.clearStaticGlobals();
                for (String vs : mapmap.keySet()) {
                    long t1 = System.currentTimeMillis();
                    //If variabel has value in db, use it.
                    if (varValueMap != null && varValueMap.containsKey(vs))
                        v = varCache.getCheckedVariable(vs, varValueMap.get(vs), true);
                    else
                        //Otherwise use default.
                        v = varCache.getCheckedVariable(vs, initialValue, false);
                    i++;

                    //Defaultvalue is either the historical value, null or the current value.
                    //Historical value will be set if the variable does not exist already. If it exists, the current value is used, even if it is null.
                    t2 += (System.currentTimeMillis() - t1);
                    if (v != null) {
                        //Log.d("vortex","CreateAsync. Adding variable "+v.getId()+" to "+mapmap.get(vs).cfs.label);
                        mapmap.get(vs).cfs.addVariable(v, displayOut, format, isVisible, showHistorical, true);
                    } else {
                        o.addText("");
                        o.addCriticalText("Variable with suffix " + vs + " was not found when creating list with id " + getId());
                        Log.e("nils", "Variable with suffix " + vs + " was not found when creating list with id " + getId());
                    }
                    if (i%10==0) {
                        publishProgress("Adding variables ("+i + "/" + tot+")");
                    }
                }
                //clear static opt-val variables.

                Log.d("beezo", "Time: " + (System.currentTimeMillis() - t) + " t2: " + t2);
               // pDialog.dismiss();
                return null;
            }
        }).execute();
    }
*/




	@Override
	public void addFieldListEntry(String listEntryID,String label,String description) {
		WF_ClickableField entryF = new WF_ClickableField_Selection(label,description,myContext,this.getId()+listEntryID,true,myEntryFieldFormat);
		get().add(entryF);
		EntryField ef = new EntryField();
		entryFields.put(this.getId()+listEntryID, ef);
		Log.d("vortex","I am now adding listentry "+this.getId()+listEntryID);
		ef.cfs = entryF;
	}

	public Variable addVariableToListEntry(String varNameSuffix,boolean displayOut,String targetField,
										   String format, boolean isVisible, boolean showHistorical, String initialValue) {
		String tfName = this.getId()+targetField;
		EntryField ef = entryFields.get(tfName);
		if (ef==null) {
			Log.e("nils","Didnt find entry field "+tfName);
			o.addText("");
			o.addCriticalText("Did NOT find entryfield referred to as "+tfName);
			return null;
		}

		String vName = targetField+Constants.VariableSeparator+varNameSuffix;
		Variable v = varCache.getVariable(vName,initialValue,-1);

		if (v==null) {
			//try with simple name.
			o.addText("Will retry with variable name: "+varNameSuffix);
			v = varCache.getVariable(varNameSuffix,initialValue,-1);
			if (v==null) {
				Log.e("nils","Didnt find variable "+vName+" in AddVariableToList");
				o.addText("");
				o.addCriticalText("Did NOT find variable referred to as "+vName+" in AddVariableToList");
				return null;
			}
		}
		ef.cfs.addVariable(v, displayOut,format,isVisible,showHistorical);
		return v;

	}

	@Override
	public void onEvent(Event e) {
		if (e.getProvider().equals(this) )
			Log.d("nils","Throwing event that originated from me");
		else {
			Log.d("nils","GOT EVENT!! Provider: "+e.getProvider());
			if (e.getType()==EventType.onSave) {
				//force complete redraw if incremental fails.
				if (!prepareIncrementalDraw(((WF_Event_OnSave)e).getListable())) {
					Log.d("nils","REDRAW!!");
					resetOnEvent();
				}
				draw();
			}
			myContext.registerEvent(new WF_Event_OnRedraw(this.getId()));
		}

	}

	@Override
	public String getName() {
		return "LIST UPDATEONSAVE "+this.getId();
	}


}
