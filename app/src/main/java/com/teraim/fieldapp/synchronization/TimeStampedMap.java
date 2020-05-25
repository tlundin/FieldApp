package com.teraim.fieldapp.synchronization;

import android.content.ContentValues;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by Terje on 2016-11-14.
 */
public class TimeStampedMap {

    //uid, mapped to variable id, mapped to timestamp.
    private final Map<Unikey,Map<String,ContentValues>> myMap = new HashMap<>();

    private int size = 0;



    public void add(Unikey uniqueKey, String varName, ContentValues cv) {
        //Log.d("bang","uid: "+uniqueKey+" vn: "+varName);

        Map<String, ContentValues> ves = myMap.get(uniqueKey);

        if (ves==null) {
            ves = new HashMap<>();
            myMap.put(uniqueKey,ves);
        }

        ContentValues existingCV= ves.get(varName);

        if (existingCV == null) {
            ves.put(varName, cv);
            size++;
            //if (natura)
            //    Log.d("froobos","Adding NATURA: "+cv.getAsString("timestamp"));
        }
        else {
            //Log.d("zoobaz","VAR: "+varName+" ts: "+existingCV);
            try {
                long existingStamp = existingCV.getAsLong("timestamp");
                long newTimeStamp = cv.getAsLong("timestamp");
                //if (existingStamp!=null && newTimeStamp!=null) {
                //    long existing = Long.parseLong(existingStamp);
                //    long newTs = Long.parseLong(newTimeStamp);
                    //if (natura) {
                    //    Log.d("froobos","Replaing NATURA: existing: "+existing+" new: "+newTs);
                    //}
                    if (existingStamp < newTimeStamp)
                        ves.put(varName, cv);

                    //Log.d("zoobaz","SWAP!");
                //}

            } catch (NumberFormatException ignored) {

            }
        }

    }


    public ContentValues get(Unikey uniqueKey, String varName) {
        Map<String, ContentValues> ves = myMap.get(uniqueKey);

        if (ves!=null && ves.get(varName)!=null) {
            Log.d("zoobaz","found variable "+varName);
            return ves.get(varName);
        }

        return null;
    }

    public Map<String, ContentValues> get(Unikey uniqueKey) {
        return myMap.get(uniqueKey);
    }

    public int size() {
        return size;
    }

    //Erase all entries that has the corresponding keys and variable pattern.
    //key can be both key and cv values.
    public int delete(Map<String,String> keys, String pattern) {


        Log.d("bascar", "KEYS :" + keys);
        //check that each key exists.

        String uid = keys.get("uid");
        if (uid == null) {
            Log.e("bascar", "uid null in eraseall");
            return 0;
        }
        Map<String, ContentValues> vars;
        if (pattern==null){
           vars = (myMap.remove(Unikey.FindKeyFromParts(uid, null, myMap.keySet())));
           if (vars!=null) {
               Log.d("bascar", "deleteall removed something!");
               return vars.size();
           } else
               return 0;
        } else {
            vars = myMap.get(Unikey.FindKeyFromParts(uid, null, myMap.keySet()));
            int result = 0;
            for(String var:vars.keySet()) {
                if (var.matches(pattern) ) {
                    Log.d("bascar","deleting "+var);
                    result++;
                }
            }
            return result;
        }

    }


     /*    else {

            String spy = keys.remove("spy");

            Map<String, ContentValues> vars = myMap.get(Unikey.FindKeyFromParts(uid, spy, myMap.keySet()));
            Log.e("myMap","myMap: "+myMap.keySet()+"\nLooking for\n "+uid+" , "+spy);
            if (vars != null) {
                Log.d("bascar", "found vars");
                for (String var : vars.keySet()) {
                    if (pattern == null || var.matches(pattern)) {
                        Log.d("bascar", "found var: " + var);
                        //now each remaining key must be in contentvalues.
                        ContentValues cv = vars.get(var);
                        boolean match = true;
                        for (String key : keys.keySet()) {
                            Log.d("bascar", "key " + key + " cv: "+cv);
                            if (cv.containsKey(key)) {
                                //check equal.
                                Log.d("bascar", "key " + key + " found in cval");
                                String cVal = cv.getAsString(key);
                                String val = keys.get(key);
                                if ((val == null && cVal == null) ||
                                        ((val != null && cVal != null) &&
                                                (val.equals(cVal) ||
                                                        val.equals("NN")))) {
                                    Log.d("bascar", "key val" + val + " matches in cval");

                                } else {
                                    match = false;
                                    break;
                                }
                            } else {
                                match = false;
                                break;
                            }

                        }
                        if (match) {
                            Log.d("bascar", "full match for variable " + var + ". Deleting...");
                            if (vars.remove(var) != null) {
                                Log.d("bascar", " removed!");
                                result++;
                            } else
                                Log.e("bascar", "not removed!");
                        }
                    }
                }
            }
        }
        return result;
    }


*/



    public void delete(Unikey uniqueKey, String variableName) {
        //Log.d("bascar","In deleteTimeSTMap with: "+uniqueKey+","+variableName);
        if (uniqueKey==null || variableName==null) {
            return;
        }
        Map<String, ContentValues> ves = myMap.get(uniqueKey);
        if (ves!=null && ves.get(variableName)!=null) {
            Log.d("bascar","Deleting "+variableName+" from Timestamped map");
            ves.remove(variableName);
            if (ves.isEmpty())
                myMap.remove(uniqueKey);
            return;
        }
        Log.d("bascar","no entry for "+variableName+" in sync cache");

    }


    public Set<Unikey> getKeySet() {
            return myMap.keySet();
    }

    //return key if it exists, otherwise create.
    public Unikey getKey(String uid, String spy) {
        Unikey key = Unikey.FindKeyFromParts(uid,spy,myMap.keySet());
        if (key==null)
            key = new Unikey(uid,spy);
        //else
         //   Log.d("bascar","found existing key for "+uid);
        return key;

    }
}
