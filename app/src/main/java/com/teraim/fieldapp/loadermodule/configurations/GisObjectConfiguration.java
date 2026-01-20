package com.teraim.fieldapp.loadermodule.configurations;

import android.content.Context;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;
import android.util.MalformedJsonException;

import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisPolygonObject;
import com.teraim.fieldapp.loadermodule.JSONConfigurationModule;
import com.teraim.fieldapp.loadermodule.LoadResult;
import com.teraim.fieldapp.loadermodule.LoadResult.ErrorCode;
import com.teraim.fieldapp.log.LogRepository;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.non_generics.NamedVariables;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.PersistenceHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GisObjectConfiguration extends JSONConfigurationModule {
    private static final String TAG = "GisObjectConfiguration";


    private final LogRepository o;
    private final DbHelper myDb;
    private final List<GisObject> myGisObjects = new ArrayList<GisObject>();
    private String myType;
    private final Table varTable;
    private boolean isDebug = false;
    private boolean isProvYtaOrTrakt = false;


    public GisObjectConfiguration(Context context, PersistenceHelper globalPh, PersistenceHelper ph, String fileLocation, String fileName, LogRepository debugConsole, DbHelper myDb, Table t) {
        super(context,globalPh,ph, fileLocation, fileName, fileName);
        this.o = debugConsole;
        this.myDb = myDb;
        //isDatabaseModule=true;
        this.hasSimpleVersion=true;
        this.isDatabaseModule=true;
        this.myType = fileName;
        if (myType!=null&&myType.length()>0) {
            myType = myType.toLowerCase();
            myType =( myType.substring(0,1).toUpperCase() + myType.substring(1));
        }
        varTable = t;
        isDebug = globalPh.getB(PersistenceHelper.DEVELOPER_SWITCH);
        List<String> requiredAttributes = new ArrayList<>();
        requiredAttributes.clear();
        if (isDebug) {
            requiredAttributes.add(GisConstants.RutaID);
            requiredAttributes.add(GisConstants.ObjectID);
        }
    }

    private static String fixedLength(String fileName) {
        if ((20-fileName.length())<=0)
            return fileName;

        String space20 = new String(new char[20-fileName.length()]).replace('\0', ' ');

        return (fileName+space20);
    }

    @Override
    public float getFrozenVersion() {
        return (ph.getF(PersistenceHelper.CURRENT_VERSION_OF_GIS_OBJECT_BLOCKS+fileName));

    }

    @Override
    protected Type getEssenceType() {
        return GisObjectConfiguration.class;
    }

    @Override
    protected void setFrozenVersion(float version) {
        ph.put(PersistenceHelper.CURRENT_VERSION_OF_GIS_OBJECT_BLOCKS+fileName,version);

    }


    public boolean isRequired() {
        return false;
    }


    @Override
    protected LoadResult prepare(JsonReader reader) throws IOException {
        //first should be an array.
        if (!myDb.deleteHistoryEntries(GisConstants.TYPE_COLUMN,myType))
            return new LoadResult(this,ErrorCode.Aborted,"Database is missing column 'ÅR', cannot continue");
        try {
            reader.beginObject();
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("features")) {
                    reader.beginArray();
                    return null;
                } else
                    reader.skipValue();
            }
        } catch (IOException e) {
            e.printStackTrace();
            ;
            o.addCriticalText("Error reading import file header. Check syntax of Version field on the first row");
            return new LoadResult(this,ErrorCode.IOError);
        };

        ;
        o.addCriticalText("Could not find beginning of data (features) in input file");
        return new LoadResult(this,ErrorCode.IOError);
    }

    //Parses one row of data, then updates status.
    @Override
    public LoadResult parse(JsonReader reader) throws IOException {
        Location myLocation;
        try {

            JsonToken tag = reader.peek();

            if (tag.equals(JsonToken.END_ARRAY)) {
                //end array means we are done.
                this.setEssence();
                ;
                o.addText("Found "+myGisObjects.size()+" objects");
                freezeSteps = myGisObjects.size();
                Log.d(TAG,"Found "+myGisObjects.size()+" objects of type "+fileName);
                //freezeSteps=myBlocks.size();
                return new LoadResult(this,ErrorCode.parsed);
            }
            else if (tag.equals(JsonToken.BEGIN_OBJECT)) {
                reader.beginObject();
                Map<String,String>keyChain = new HashMap<String,String>();
                Map<String,String> attributes = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
                String mType=null;

                //feature or geometry in any order.
                boolean featureF=false,geometryF=false,propF=false;
                int attrCount1=0;
                while (!(geometryF&&featureF&&propF)) {
                    //avoid spiraling forever.
                    attrCount1++;
                    if (attrCount1 > 3) {
                        if (!geometryF) {
                            ;
                            o.addCriticalText("Attribute Geometry missing in Json file " + myType);
                            return new LoadResult(this, ErrorCode.ParseError, "Attribute Geometry missing in " + myType);
                        }
                        break;
                    }
                    //Log.d(TAG,reader.toString());
                    String nName = reader.nextName();
                    switch (nName) {
                        case "type":
                            //This type attribute is discarded
                            //Log.d(TAG,"hasType");
                            this.getAttribute(reader);
                            featureF = true;
                            break;
                        case "properties":
                            reader.beginObject();
                            while(reader.hasNext()) {
                                //Log.d(TAG,"PEEK: "+reader.peek());
                                if (reader.peek()!=JsonToken.NUMBER) {
                                    String name = reader.nextName();//.toLowerCase();
                                    attributes.put(name, this.getAttribute(reader));
                                }
                            }
                            //Log.d(TAG,attributes.toString());
                            //end attributes
                            reader.endObject();
                            //Log.d(TAG,reader.toString());
                            String uuid = attributes.remove(GisConstants.FixedGid);
                            //Log.d(TAG,"FixedGid: "+uuid);
                            String rutaId = attributes.remove(GisConstants.RutaID);

                            String objectId = attributes.get(GisConstants.ObjectID);
                            if (uuid!=null) {
                                uuid = uuid.replace("{","").replace("}","");
                                //Log.d(TAG,"FixedGid: "+uuid);
                                keyChain.put("uid", uuid);
                            }
                            else {
                                return new LoadResult(this,ErrorCode.Aborted,"missing 'FIXEDGID', cannot continue");
                                //generatedUID=true;
                                //Log.e("vortex","Missing Global ID for ruta: "+rutaId+" objectid: "+objectId+" gistyp: "+fileName);

                                //keyChain.put("uid", UUID.randomUUID().toString());
                                //attributes.put("GENERATEDUID","true");
                            }
                            //keyChain.put("år", Constants.HISTORICAL_TOKEN_IN_DATABASE);


                            if (rutaId==null) {
                                Log.e("vortex", "ingen ruta ID!!!!");
                                //return new LoadResult(this,ErrorCode.ParseError,"MISSING Ruta ID for globalid: "+uuid+" objectid: "+objectId+" gistyp: "+fileName);
                            }
                            else {
                                //Log.d(TAG,"RUTA ID: "+rutaId);
                                keyChain.put(NamedVariables.AreaTerm, rutaId);
                            }

                            if (objectId == null) {
                                Log.e("vortex", "ingen object ID!!!!");
                                //return new LoadResult(this,ErrorCode.ParseError,"MISSING OBJECT ID");

                            } //else
                            //	Log.d(TAG,"OBJECT ID: "+objectId);

                            keyChain.put(GisConstants.TYPE_COLUMN, myType);

                            //Add geotype to attributes so that the correct object can be used at export.

                            attributes.put(GisConstants.Geo_Type, mType);
                            //Log.d(TAG,"added new gis object");
                            propF=true;
                            break;
                        case "geometry":
                            //Log.d(TAG,"hasGeo");
                            geometryF = true;
                            //Coordinates.
                            reader.beginObject();

                            //Next can be either coordinates or geo type.

                            int attrCount2 = 0;
                            boolean geoTypeF = false, coordinatesF = false;
                            Map<String, List<Location>> polygonSet = new HashMap<String, List<Location>>();
                            List<Location> myCoordinates=null;

                            while (!(geoTypeF && coordinatesF)) {
                                attrCount2++;
                                if (attrCount2 > 2) {
                                    if (!geoTypeF) {
                                        ;
                                        o.addCriticalText("Geotype eg. 'Polygon' is missing in Json file " + myType);
                                        return new LoadResult(this, ErrorCode.ParseError, "Geotype missing in " + myType);

                                    } else {
                                        ;
                                        o.addCriticalText("Attribute Coordinates missing in Json file " + myType);
                                        return new LoadResult(this, ErrorCode.ParseError, "Coordinates missing in " + myType);

                                    }

                                }

                                nName = reader.nextName();
                                //Log.d(TAG, nName);
                                switch (nName) {

                                    case "type":

                                        //This is the geotype, eg. "polygon"
                                        mType = this.getAttribute(reader);
                                        //Log.d(TAG,"type matched: "+mType);
                                        geoTypeF = true;
                                        if (mType == null) {
                                            ;
                                            o.addCriticalText("Type field expected (point, polygon..., but got null");
                                            Log.e("vortex", "type null!");
                                            return new LoadResult(this, ErrorCode.ParseError, "Type field expected (point, polygon..., but got null");
                                        }
                                        mType = mType.trim();
                                        break;
                                    case "coordinates":
                                        coordinatesF = true;

                                        //Always start with an array [
                                        //Log.d(TAG, reader.toString());

                                        reader.beginArray();
                                        //If single point, next must be number.
                                        if (reader.peek() == JsonToken.NUMBER) {
                                            //Log.d(TAG, "point");
                                            myGisObjects.add(new GisObject(keyChain, Collections.singletonList(readLocation(reader)), attributes));
                                        } else {
                                            //next must be an array. Otherwise error.
                                            //[->[
                                            //If multipoint or Linestring, next is a number.
                                            //[[->1,2,3],[...]]

                                            //Log.d(TAG, "bA2");
                                            reader.beginArray();

                                            if (reader.peek() == JsonToken.NUMBER) {
                                                //Log.d(TAG, "linestring/multipoint");
                                                myCoordinates = (readAllLocations(reader));
                                            } else {
                                                int id = 1;
                                                //Log.d(TAG, "bA3");

                                                reader.beginArray();
                                                //If polygon next is a number.
                                                //"coordinates": [
                                                //[ [100.0, 0.0], [101.0, 0.0], [101.0, 1.0],
                                                //[100.0, 1.0], [100.0, 0.0] ]
                                                //]
                                                //Log.d(TAG,reader.toString());
                                                if (reader.peek() == JsonToken.NUMBER) {
                                                    boolean stillMore = true;
                                                    while (stillMore) {
                                                        //Log.d(TAG, "poly");
                                                        polygonSet.put(id + "", readAllLocations(reader));
                                                        id++;
                                                        reader.endArray();
                                                        if  (reader.peek() != JsonToken.BEGIN_ARRAY) {
                                                            stillMore=false;
                                                        } else {
                                                            //Log.d(TAG,"found another poly");
                                                            reader.beginArray();
                                                        }

                                                    }
                                                } else {
                                                    //Multipolygon - Array of polygon arrays.
                                                    //Log.d(TAG, "bA4");
                                                    reader.beginArray();
                                                    if (reader.peek() == JsonToken.NUMBER) {
                                                        boolean stillMore = true;
                                                        while (stillMore) {
                                                            polygonSet.put(id + "", readAllLocations(reader));
                                                            id++;
                                                            //end this poly..look if more.
                                                            while (reader.peek()==JsonToken.END_ARRAY)
                                                                reader.endArray();
                                                            if  (reader.peek() != JsonToken.BEGIN_ARRAY) {
                                                                stillMore=false;
                                                            } else {
                                                                //Log.d(TAG,"found another multi or another poly.");
                                                                reader.beginArray();
                                                                if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                                                                    //Log.d(TAG,"found another multi");
                                                                    reader.beginArray();
                                                                }

                                                            }
                                                        }
                                                    }
                                                }

                                            }

                                        }
                                        //Always end with endarrays.

                                        while (reader.peek() == JsonToken.END_ARRAY)
                                            reader.endArray();



                                        break;



                                    default:
                                        Log.d(TAG,"in default...not good: "+reader.peek()+"::::"+reader.toString());
                                        List<String> skippies = new ArrayList<>();
                                        while (reader.hasNext()) {
                                            String skipped = this.getAttribute(reader);
                                            if (skipped.length() > 0)
                                                skippies.add(skipped);
                                            //this.getAttribute(reader);

                                        }
                                        if (skippies.size() > 0 && isDebug) {
                                            ;
                                            o.addCriticalText("");
                                            o.addCriticalText("Skipped " + skippies.size() + " attributes in file " + getFileName() + ":");
                                            for (String skip : skippies) {
                                                o.addCriticalText(skip);
                                            }
                                            Log.d(TAG,"Skipped " + skippies.size() + " attributes in file " + getFileName() + ":");

                                        }

                                        //Log.d(TAG,"nasdaq "+reader+"PEEK: "+reader.peek());
                                        break;




                                }
                            }

                            //now we have type and coordinates.

                            switch (mType) {
                                case GisConstants.LINE_STRING:
                                case GisConstants.MULTI_POINT:
                                    //Log.d(TAG,"multip");
                                    if (myCoordinates!=null && !myCoordinates.isEmpty())
                                        myGisObjects.add(new GisObject(keyChain, myCoordinates, attributes));
                                    else {
                                        ;
                                        o.addCriticalText("missing coordinates for Linestring/multipoint in "+getFileName());
                                        Log.e("vortex", "No coordinates for multipoint in " + myType + "!");

                                    }

                                    break;

                                case GisConstants.POLYGON:
                                    //Log.d(TAG,"poly");
                                    myGisObjects.add(new GisPolygonObject(keyChain, polygonSet, attributes));
                                    break;
                                case GisConstants.MULTI_POLYGON:
                                    //Log.d(TAG,"multi");
                                    myGisObjects.add(new GisPolygonObject(keyChain, polygonSet, attributes));
                                    break;
                                /*default:
                                    ;
                                    o.addCriticalText("Unrecognized gis type: "+mType+" in "+getFileName());
                                    Log.e("bebox","unrecognized type: "+mType);
                                    break;
                                    */

                            }
                            //Log.d(TAG,"PEEK: "+reader.peek());
                            //Log.d(TAG,reader.toString());
                            reader.endObject();
                            break;
                    }
                }
                //end row
                reader.endObject();




            } else {
                ;
                o.addCriticalText("Parse error when parsing file "+fileName+". Expected Object type at "+reader.toString());
                Log.e("vortex","Parse error when parsing file "+fileName+". Expected Object type at "+reader.toString()+" peek: "+reader.peek());
                return new LoadResult(this,ErrorCode.ParseError,"Parse error when parsing file "+fileName+". Expected Object type at "+reader.toString()+" peek: "+reader.peek());
            }
        } catch (MalformedJsonException je) {
            Tools.printErrorToLog(o, je,null);
            return new LoadResult(this,ErrorCode.IOError);
        } catch (Exception e) {
            ;
            o.addCriticalText("Fel i styrfil "+fileName);
            Tools.printErrorToLog(o,e,null);
            return new LoadResult(this,ErrorCode.IOError);
        }
        return null;
    }


    private List<Location> readAllLocations(JsonReader reader) {
        List<Location> myLocation = new ArrayList<>();
        try {
            while (!reader.peek().equals(JsonToken.END_ARRAY)) {
                if (reader.peek().equals(JsonToken.BEGIN_ARRAY))
                    reader.beginArray();
                myLocation.add(readLocation(reader));
                reader.endArray();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return myLocation;
    }

    private Location readLocation(JsonReader reader) {
        double x = 0;
        double y = 0;
        try {
            x = reader.nextDouble();
            y = reader.nextDouble();
            if (!reader.peek().equals(JsonToken.END_ARRAY)) {
                double z = reader.nextDouble();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new SweLocation(x,y);
    }


    @Override
    protected void setEssence() {
        essence = null;
    }



    private boolean firstCall = true;
    private Set<String> missingVariables=null;
    private final Set<GisObject>dubletter = new HashSet<>();
    private final Set<String>seenAlready=new HashSet<>();
    @Override
    public void freeze(int counter) {
        if (counter==-1 || myGisObjects==null || myGisObjects.isEmpty()) {
            Log.d(TAG,"nothing to freeze!");
            newVersion=-1;
            return;
        }
        if (firstCall) {
            missingVariables=new HashSet<String>();

            boolean generatedUID = false;
            if (generatedUID) {
                ;
                o.addYellowText("At least one row in file "+fileName+" did not contain FixedGID (UUID). Generated value will be used");
            }
            myDb.beginTransaction();
            //Log.d(TAG,"Transaction begins");
            firstCall = false;
            //Log.d(TAG,"keyhash for first: "+myGisObjects.get(0).getKeyHash().toString());
            dubletter.clear();

        }

        //Insert GIS variables into database
        GisObject go = myGisObjects.get(counter);
        String coordS = go.coordsToString();

        if (!myDb.fastHistoricalInsert(go.getKeyHash(),
                GisConstants.GPS_Coord_Var_Name,coordS)) {
            ;
            o.addCriticalText("Row: "+counter+". Insert failed for "+GisConstants.GPS_Coord_Var_Name+". Hash: "+go.getKeyHash().toString());
        }
        if (isDebug) {

            boolean s = seenAlready.add(coordS);
            if (!s) {
                //Log.d(TAG,"added "+coordS);
                dubletter.add(go);
            }
        }
        Map<String, String> attr = go.getAttributes();

        for (String key:attr.keySet()) {
            //check for status variables before insert.
            //Log.d(TAG,"key: "+key+" value: "+attr.get(key));
            if (key.equalsIgnoreCase(NamedVariables.PYSTATUS) || key.equalsIgnoreCase(NamedVariables.TRAKTSTATUS)) {
                String statusVariableName= Constants.STATUS_VARIABLES_GROUP_NAME+":"+"status_"+myType.toLowerCase();
                //Log.d(TAG,"status variable found, setting "+statusVariableName);
                myDb.fastInsert(new HashMap<>(go.getKeyHash()), statusVariableName, attr.get(key));
                /*
                DbHelper.DBColumnPicker cp = myDb.getLastVariableInstance(myDb.createSelection(go.getKeyHash(), statusVariableName));
                if (cp != null) {
                    while (cp.next()) {
                        Log.d(TAG, "picker return for " + cp.getVariable().name + " is\n" + cp.getKeyColumnValues());
                        String varValue = cp.getVariable().value;
                        Log.d(TAG, "VALUE: " + varValue);
                    }
                    cp.close();
                }
                */
            } else if (!myDb.fastHistoricalInsert(go.getKeyHash(),key,attr.get(key))) {
                ;
                o.addCriticalText("Row: "+counter+". Insert failed for "+key+". Hash: "+go.getKeyHash().toString());
            }
            if (isDebug && varTable != null) {
                if (varTable.getRowFromKey(key)==null) {
                    missingVariables.add(key);
                    //Log.d(TAG,"key missing: "+key);
                }
            }
            if (key.equalsIgnoreCase(NamedVariables.PROVYTE_TYP) || (key.equalsIgnoreCase(NamedVariables.GIS_TYPE) && attr.get(key).equalsIgnoreCase("trakter"))) {
                this.isProvYtaOrTrakt = true;
            }
        }

        if (this.freezeSteps==(counter+1)) {
            //Log.d(TAG,"Transaction ends");
            myDb.endTransactionSuccess();

            if (isDebug && !missingVariables.isEmpty()) {
                ;
                o.addCriticalText("VARIABLES MISSING IN VARIABLES CONFIGURATION FOR " + this.fileName + ":");

                for (String m : missingVariables) {
                    ;
                    o.addCriticalText(m);
                }
            }
            if (isDebug && !dubletter.isEmpty()) {
                ;
                o.addCriticalText("Filen "+fileName+"har dubletter: ");
                for (GisObject g:dubletter) {
                    o.addCriticalText(g.getKeyHash().toString());
                }
            }
            if (isDebug && dubletter.isEmpty()) {
                Log.d(TAG,seenAlready.toString());
            }
        }

    }


    public boolean isProvYtaOrTrakt() {
        return isProvYtaOrTrakt;
    }

}
