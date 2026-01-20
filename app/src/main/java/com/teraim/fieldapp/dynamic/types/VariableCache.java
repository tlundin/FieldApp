package com.teraim.fieldapp.dynamic.types;

import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.Variable.DataType;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.DbHelper.TmpVal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;

import static com.teraim.fieldapp.utils.Tools.copyKeyHash;
import static com.teraim.fieldapp.utils.Tools.cutKeyMap;


public class VariableCache {
	private static final String TAG = "VariableCache";


    private final static String vCol = "value";
    String varID;
    //private Map<String,List<Variable>> oldcache = new ConcurrentHashMap<String,List<Variable>>();
    private final Map<Map<String, String>, Map<String, Variable>> newcache = new HashMap<Map<String, String>, Map<String, Variable>>();
    private final GlobalState gs;
    private final LogRepository o;
    private Map<String, Variable> currentCache, globalCache;
    private Map<String, String> currentHash;
    private DB_Context myDbContext;


    public VariableCache(GlobalState gs) {
        reset();
        myDbContext = new DB_Context("", null);
        this.gs = gs;
        o = gs.getLogger();
        preCache();

    }

    public void reset() {

        //newcache.clear();
        int i = 0;
        Collection<Map<String, Variable>> allEntries = newcache.values();
        for (Map<String, Variable> entry : allEntries) {
            Collection<Variable> variables = entry.values();
            for (Variable v : variables) {
                v.invalidate();
                i++;
            }
        }
        Log.d(TAG, "RESET: INVALIDATED " + i + " variables");
        newcache.clear();
        globalCache = this.createOrGetCache(null);
        currentCache = globalCache;
    }

    public void setCurrentContext(DB_Context context) {
        currentHash = context.getContext();
        currentCache = this.createOrGetCache(currentHash);
        Log.d(TAG, "currentCache is now based on " + context);
        myDbContext = context;
    }


    public DB_Context getContext() {
        return myDbContext;
    }

    public Map<String, Variable> createEmptyCacheOrGetCache(Map<String, String> myKeyHash) {
        Map<String, Variable> ret = newcache.get(myKeyHash);
        if (ret == null) {
            if (myKeyHash != null) {
                Log.d(TAG, "Creating Empty Cache for " + myKeyHash + " hash: " + myKeyHash.hashCode());
                Map<String, String> copy = copyKeyHash(myKeyHash);
                ret = new HashMap<String, Variable>();
                newcache.put(copy, ret);
            } else
                return createOrGetCache(null);
        }
        return ret;
    }
    Map<String, Map<String, Variable>> preCache = new HashMap<String, Map<String, Variable>>();
    List<String> preCacheVariables = List.of(GisConstants.ObjectID,GisConstants.Typkod);
    public void preCache() {
        long time = System.currentTimeMillis();
        for(String varId: preCacheVariables) {
            //Log.d(TAG, "Precaching all variables for " + varId);
            DbHelper.Selection s = gs.getDb().createSelection(null, varId);
            DbHelper.DBColumnPicker p = gs.getDb().getAllVariableInstances(s);
            Variable v;
            while (p.next()) {
                DbHelper.StoredVariableData sv = p.getVariable();
                List<String> varDesc = gs.getVariableConfiguration().getCompleteVariableDefinition(varId);
                if (varDesc == null) {
                    Log.d(TAG,"skipping precache of variable "+varId);
                    continue;
                }
                v = new Variable(varId, gs.getVariableConfiguration().getVarLabel(varDesc), varDesc, p.getKeyColumnValues(), gs, vCol, sv.value, true, null);
                //Log.d(TAG, "precaching " + varId+" hash: "+p.getKeyColumnValues() + " with value " + sv.value);
                String uid = p.getKeyColumnValues().get("uid");
                if (preCache.get(uid) == null)
                    preCache.put(uid, new HashMap<String, Variable>());
                preCache.get(uid).put(varId, v);
            }
            p.close();

        }
        Log.d(TAG,"precache time: "+(System.currentTimeMillis()-time)+" precache size "+preCache.size());
    }

    public Map<String, Variable> createOrGetCache(Map<String, String> myKeyHash) {
        Map<String, Variable> ret = newcache.get(myKeyHash);
        Map<String, String> copy = null;
        long t = System.currentTimeMillis();
        if (ret == null) {
            if (myKeyHash != null) {

                //Log.d(TAG, "Creating Cache for " + myKeyHash + " hash: " + myKeyHash.hashCode());
                copy = copyKeyHash(myKeyHash);

            }
            else
                Log.d(TAG, "Creating Cache for keyhash null");

            ret = createAllVariablesForKey(copy);
            if (ret == null) {
                //Log.e("vortex","No variables found in db for "+myKeyHash+". Creating empty hash");
                ret = new HashMap<String, Variable>();
            }


            newcache.put(copy, ret);
            Log.d(TAG,"time: "+(System.currentTimeMillis()-t)+" variables created: "+ret.size());
        } else {
            //Log.d(TAG, "Returning existing cache for " + myKeyHash+" : "+ret);

        }

        return ret;
    }

    private void refreshCache(Map<String, String> myKeyHash) {
        //Map<String, Variable> ret = newcache.remove(myKeyHash);
        Map<String, Variable> ret = newcache.get(myKeyHash);
        if (ret != null) {
            for (Variable v : ret.values())
                v.invalidate();
        } else
            Log.e("vortex", "key hash does not exist in refreshcache");
    }

    public void deleteCacheEntry(Map<String, String> myKeyHash) {
        Map<String, Variable> ret = newcache.remove(myKeyHash);
        if (ret != null) {
            Log.d(TAG, "found and deleted " + myKeyHash + " from cache");
        } else
            Log.e("vortex", "key hash does not exist in deletecacheentry." + myKeyHash);
    }

    private Map<String, Variable> createAllVariablesForKey(Map<String, String> myKeyHash) {
        Log.d(TAG,"adding hash "+myKeyHash);
        GlobalState gs = GlobalState.getInstance();
        long time = System.currentTimeMillis();
        Map<String, Variable> ret = null;


        Map<String, TmpVal> map = gs.getDb().preFetchValuesForAllMatchingKeyV(myKeyHash);
        if (map != null) {
            //Create variables.
            Variable v;

            if (!map.isEmpty()) {
                ret = new HashMap<String, Variable>();
                for (String varName : map.keySet()) {

                    TmpVal variableValues = map.get(varName);
                    List<String> row = gs.getVariableConfiguration().getCompleteVariableDefinition(varName);
                    if (row == null) {
                        //Log.e("vortex", "Variable " + varName + " does not exist in variables but exists in Database");
                        //Table table = gs.getVariableConfiguration().getTable();
                        //gs.getVariableConfiguration().getTable().printTable();
                        //Map<String, String> deleteHash = Tools.copyKeyHash(myKeyHash);
                        //Entry is either historical or normal. Delete independently
                        //if (deleteHash!=null) {
                        //    deleteHash.remove("år");
                        //    gs.getDb().deleteVariable(varName, gs.getDb().createSelection(deleteHash, varName), false);
                        //    Log.e("vortex", "Deleted " + varName);
                        //}
                        //String p = null;
                        //p.length();
                    } else {
                        String header = gs.getVariableConfiguration().getVarLabel(row);
                        DataType type = gs.getVariableConfiguration().getnumType(row);
                        String rowKH = gs.getVariableConfiguration().getKeyChain(row);
                        //String variableId = gs.getVariableConfiguration().getVarName(row);
                        int rowKHL = 0;
                        int myKeyhashSize = 0;
                        String[] rowKHA=null;
                        if (myKeyHash != null) {
                            myKeyhashSize = myKeyHash.keySet().size();
                            //Log.d(TAG,"myKSize: "+myKeyhashSize+" myKeyHash: "+myKeyHash.keySet()+"rowKH: "+rowKH);
                        }
                        if (rowKH != null && !rowKH.isEmpty()) {
                            rowKHA = rowKH.split("\\|");
                        }

                        if (rowKHA !=null && myKeyhashSize != rowKHA.length) {

                            Log.d(TAG, "partiell nyckel. IN ROW: " + rowKH + " IN DB: " + (myKeyHash == null ? "null" : myKeyHash.toString()));
                            Log.d(TAG,"searching if there is one unique result on partial key");
                            if (myKeyHash == null) {
                                Log.e("vortex","Keyhash null for "+varName+" Dropping");
                                //gs.getDb().deleteVariable(varName,gs.getDb().createSelection(null, varName),false);
                                continue;
                            } else
                                myKeyHash = gs.getDb().createNotNullSelection(rowKHA,myKeyHash);

                            //Return any value in database that is unique for the given columns as primary key.
                            //år|ruta|gistyp|uid "There exists exactly one combination where ruta = 2222, gistyp not null, uid not null


                        }
                        if (type == DataType.array)
                            v = new ArrayVariable(varName, header, row, myKeyHash, gs, vCol, variableValues.norm, true, variableValues.hist);
                        else
                            v = new Variable(varName, header, row, myKeyHash, gs, vCol, variableValues.norm, true, variableValues.hist);
                        ret.put(varName.toLowerCase(), v);
						//Log.d(TAG,"Added "+varName+" to cache with value n:"+variableValues.norm+" h: "+variableValues.hist);
                    }
                }
            } else
                Log.d(TAG, "Map empty in CreateAllVariablesForKey");


        }
        long ctime = System.currentTimeMillis();
        //Log.d(TAG, "Generating all variables took: " + (ctime - time) + " ms");
        //Log.d(TAG, "Key: " + myKeyHash);
        //Log.d(TAG, "Variables found: "+(ret==null?"null":ret.keySet()));
        return ret;
    }


    public void put(Variable v) {
        Map<String, Variable> map = newcache.get(v.getKeyChain());
        if (map!=null) {
            Log.d(TAG,"successfully added variable "+v.getId()+"to "+v.getKeyChain());
            map.put(v.getId(), v);
        }
    }

    public Variable getGlobalVariable(String varId) {
        return getVariable(null, globalCache, varId, null, null);
    }


    public Variable getVariable(String varId) {
        return getVariable(currentHash, currentCache, varId, null, null);
    }

    public Variable getVariable(String varId, String defaultValue, int dummy) {
        return getVariable(currentHash, currentCache, varId, defaultValue, null);
    }

    public Variable getVariable(Map<String, String> keyChain, String varId) {
        //Log.d(TAG,"in getvar for "+varId+" with keychain "+keyChain);
        if (preCacheVariables.contains(varId)) {
            Map<String, Variable> entry = preCache.get(keyChain.get("uid"));
            if (entry != null) {
                Variable v = entry.get(varId);
                if (v == null)
                    Log.d(TAG, "Variable not found for uid " + keyChain.get("uid"));
                else {
                    //Log.d(TAG, "Precache Variable found for uid " + keyChain.get("uid"));
                    return v;
                }
            }
        }
        return getVariable(keyChain, createOrGetCache(keyChain), varId, null, null);
        //return new Variable(varId,null,null,keyChain,gs,"value",null);
    }


    //A variable that is given a value at start.
    public Variable getCheckedVariable(Map<String, String> keyChain, String varId, String value, Boolean wasInDatabase) {
        //Log.d(TAG,"in getcheckedvar for "+varId+" with keychain "+keyChain);
        return getVariable(keyChain, createOrGetCache(keyChain), varId, value, wasInDatabase);
    }


    //A variable that is given a value at start.

    public Variable getCheckedVariable(String varId, String value, Boolean wasInDatabase) {
        return getVariable(currentHash, currentCache, varId, value, wasInDatabase);
    }







    //Only get the value.

    public String getVariableValue(Map<String, String> keyChain, String varId) {
        Variable v = getVariable(keyChain, varId);
        if (v == null) {
            Log.e("nils", "Varcache returned null!!");
            return null;
        }
        return v.getValue();
    }

    //A variable type that will not allow its keychain to be changed.
    public Variable getFixedVariableInstance(Map<String, String> keyChain, String varId, String defaultValue) {
        List<String> row = gs.getVariableConfiguration().getCompleteVariableDefinition(varId);
        return new FixedVariable(varId, gs.getVariableConfiguration().getEntryLabel(row), row, keyChain, gs, defaultValue, true);
    }


    public Variable getVariable(Map<String, String> hash, Map<String, Variable> cache, String varId, String defaultValue, Boolean hasValueInDB) {
        //Log.d(TAG, "in CACHE GetVariable for " + varId);
        long t0 = System.currentTimeMillis();
        //Log.d(TAG,"cache is "+cache);
        if (varId == null) {
            Log.e("vortex","getVariable in Cache called with VarId = null");
            return null;
        }
        Variable variable = cache.get(varId.toLowerCase());
        if (variable == null) {
            //check if variable has subset of keypairs
            List<String> row = gs.getVariableConfiguration().getCompleteVariableDefinition(varId);
            if (row == null) {
                Log.e("vortex", "Variable definition missing for " + varId);
                o.addYellowText("Variable definition missing for " + varId);
                return null;

            }

            String mColumns = gs.getVariableConfiguration().getKeyChain(row);
            Map<String, String> tryThis = cutKeyMap(mColumns, hash);
            if (tryThis != null && tryThis.isEmpty()) {
                Log.e("vortex", "KEY FAIL for variable "+varId);
                return null;
            }
            cache = createOrGetCache(tryThis);
            variable = cache.get(varId.toLowerCase());
            hash = tryThis;

            if (variable == null) {
                //for (String s:cache.keySet())
                //	Log.d(TAG,"In cache: "+s);
                //Log.d(TAG,"Creating new CacheList entry for "+varId);
                String header = gs.getVariableConfiguration().getVarLabel(row);
                DataType type = gs.getVariableConfiguration().getnumType(row);
                //Log.d(TAG,"T1:"+(System.currentTimeMillis()-t0));
                //TODO: ABRAKA CHANGE TO FALSE BELOW
                if (type == DataType.array)
                    variable = new ArrayVariable(varId, header, row, hash, gs, vCol, defaultValue, hasValueInDB, "*NULL*");
                else {
                    //Log.d(TAG,"varche defval: "+defaultValue+" for "+varId);
                    variable = new Variable(varId, header, row, hash, gs, vCol, defaultValue, hasValueInDB, "*NULL*");
                }
                cache.put(varId.toLowerCase(), variable);
            } else
                Log.d(TAG, "Found " + variable.getId() + " in global cache with value " + variable.getValue() + " varhash:" + tryThis);

        } else {
            //Log.d(TAG,"Found "+variable.getId()+" in cache with value "+variable.getValue()+" varobj: "+variable.toString());

            //hasValueInDB = false, if value is null in DB, NULL if no known value, true if known value.
            if (variable.unknown && hasValueInDB!=null) {
                variable.unknown=false;
            }
            if (variable.getValue() == null && defaultValue != null && !defaultValue.equals(Constants.NO_DEFAULT_VALUE)) {
                Log.d(TAG, "defTrigger on " + defaultValue);
                variable.setDefaultValue(defaultValue);
            }
            //Log.d(TAG,"default value: "+defaultValue);
        }
        //Log.d(TAG,"Te:"+(System.currentTimeMillis()-t0));
        //Log.d(TAG, "variable found with value " + variable.getValue());
        return variable;
    }


    //Check if two maps are equal
    private static boolean Eq(Map<String, String> chainToFind, Map<String, String> varChain) {
        //		Log.d(TAG,"in Varcache EQ");
        if (chainToFind == null && varChain == null)
            return true;
        if (chainToFind == null || varChain == null || chainToFind.size() < varChain.size()) {
            //			Log.d(TAG,"eq returns false. Trying to match: "+(chainToFind==null?"null":chainToFind.toString())+" with: "+(varChain==null?"null":varChain.toString()));
            return false;
        }
        //		Log.d(TAG,"ChainToFind: "+chainToFind.toString());
        //		Log.d(TAG,"VarChain: "+varChain.toString());
        for (String key : varChain.keySet()) {
            if (chainToFind.get(key) == null) {
                //				Log.d(TAG,"eq returns false. Key "+key+" is not in Chaintofind: "+chainToFind.toString());
                return false;
            }
            if (!chainToFind.get(key).equals(varChain.get(key))) {
                //				Log.d(TAG,"eq returns false. Key "+key+" has different value than varchain with same key: "+chainToFind.get(key)+","+varChain.get(key));
                return false;
            }

        }
        return true;
    }

    //Check if two maps are equal
    private static boolean SubsetOf(Map<String, String> chainToFind, Map<String, String> varChain) {
        //		Log.d(TAG,"in Varcache EQ");
        if (chainToFind == null && varChain == null)
            return true;
        if (chainToFind == null || varChain == null || varChain.size() < chainToFind.size()) {
            //			Log.d(TAG,"eq returns false. Trying to match: "+(chainToFind==null?"null":chainToFind.toString())+" with: "+(varChain==null?"null":varChain.toString()));
            return false;
        }
        Log.d(TAG, "ChainToFind: " + chainToFind.toString());
        Log.d(TAG, "VarChain: " + varChain.toString());
        for (String key : chainToFind.keySet()) {
            if (chainToFind.get(key) == null) {
                Log.d(TAG, "SubsetOf returns false. Key " + key + " is not in Chaintofind: " + chainToFind.toString());
                return false;
            }
            if (!chainToFind.get(key).equals(varChain.get(key))) {
                Log.d(TAG, "SubsetOf returns false. Key " + key + " has different value than varchain with same key: " + chainToFind.get(key) + "," + varChain.get(key));
                return false;
            }

        }
        return true;
    }


    public void invalidateOnName(String varId) {
        Log.d(TAG, "invalidating variable named " + varId);
        Variable v = currentCache.get(varId.toLowerCase());

        if (v != null) {
            Log.d(TAG, "Found variable " + v.getId() + ". Invalidating..");
            v.invalidate();
        } else {
            Log.d(TAG, "Could not find variable " + varId + " in invalidateOnName");
        }

    }

    //Invalidate all variables in any group containing at least one instance matching the keyChain.

    public void invalidateOnKey(Map<String, String> keyChain, boolean exactMatch) {
        Log.d(TAG, "invalidating all variables in Cache matching " + keyChain.toString() + " with exactMatch set to " + exactMatch);
        if (exactMatch) {
            //invalidate all variables with the given key.
            this.refreshCache(keyChain);
            //delete it from cache.
            this.deleteCacheEntry(keyChain);
        }
        else {
            for (Map<String, String> chain : newcache.keySet()) {
                if (SubsetOf(keyChain, chain)) {
                    Log.d(TAG, "found subset chain " + chain.toString());
                    this.refreshCache(chain);
                    this.deleteCacheEntry(chain);

                }
            }
        }
    }

    //Delete all variables with the given hash with sync
/*
    public void deleteAll(Map<String, String> keyChain) {
        Log.d(TAG,"In delete all variables!");
        Map<String, Variable> varMap = newcache.remove(keyChain);
        if (varMap==null)
           ; //varMap = createAllVariablesForKey(keyChain);
        else {
            Log.d(TAG, "Already have cached!");

        if (varMap!=null && varMap.values()!=null && !varMap.values().isEmpty()) {
            for (Variable v : varMap.values()) {
                Log.d(TAG, "deleting " + v.getLabel());
                v.deleteValue();
            }
        }

    }
*/


    //Find all variables in cache with a given keyhash that belongs to the given group.
    public List<Variable> findVariablesBelongingToGroup(Map<String, String> keyChain, String groupName) {
        Log.d(TAG, "In FindVariablesBelongingToGroup");
        //Identify matching variables.
        Map<String, Variable> cache = newcache.get(keyChain);
        if (cache != null) {
            Log.d(TAG, "findVariablesBelongingToGroup: " + keyChain.toString() + " size of cache " + cache.size());
            boolean found = false;
            groupName = groupName.toLowerCase();
            Set<String> myKeys = new HashSet<String>();
            for (String varName : cache.keySet()) {
                Log.d(TAG, "Looking at varName: " + varName);
                //Take first in each list.
                if (varName.startsWith(groupName)) {
                    Log.d(TAG, "found one: " + varName);
                    myKeys.add(varName);
                }

            }
            if (myKeys.isEmpty()) {
                Log.d(TAG, "found no variable of group " + groupName);
                return null;
            }
            Log.d(TAG, "myKeys has " + myKeys.size() + " members");
            List<Variable> resultSet = new ArrayList<Variable>();
            for (String vName : myKeys)
                resultSet.add(cache.get(vName));
            return resultSet;
        }
        Log.d(TAG, "No cache exists for" + keyChain);
        return null;

    }

    //Insert when unique key value pair is known.
    //cache last chain to save some time.
    private Map<String,String> prevChain = null;
    private final static String uniqueKey = "uid";
    public boolean turboRemoveOrInvalidate(String uniqueKeyValue, String sub, String variableName, boolean invalidate) {
//        if (prevValue!=null && prevValue.equals(uniqueKeyValue))
        //Log.d(TAG,"turbo1 "+uniqueKeyValue+" "+variableName);
        boolean success =false;
        for (Map<String, String> chain : newcache.keySet()) {
            //Log.d(TAG,"turbo2 chain: "+chain+" uniq: "+uniqueKey+" uval: "+uniqueKeyValue);
            if (chain!=null && chain.containsKey(uniqueKey) && chain.get(uniqueKey).equals(uniqueKeyValue)) {
                //Log.d(TAG,"turbo3 Varname: "+variableName+"cache:"+newcache.get(chain));
                if (sub==null || (chain.containsKey("sub") && sub.equals(chain.get("sub")))) {
                    Variable v = newcache.get(chain).get(variableName.toLowerCase());
                    if (v != null) {

                        Log.d(TAG, "Invalidated " + v.getId() + "SUB: "+sub+" UUID: "+uniqueKeyValue+". Chain eq to prevchain?" + (prevChain != null && prevChain.equals(chain)));
                        if (invalidate)
                            v.invalidate();
                        else
                            v.deleteValue();
                        success=true;
                    }
                    prevChain = chain;
                }
            }
        }
        return success;
    }

    public void insert(String name, Map<String, String> keyHash, String newValue) {
        //Log.d(TAG, "In insert with " + keyHash.toString());
        Map<String, Variable> vars = newcache.get(keyHash);
        if (vars != null) {
            //Log.d(TAG, "finding " + name);
            Variable var = vars.get(name.toLowerCase());
            if (var != null) {
                Log.d(TAG, "replacing value " + var.getValue() + " with " + newValue);
                var.setOnlyCached(newValue);
            } else {
                Log.e("vortex", "did not find variable " + name + " in cache");
                getCheckedVariable(keyHash, name, newValue, true);

            }
        }

    }

    public void delete(String name, Map<String, String> keyHash) {
        Log.d(TAG, "In delete with " + keyHash.toString());
        Map<String, Variable> vars = newcache.get(keyHash);
        if (vars != null) {
            Log.d(TAG, "finding " + name);
            Variable var = vars.get(name.toLowerCase());
            if (var != null) {
                Log.d(TAG, "removing variable " + name);
                //Variable removedVar = vars.remove(name.toLowerCase());
                //if (removedVar!=null) {
                //    Log.d(TAG,"REMOVED!");
                //    removedVar.invalidate();
                //} else
                //    Log.e("vortex","REMOVE FAILED ");
                //if (vars.get(name.toLowerCase())!=null)
                //    Log.d(TAG,"still able to find "+name);
                var.setOnlyCached(null);
            } else {
                Log.d(TAG, "did not find variable " + name + " in cache");
            }
        } else
            Log.d(TAG, "not found!");
    }

    public void printCache() {
        Log.d(TAG, "newCache: ");
        for (Map<String, String> key : newcache.keySet()) {
            if (key != null) {
                Log.d(TAG, key.toString() + ", HASH: " + key.hashCode());
                Map<String, Variable> varMap = newcache.get(key);
                for (String vKey : varMap.keySet()) {
                    Variable v = varMap.get(vKey);
                    if (v.getKeyChain().equals(key))
                        Log.d(TAG, "        " + v.getId() + " värde: " + v.getValue()+" invalid: "+v.isInvalidated()+" isusingdef: "+v.isUsingDefault());
                    else
                        Log.e("vortex", "       " + v.getId() + " Nyckel: " + v.getKeyChain()+" värde: "+v.getValue());
                }

            } else
                Log.d(TAG, "*NULL*");
        }
    }

    private class KeyException extends java.lang.Exception {

        private static final long serialVersionUID = 458116191810109227L;

    }

    //Keep a queue of variables for saving.
    //Trigger insert regularly
    //Save all before sync.

    //private final Queue<Variable> dbQueue = new ConcurrentLinkedQueue<Variable>() ;

    private ScheduledExecutorService scheduleTaskExecutor = null;


    //public void save(Variable variable) {
    //	dbQueue.add(variable);
    //}


}
