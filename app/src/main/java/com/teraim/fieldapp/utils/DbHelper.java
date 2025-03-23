package com.teraim.fieldapp.utils;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.ArrayVariable;
import com.teraim.fieldapp.dynamic.types.Location;
import com.teraim.fieldapp.dynamic.types.SweLocation;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.log.LoggerI;
import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.SyncEntry;
import com.teraim.fieldapp.synchronization.SyncEntryHeader;
import com.teraim.fieldapp.synchronization.SyncReport;
import com.teraim.fieldapp.synchronization.SyncStatus;
import com.teraim.fieldapp.synchronization.SyncStatusListener;
import com.teraim.fieldapp.synchronization.TimeStampedMap;
import com.teraim.fieldapp.synchronization.Unikey;
import com.teraim.fieldapp.synchronization.VariableRowEntry;
import com.teraim.fieldapp.ui.MenuActivity.UIProvider;
import com.teraim.fieldapp.utils.Exporter.ExportReport;
import com.teraim.fieldapp.utils.Exporter.Report;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

@SuppressWarnings("SyntaxError")
public class DbHelper extends SQLiteOpenHelper {

    /* Database Version*/ private static final int DATABASE_VERSION = 18;/* Books table name*/
    private static final String TABLE_VARIABLES = "variabler";
    public static final String TABLE_TIMESTAMPS = "timestamps";


    public static final String TABLE_AUDIT = "audit";
    public static final String TABLE_SYNC = "sync";


    private static final String VARID = "var";
    private static final String VALUE = "value";
    private static final String TIMESTAMP = "timestamp";
    public static final String LAG = "lag";
    public static final String AUTHOR = "author";
    public static final String YEAR="år";
    private static final String[] VAR_COLS = new String[]{TIMESTAMP, AUTHOR, LAG, VALUE};
    //	private static final Set<String> MY_VALUES_SET = new HashSet<String>(Arrays.asList(VAR_COLS));

    private static final int NO_OF_KEYS = 10;
    private static final String SYNC_SPLIT_SYMBOL = "_$_";
    public SQLiteDatabase db;
    private PersistenceHelper globalPh = null;

    private final Map<String, String> realColumnNameToDB = new HashMap<String, String>();
    private final Map<String, String> dbColumnNameToReal = new HashMap<String, String>();

    private final Context ctx;

    public static enum Table_Timestamps {
        LABEL,
        VALUE,
        SYNCGROUP
    }
    public void eraseSyncObjects() {
        db().delete(TABLE_SYNC,null,null);
    }

    //This function attempts to recover a newer  uuid from a old uuid using the variable GlobalID as key.
    public String findUIDFromGlobalId(String uid) {
        String query = "SELECT " + this.getDatabaseColumnName("uid") + " FROM " + TABLE_VARIABLES + " WHERE " + VALUE + " = '{"+ uid + "}' AND " + VARID + " = '" + GisConstants.GlobalGid + "' AND " + getDatabaseColumnName("år") + " = '" + Constants.HISTORICAL_TOKEN_IN_DATABASE + "'";
        String res=null;
        //Log.d("quinto",query);
        Cursor resultSet = db().rawQuery(query, null);
        if (resultSet.moveToNext())
            res = resultSet.getString(0);
        resultSet.close();
        return res;
    }

    public String findVarFromUID(String uid,String variableName) {
        String query = "SELECT " + VALUE + " FROM " + TABLE_VARIABLES + " WHERE " + this.getDatabaseColumnName("uid") + " = '" + uid + "' AND " + VARID + " = '" + variableName + "' AND " + getDatabaseColumnName("år") + " = '" + Constants.HISTORICAL_TOKEN_IN_DATABASE + "'";
        Cursor cursor = db().rawQuery(query, null);
        String result = null;
        if (cursor.moveToNext())
            result = cursor.getString(0);
        cursor.close();
        return result;
    }

    public Map<String,String> createNotNullSelection(Map<String, String> myKeyHash) {
        boolean first = true;
        String query = "";
        String cols = "";
        String ar = null;String arval=null;
        Map<String,String> newKeyHash = new HashMap<>();
        for (String key:myKeyHash.keySet()) {
            if (key.equalsIgnoreCase("ÅR")) {
                ar=key;
                arval=myKeyHash.get(key);
                continue;
            }
            String value = myKeyHash.get(key);
            if ("?".equals(value)) {
                value = " NOT NULL";

            } else
                value = "= '"+value+"'";
            if (first)  {
                first=false;
            } else {
                query += " AND ";
                cols+=",";
            }

            String col = getDatabaseColumnName(key);
            Log.d("graaa","getdbcol ret "+col+" for key "+key);
            cols +=col;
            query+=col+" "+value;
            Log.d("notnull","query now "+query);
        }
        //db.delete(TABLE_VARIABLES,"L4 = '?'",null);
        //return myKeyHash;}

        Cursor c = db().rawQuery(String.format("SELECT DISTINCT %s FROM %s WHERE %s", cols, TABLE_VARIABLES, query),null);
        if (c.moveToNext()) {
            for (int i = 0;i < c.getColumnCount();i++) {
                Log.d("vortex","COL: "+c.getColumnName(i)+":"+c.getString(i));
                newKeyHash.put(getRealColumnNameFromDatabaseName(c.getColumnName(i)),c.getString(i));
            }

        }
        if (c.moveToNext()) {
            Log.e("vortex","MORE THAN ONE RESULT!!!");
        }
        Log.d ("vortex","now after C with keyhash: "+newKeyHash);
        if (!newKeyHash.isEmpty()) {
            if (ar!=null)
                newKeyHash.put(ar,arval);
            return newKeyHash;
        }
        Log.e("vortex","failed to resolve unknown");
        LoggerI o = GlobalState.getInstance().getLogger();
        if (o!=null) {
            o.addRow("");
            o.addRedText("Failed to resolve unknown in context "+myKeyHash);
        }
        return null;
    }

    public Map<String,String> createNotNullSelection(String[] rowKHA, Map<String, String> myKeyHash) {

        String query = "";
        String cols = "";
        boolean first=true;
        //The new keyhash to return
        Map<String,String> myNewKeyHash = new HashMap<>();
        for (String key: rowKHA) {

            String existingValue = myKeyHash.get(key);
            if(existingValue==null) {
                existingValue = "NOT NULL";
            } else
                existingValue = "= '"+existingValue+"'";
            if (first)  {
                first=false;
            } else {
                query += " AND ";
                cols+=",";
            }

            String col = getDatabaseColumnName(key);
            cols +=col;
            query+=col+" "+existingValue;
            Log.d("notnull","query now "+query);
        }
        Log.d("notnull","final query: "+query);

        Cursor c = db().rawQuery(String.format("SELECT DISTINCT %s FROM %s WHERE %s", cols, TABLE_VARIABLES, query),null);
        if (c.moveToNext()) {
            for (int i = 0;i < c.getColumnCount();i++) {
                Log.d("vortex","COL: "+c.getColumnName(i)+":"+c.getString(i));
                myNewKeyHash.put(getRealColumnNameFromDatabaseName(c.getColumnName(i)),c.getString(i));
            }

        }
        if (c.moveToNext()) {
            Log.e("vortex","MORE THAN ONE RESULT!!!");
        }
        Log.d ("vortex","now after C with keyhash: "+myNewKeyHash);
        return myNewKeyHash;
    }



    public class LocationAndTimeStamp {
        private final long AnHour = 3600 * 1000;
        private final long HalfAnHour = AnHour/2;
        private final long QuarterOfAnHour = AnHour/4;
        private final long timeSinceRegistered;
        public final Location location;

        LocationAndTimeStamp(long timeSinceRegistered, Location loc) {
            location=loc;
            this.timeSinceRegistered = timeSinceRegistered;
        }
        public boolean isOverAnHourOld() { return timeSinceRegistered > AnHour; }
        public boolean isOverHalfAnHourOld() {
            return timeSinceRegistered > HalfAnHour;
        }
        public boolean isOverAQuarterOld() {
            return timeSinceRegistered > QuarterOfAnHour;
        }

        public long getMostRecentTimeStamp() {
            return timeSinceRegistered;
        }
    }

    private final static long TenDays = 3600*24*5*2*1000;

    public Map<String,LocationAndTimeStamp> getTeamMembers(String team, String user) {
        HashMap<String, LocationAndTimeStamp> ret = null;

        Cursor qx = db().rawQuery("select author, value, max(timestamp) as t from variabler where var = 'GPS_X' and "+LAG+" like '"+team+"' and "+AUTHOR+" <> '"+user+"' group by "+AUTHOR, null);
        Cursor qy = db().rawQuery("select author, value, max(timestamp) as t from variabler where var = 'GPS_Y' and "+LAG+" like '"+team+"' and "+AUTHOR+" <> '"+user+"' group by "+AUTHOR, null);
        while (qx!=null && qx.moveToNext() && qy.moveToNext()) {
            long timeStamp = qx.getLong(2);
            String teamMemberName = qx.getString(0);
            long timeSinceRegistered = (System.currentTimeMillis() - timeStamp);
            if (timeSinceRegistered > TenDays) {
                Log.d("bortex","timestamp for "+teamMemberName+" is older than 10 days.");
            } else {
                if (ret == null)
                    ret = new HashMap<String, LocationAndTimeStamp>();
                Log.d("bortex", "Adding " + teamMemberName);
                ret.put(teamMemberName, new LocationAndTimeStamp(timeSinceRegistered, new SweLocation(qx.getString(1), qy.getString(1))));
            }
        }
        qx.close();
        qy.close();

        return ret;

    }

    //Helper class that wraps the Cursor.
    public class DBColumnPicker {
        final Cursor c;
        private static final String NAME = "var", VALUE = "value", TIMESTAMP = "timestamp", LAG = "lag", CREATOR = "author";

        DBColumnPicker(Cursor c) {
            this.c = c;
        }

        public StoredVariableData getVariable() {
            return new StoredVariableData(pick(NAME), pick(VALUE), pick(TIMESTAMP), pick(LAG), pick(CREATOR));
        }

        public Map<String, String> getKeyColumnValues() {
            Map<String, String> ret = new HashMap<String, String>();
            Set<String> keys = realColumnNameToDB.keySet();
            String col = null;
            for (String key : keys) {
                col = realColumnNameToDB.get(key);
                if (col == null)
                    col = key;
                if (pick(col)!= null)
                    ret.put(key, pick(col));
            }
            //Log.d("nils","getKeyColumnValues returns "+ret.toString());
            return ret;
        }

        private String pick(String key) {
            return c.getString(c.getColumnIndex(key));
        }

        public boolean moveToFirst() {
            return c != null && c.moveToFirst();
        }

        public boolean next() {
            boolean b = c.moveToNext();
            if (!b)
                c.close();
            return b;
        }

        public void close() {
            c.close();
        }

    }

    public SQLiteDatabase db() {
        if (db == null || !db.isOpen()) {
            Log.d("DB","Called getwriteabledb");
            db = this.getWritableDatabase();
        }
        return db;
    }

    public DbHelper(Context context, Table t, PersistenceHelper globalPh, PersistenceHelper appPh, String bundleName) {
        super(context, bundleName, null, DATABASE_VERSION);
        db = this.getWritableDatabase();
        Log.d("DB", "Bundle name: " + bundleName +" DATABASE VERSION "+DATABASE_VERSION);
        ctx = context;


        this.globalPh = globalPh;
        if (t != null)
            init(t.getKeyParts(), appPh);
        else {
            Log.d("nils", "Table doesn't exist yet...postpone init");
        }


    }

    public void closeDatabaseBeforeExit() {
        if (db != null) {
            db.close();
            Log.e("vortex", "database is closed!");
        }
    }

    private void init(ArrayList<String> keyParts, PersistenceHelper appPh) {

        //check if keyParts are known or if a new is needed.

        //Load existing map from sharedStorage.
        String colKey;
        Log.d("nils", "DBhelper init");
        for (int i = 1; i <= NO_OF_KEYS; i++) {

            colKey = appPh.get("L" + i);
            //If empty, I'm done.
            if (colKey.equals(PersistenceHelper.UNDEFINED)) {
                Log.d("nils", "didn't find key L" + i);
                break;
            } else {
                realColumnNameToDB.put(colKey, "L" + i);
                dbColumnNameToReal.put("L" + i, colKey);
            }
        }
        //Now check the new keys. If a new key is found, add it.
        if (keyParts == null) {
            Log.e("nils", "Keyparts were null in DBHelper");
        } else {
            //Log.e("nils","Keyparts has"+keyParts.size()+" elements");
            for (int i = 0; i < keyParts.size(); i++) {
                //Log.d("nils","checking keypart "+keyParts.get(i));
                if (realColumnNameToDB.containsKey(keyParts.get(i))) {
                    Log.d("nils", "Key " + keyParts.get(i) + " already exists..skipping");
                } else if (staticColumn(keyParts.get(i))) {
                    Log.d("nils", "Key " + keyParts.get(i) + " is a static key. Sure this ok??");

                } else {
                    Log.d("nils", "Found new column key " + keyParts.get(i));
                    if (keyParts.get(i).isEmpty()) {
                        Log.d("nils", "found empty keypart! Skipping");
                    } else {
                        String colId = "L" + (realColumnNameToDB.size() + 1);
                        //Add key to memory
                        realColumnNameToDB.put(keyParts.get(i), colId);
                        dbColumnNameToReal.put(colId, keyParts.get(i));
                        //Persist new column identifier.
                        appPh.put(colId, keyParts.get(i));
                    }
                }

            }
        }
        Log.d("nils", "Keys added: ");
        Set<String> s = realColumnNameToDB.keySet();
        for (String e : s)
            Log.d("nils", "Key: " + e + "Value:" + realColumnNameToDB.get(e));

    }
/*
    public void fixYearNull() {
        String colYear = getDatabaseColumnName(YEAR);
        //add year to rows missing year
        if (!colYear.equals(getDatabaseColumnName(YEAR)))
            db().execSQL("update variabler set " + colYear + "= '" + Calendar.getInstance().get(Calendar.YEAR) + "' where " + colYear + " is null");

    }
*/



    private boolean staticColumn(String col) {
        for (String staticCol : VAR_COLS) {
            if (staticCol.equals(col))
                return true;
        }
        return false;
    }


    @Override
    public void onCreate(SQLiteDatabase _db) {

        // create variable table Lx columns are key parts.
        String CREATE_VARIABLE_TABLE = "CREATE TABLE IF NOT EXISTS variabler ( id INTEGER PRIMARY KEY ,L1 TEXT , L2 TEXT , L3 TEXT , L4 TEXT , L5 TEXT , L6 TEXT , L7 TEXT , L8 TEXT , L9 TEXT , L10 TEXT , var TEXT COLLATE NOCASE, value TEXT, lag TEXT, timestamp NUMBER, author TEXT ) ";

        //audit table to keep track of all insert,updates and deletes.
        String CREATE_AUDIT_TABLE = String.format("CREATE TABLE IF NOT EXISTS audit ( id INTEGER PRIMARY KEY ,%s TEXT, timestamp NUMBER, action TEXT, target TEXT, %s TEXT, changes TEXT ) ", LAG, AUTHOR);

        //synck table to keep track of incoming rows of data (sync entries[])
        String CREATE_SYNC_TABLE = "CREATE TABLE IF NOT EXISTS "+TABLE_SYNC+" ( id INTEGER PRIMARY KEY ,data BLOB )";

        //keeps track of sync timestamps. Changing team will reset the timestamp.
        String CREATE_TIMESTAMP_TABLE = "CREATE TABLE IF NOT EXISTS "+TABLE_TIMESTAMPS+" ( id INTEGER PRIMARY KEY ,LABEL TEXT, VALUE NUMBER, SYNCGROUP TEXT, SEQNO INTEGER )" ;


        _db.execSQL(CREATE_VARIABLE_TABLE);
        _db.execSQL(CREATE_AUDIT_TABLE);
        _db.execSQL(CREATE_SYNC_TABLE);
        _db.execSQL(CREATE_TIMESTAMP_TABLE);

        Log.d("NILS", "DB CREATED");
    }

    @Override
    public void onUpgrade(SQLiteDatabase _db, int oldVersion, int newVersion) {
        Log.d("NILS", "UPDATE CALLED");
        // Drop older books table if existed
        _db.execSQL("DROP TABLE IF EXISTS variabler");
        _db.execSQL("DROP TABLE IF EXISTS audit");
        _db.execSQL("DROP TABLE IF EXISTS sync");
        _db.execSQL("DROP TABLE IF EXISTS "+TABLE_TIMESTAMPS);
        // create fresh table
        this.onCreate(_db);
    }

		/*
        public void exportAllData() {
			Cursor c = db.query(TABLE_VARIABLES,null,
					null,null,null,null,null,null);
			if (c!=null) {

				//"timestamp","lag","author"
				Log.d("nils","Variables found in db:");
				String L[] = new String[realColumnNameToDB.size()];
				String var,value,timeStamp,lag,author;
				while (c.moveToNext()) {
					var = c.getString(c.getColumnIndex("var"));
					value = c.getString(c.getColumnIndex("value"));
					timeStamp = c.getString(c.getColumnIndex("timestamp"));
					lag = c.getString(c.getColumnIndex("lag"));
					author = c.getString(c.getColumnIndex("author"));
					for (int i=0;i<L.length;i++)
						L[i]=c.getString(c.getColumnIndex("L"+(i+1)));

				}
			}
		}
		 */

    //Export a specific context with a specific Exporter.
    public Report export(Map<String, String> context, final Exporter exporter, String exportFileName) {
        File[] externalStorageVolumes =
                ContextCompat.getExternalFilesDirs(GlobalState.getInstance().getContext(), null);
        File primaryExternalStorage = externalStorageVolumes[0];
        String exportFolder = primaryExternalStorage.getAbsolutePath() + "/export/";
        File f = new File(exportFolder);
        if (f.isDirectory()) {
            Log.i("glado","It is a directory");
        } else {
            Log.i("glado", "It is not a directory");
            if(!f.mkdirs())
                Log.e("glado","Failed to create export folder");

        }
        if (exporter == null)
            return new Report(ExportReport.EXPORTFORMAT_UNKNOWN);
        Log.d("nils", "Started export");
        Log.d("vortex", "filename: " + exportFileName + " context: " + context);
        String selection = "";

        if (exporter instanceof GeoJSONExporter) {
            Log.d("vortex", "geojsonexport");
            selection = (getDatabaseColumnName("uid") + " NOT NULL " + (context != null ? "AND " : ""));
        }

        List<String> selArgs = null;
        if (context != null) {
            Log.d("vortex", "context: " + context.toString());
            //selection = "";
            String col;
            //Build query
            Iterator<String> it = context.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                col = this.getDatabaseColumnName(key);
                if (col == null) {
                    Log.e("nils", "Could not find column mapping to columnHeader " + key);
                    return new Report(ExportReport.COLUMN_DOES_NOT_EXIST);
                }
                if (context.get(key).equals("*")) {
                    selection += col + " LIKE '%'";
                    if (it.hasNext())
                        selection += " AND ";
                } else {
                    selection += col + (it.hasNext() ? "=? AND " : "=?");
                    if (selArgs == null)
                        selArgs = new ArrayList<String>();
                    selArgs.add(context.get(key));
                }
            }
        }
        //Select.
        Log.d("vortex", "selection is " + selection);
        Log.d("vortex", "Args is " + selArgs);
        String[] selArgsA = null;
        if (selArgs != null)
            selArgsA = selArgs.toArray(new String[selArgs.size()]);
        Cursor c=null;
        try {
            c = db().query(TABLE_VARIABLES, null, selection,
                    selArgsA, null, null, null, null);
        } catch (SQLiteException e) {
            Log.d("dbhelper","sqlexception on query with "+selection+" args: "+print(selArgsA));
        }

        if (c != null) {
            Log.d("nils", "Variables found in db for context " + context);
            //Wrap the cursor in an object that understand how to pick it!
            Report r = exporter.writeVariables(new DBColumnPicker(c));
            if (r != null && r.noOfVars > 0) {
                final Report res;
                if (Tools.writeToFile(exportFolder + exportFileName + "." + exporter.getType(), r.getData())) {
                    Log.d("nils", "Exported file succesfully");
                    LoggerI logger = GlobalState.getInstance().getLogger();
                    logger.addCriticalText("Exported to folder: "+exportFolder);
                    c.close();
                    res = r;
                } else {
                    Log.e("nils", "Export of file failed");
                    c.close();
                    GlobalState.getInstance().getLogger().addRow("EXPORT FILENAME: [" + exportFolder + exportFileName + "." + exporter.getType() + "]");
                    res = new Report(ExportReport.FILE_WRITE_ERROR);
                }
                final Activity act = (Activity) exporter.getContext();
                if (act!=null)
                    act.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            if (res.getReport() == ExportReport.OK) {
                                exporter.getDialog().setCheckGenerate(true);
                            } else {
                                exporter.getDialog().setCheckGenerate(false);
                                exporter.getDialog().setGenerateStatus(res.getReport().name());
                            }
                        }
                    });

                //final String ret = GlobalState.getInstance().getBackupManager().backupExportDataWithProgress(exportFileName + "." + exporter.getType(), r.result,exporter.getDialog(),act);
                final String ret = GlobalState.getInstance().getBackupManager().backupExportData(exportFileName + "." + exporter.getType(), r.getData());
                if (act!=null)
                    act.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            exporter.getDialog().setCheckBackup(ret.equals("OK"));
                            exporter.getDialog().setBackupStatus(ret);
                        }
                    });

                return res;
            }
        }

        if (exporter.getContext() != null)
            ((Activity) exporter.getContext()).runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    exporter.getDialog().setCheckGenerate(false);
                    exporter.getDialog().setGenerateStatus("Failed export. No data?");
                }
            });

        return new Report(ExportReport.NO_DATA);
    }

    /*
        private void printAuditVariables() {
            Cursor c = db.query(TABLE_AUDIT, null,
                    null, null, null, null, null, null);
            if (c != null) {
                Log.d("nils", "Variables found in db:");
                while (c.moveToNext()) {
                    Log.d("nils", "ACTION: " + c.getString(c.getColumnIndex("action")) +
                            "CHANGES: " + c.getString(c.getColumnIndex("changes")) +
                            "TARGET: " + c.getString(c.getColumnIndex("target")) +
                            "TIMESTAMP: " + c.getString(c.getColumnIndex("timestamp")));

                }
                c.close();
            } else
                Log.e("nils", "NO AUDIT VARIABLES FOUND");

        }


        public void printAllVariables() {
            String fileName = Constants.VORTEX_ROOT_DIR +
                    globalPh.get(PersistenceHelper.BUNDLE_NAME) + "/dbdump.txt";
            File file = new File(fileName);
            Log.d("vortex", "file created at: " + fileName);
            PrintWriter writer = null;
            Cursor c = null;
            try {
                boolean cre = file.createNewFile();
                if (!cre)
                    Log.d("vortex", "file was already there");
                writer = new PrintWriter(file, "UTF-8");
                String header = "";
                Log.d("nils", "Variables found in db:");
                for (int i = 1; i <= DbHelper.NO_OF_KEYS; i++)
                    header += dbColumnNameToReal.get("L" + i) + "|";
                header += "var|value";
                Log.d("vortex", header);
                writer.println(header);
                int offSet = 0;
                boolean notDone = true;

                //Ask for 500 rows per query.
                String row;
                //while (notDone) {

                c = db.query(TABLE_VARIABLES, null,
                        null, null, null, null, null, null);//offSet+",100"
                if (c != null) {

                    int i = 0;
                    while (c.moveToNext()) {
                        row = (
                                c.getString(c.getColumnIndex("L1")) + "|" +
                                        c.getString(c.getColumnIndex("L2")) + "|" +
                                        c.getString(c.getColumnIndex("L3")) + "|" +
                                        c.getString(c.getColumnIndex("L4")) + "|" +
                                        c.getString(c.getColumnIndex("L5")) + "|" +
                                        c.getString(c.getColumnIndex("L6")) + "|" +
                                        c.getString(c.getColumnIndex("L7")) + "|" +
                                        c.getString(c.getColumnIndex("L8")) + "|" +
                                        c.getString(c.getColumnIndex("L9")) + "|" +
                                        c.getString(c.getColumnIndex("L10")) + "|" +
                                        c.getString(c.getColumnIndex("var")) + "|" +
                                        c.getString(c.getColumnIndex("value")));
                        writer.println(row);
                        i++;
                    }
                    c.close();
                }
                    /*
                            Log.d("vortex","i is "+i);
                            if (i<100)
                                notDone = false;
                        } else {
                            Log.d("vortex","C was null!");
                            notDone = false;
                        }
                        offSet+=100;
                    }

            } catch (Exception e) {
                e.printStackTrace();
                if (c != null)
                    c.close();
            }


            if (writer != null)
                writer.close();
        }

        enum ActionType {
            insert,
            delete
        }
     */
    public void deleteVariable(String name, Selection s, boolean isSynchronized) {
        // 1. get reference to writable DB
        //SQLiteDatabase db = this.getWritableDatabase();


        int aff =
                db().delete(TABLE_VARIABLES, //table name
                        s.selection,  // selections
                        s.selectionArgs); //selections args

        //if(aff==0)
        //	Log.e("nils","Couldn't delete "+name+" from database. Not found. Sel: "+s.selection+" Args: "+print(s.selectionArgs));
        //else
        //	Log.d("nils","DELETED: "+ name);

        if (isSynchronized)
            insertDeleteAuditEntry(s, name);
    }


    private Map<String, String> createAuditEntry(Variable var, String newValue,
                                                 long timeStamp) {
        Map<String, String> valueSet = new HashMap<String, String>();
        //if (!var.isKeyVariable())
        valueSet.put(var.getValueColumnName(), newValue);
        valueSet.put("timestamp", timeStamp+"");
        valueSet.put("author", globalPh.get(PersistenceHelper.USER_ID_KEY));
        return valueSet;
    }

    private void insertDeleteAuditEntry(Selection s, String varName) {
        //package the value array.
        String dd = "";

        if (s.selectionArgs != null) {
            String realColNames[] = new String[s.selectionArgs.length];
            //get the true column names.
            String selection = s.selection;
            if (selection == null) {
                Log.e("vortex", "Selection was null...no variable name!");
                return;
            }
            String selA = "";
            for (String ss : s.selectionArgs)
                selA += ss + ",";
            //Log.d("vortex", "SelectionArgs: " + selA);
            String zel[] = selection.split("=");
            for (int ii = 0; ii < s.selectionArgs.length; ii++) {
                String z = zel[ii];
                //Log.d("vortex", "z is now " + z);
                int iz = z.indexOf("L");
                if (iz == -1) {
                    if (!z.isEmpty()) {
                        int li = z.lastIndexOf(" ");
                        String last = z.substring(li + 1, z.length());
                        //    if (li != -1)
                        //        Log.e("vortex", "var is " + last);
                        realColNames[ii] = last;

                    }
                    //Log.d("vortex", "Found column: " + z);
                    //Log.d("vortex", "real name: " + z);

                } else {
                    String col = z.substring(iz, z.length());
//                    Log.d("vortex", "Found column: " + col);
//                    Log.d("vortex", "real name: " + dbColumnNameToReal.get(col));
                    realColNames[ii] = getRealColumnNameFromDatabaseName(col);

                }

            }
            for (int i = 0; i < s.selectionArgs.length; i++)
                dd += realColNames[i] + "=" + s.selectionArgs[i] + "|";
            dd = dd.substring(0, dd.length() - 1);
        } else
            dd = null;
        //store
        if (dd != null) {
            storeAuditEntry("D", dd, varName,System.currentTimeMillis(),globalPh.get(PersistenceHelper.USER_ID_KEY));
        }
        Log.d("vortex", "INSERT Delete audit entry. Args:  " + dd);
    }

    public void insertEraseAuditEntry(String keyPairs, String pattern) {
        storeAuditEntry("M", keyPairs, pattern, System.currentTimeMillis(),globalPh.get(PersistenceHelper.USER_ID_KEY));
        Log.d("vortex", "inserted Erase Many with: " + keyPairs + " and pattern " + pattern);

    }


    private void insertAuditEntry(Variable v, Map<String, String> valueSet, String author, String action,long timestamp) {
        String changes = "";
        //First the keys.
        Log.d("vortex","Inserting Audit entry for "+v.getId());
        Map<String, String> keyChain = v.getKeyChain();
        Iterator<Entry<String, String>> it;
        if (keyChain != null) {
            it = keyChain.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, String> entry = it.next();
                String value = entry.getValue();
                //KOKKO
                String column = entry.getKey();
                //Log.d("insa","in insertaudit column: "+column+" entry.getkey "+entry.getKey());
                //Log.d("vortex","FIZ column: "+column+" maps to: "+entry.getKey());
                //changes+=entry.getKey()+"="+value+"|";
                changes += column + "=" + value + "|";
            }
        }
        changes += "var=" + v.getId();
        changes += SYNC_SPLIT_SYMBOL;
        //Now the values
        it = valueSet.entrySet().iterator();
        while (it.hasNext()) {
            Entry<String, String> entry = it.next();
            String value = entry.getValue();
            String column =entry.getKey();
            changes += (column + "=" + value);
            if (it.hasNext())
                changes += "§";
            else
                break;

        }
        //Log.d("nils","Variable name: "+v.getId());
        //Log.d("nils","Audit entry: "+changes);
        storeAuditEntry(action, changes, v.getId(),timestamp,author);
    }


    private void storeAuditEntry(String action,String changes, String varName,long timestamp, String author) {

        if (action == null || changes == null ) {
            Log.e("vortex", "STOREADUIT ERROR: Action: " + action + " changes: " + changes + " varName: " + varName);
            return;
        }
        ContentValues values = new ContentValues();
        values.put("action", action);
        values.put("lag",globalPh.get(PersistenceHelper.LAG_ID_KEY));
        values.put("changes", changes);
        values.put("target", varName);
        values.put("timestamp", timestamp);
        values.put("author",author);
        //need to save timestamp + value
        db().insert(TABLE_AUDIT, null, values);
    }

    private Cursor getExistingVariableCursor(String name, Selection s) {
        //Log.d("nils","In getId with name "+name+" and selection "+s.selection+" and selectionargs "+print(s.selectionArgs));
        Cursor c = db().query(TABLE_VARIABLES, new String[]{"id", "timestamp", "value", "var", "author"},
                s.selection, s.selectionArgs, null, null, null, null);
        return c;
    }
    /*
    public StoredVariableData getVariable(String name, Selection s) {

        Cursor c = db.query(TABLE_VARIABLES, new String[]{"value", "timestamp", "lag", "author"},
                s.selection, s.selectionArgs, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            StoredVariableData sv = new StoredVariableData(name, c.getString(0), c.getString(1), c.getString(2), c.getString(3));

            //Log.d("nils","Found value and ts in db for "+name+" :"+sv.value+" "+sv.timeStamp);
            c.close();
            return sv;
        }
        Log.e("nils", "Variable " + name + " not found in getVariable DbHelper");
        if (c!=null)
            c.close();
        return null;
    }
*/

    public class StoredVariableData {
        StoredVariableData(String name, String value, String timestamp,
                           String lag, String author) {
            this.timeStamp = timestamp;
            this.value = value;
            this.lagId = lag;
            this.creator = author;
            this.name = name;
        }

        public final String name;
        public final String timeStamp;
        public final String value;
        public final String lagId;
        public final String creator;
    }

    //public final static int MAX_RESULT_ROWS = 500;

    public List<String[]> getUniqueValues(String[] columns, Selection s) {

        //Log.d("nils","In getvalues with columns "+print(columns)+", selection "+s.selection+" and selectionargs "+print(s.selectionArgs));
        //Substitute if possible.
        String[] substCols = new String[columns.length];

        for (int i = 0; i < columns.length; i++)
            substCols[i] = getDatabaseColumnName(columns[i]);
        //Get cached selectionArgs if exist.
        //this.printAllVariables();
        Cursor c = db().query(true,TABLE_VARIABLES, substCols,
                s.selection, s.selectionArgs, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            List<String[]> ret = new ArrayList<String[]>();
            String[] row;
            do {
                row = new String[c.getColumnCount()];
                boolean nullRow = true;
                for (int i = 0; i < c.getColumnCount(); i++) {
                    if (c.getString(i) != null) {
                        if (c.getString(i).equalsIgnoreCase("null"))
                            Log.e("nils", "StringNull!");
                        row[i] = c.getString(i);
                        nullRow = false;
                    }

                }
                if (!nullRow) {
                    //Log.d("nils","GetValues found row. First elem: "+c.getString(0));
                    //only add row if one of the values is not null.
                    ret.add(row);
                } //else
                //Log.e("vortex","Null row! "+print(row));
            } while (c.moveToNext());
            if (ret.size()==0)
                Log.d("nils","Found no values in GetValues");
            c.close();
            return ret;
        }
       /* String su = "[";
        for (String ss : columns)
            su += ss + ",";
        su += "]";
        Log.d("nils","Did NOT find value in db for columns "+su);
        */
        if (c!=null)
            c.close();
        return null;
    }

    public List<String> getValues(Selection s) {
        Cursor c = db().query(TABLE_VARIABLES, new String[]{"value"},
                s.selection, s.selectionArgs, null, null, null, null);
        List<String> ret = null;
        if (c != null && c.moveToFirst()) {
            ret = new ArrayList<String>();
            do {
                ret.add(c.getString(0));
            } while (c.moveToNext());

        }
        if (c != null)
            c.close();
        return ret;
    }


    public String getValue(String name, Selection s, String[] valueCol) {
        //this.printAllVariables();
        //Log.d("nils","In getvalue with name "+name+" and selection "+s.selection+" and selectionargs "+print(s.selectionArgs));
        Cursor c = null;
        if (checkForNulls(s.selectionArgs)) {
            c = db().query(TABLE_VARIABLES, valueCol,
                    s.selection, s.selectionArgs, null, null, "timestamp DESC", "1");
            if (c != null && c.moveToFirst()) {
                //Log.d("nils","Cursor count "+c.getCount()+" columns "+c.getColumnCount());
                String value = c.getString(0);
                //Log.d("vortex", "GETVALUE [" + name + " :" + value + "] Value = null? " + (value == null));
                c.close();

                return value;
            }
        }

        //Log.d("nils","Did NOT find value in db for "+name+". Key arguments:");
			/*
			String sel = s.selection;
			int cc=0;
			String k[]= new String[100];
			for (int i=0;i<sel.length();i++){
				if (sel.charAt(i)=='L') {
					i++;
					k[cc++] = "L"+sel.charAt(i);
				}
			}
			Set<Entry<String, String>> x = dbColumnNameToReal.entrySet();
			*/
        //		for(Entry e:x)
        //			Log.d("nils","kolkey KEY:"+e.getKey()+" "+e.getValue());
        //		for (int i=0;i<cc;i++) {
        //
        //
        //			Log.d("nils"," Key: ("+k[i]+") "+dbColumnNameToReal.get(k[i])+" = "+s.selectionArgs[i]);
        //		}

        if (c != null)
            c.close();
        return null;

    }


    private boolean checkForNulls(String[] selectionArgs) {
        for (String s : selectionArgs)
            if (s == null)
                return false;
        return true;
    }

    private int getId(Selection s) {
        //Log.d("nils","In getId with name "+name+" and selection "+s.selection+" and selectionargs "+print(s.selectionArgs));
        Cursor c = db().query(TABLE_VARIABLES, new String[]{"id"},
                s.selection, s.selectionArgs, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            //Log.d("nils","Cursor count "+c.getCount()+" columns "+c.getColumnCount());
            int value = c.getInt(0);
            //Log.d("nils","Found id in db for "+name+" :"+value);
            c.close();
            return value;
        }
        //Log.d("nils","Did NOT find value in db for "+name);
        if (c != null)
            c.close();
        return -1;
    }


    private String print(String[] selectionArgs) {
        if (selectionArgs == null)
            return "NULL";
        String ret = "";
        for (int i = 0; i < selectionArgs.length; i++)
            ret += ("[" + i + "]: " + selectionArgs[i] + " ");
        return ret;
    }


    //Insert or Update existing value. Synchronize tells if var should be synched over blutooth.
    //This is done in own thread.
    private final ContentValues insertContentValues = new ContentValues();

    public void insertVariable(Variable var, String newValue, boolean syncMePlease) {

        insertContentValues.clear();
        boolean isReplace = false;
        long timeStamp = System.currentTimeMillis();
        //for logging
        //Log.d("nils", "INSERT VALUE ["+var.getId()+": "+var.getValue()+"] Local: "+isLocal+ "NEW Value: "+newValue);



        //Key need to be updated?
        if (var.isKeyVariable()) {
            //Log.d("nils","updating key to "+newValue);
            //String oldValue = var.getKeyChain().put(var.getValueColumnName(), newValue);
            var.getSelection().selectionArgs = createSelectionArgs(var.getKeyChain(), var.getId());
            //Check if the new chain leads to existing variable.
            int id = getId(var.getSelection());
            //Found match. Replace.
            if (id != -1) {
                Log.d("nils", "variable exists");
                insertContentValues.put("id", id);
                isReplace = true;
            }
        }
        // 1. create ContentValues to add key "column"/value
        String author = globalPh.get(PersistenceHelper.USER_ID_KEY);
        createValueMap(var, newValue, insertContentValues, timeStamp,author);
        // 3. insert
        long rId;
        if (isReplace) {
            rId = db().replace(TABLE_VARIABLES, // table
                    null, //nullColumnHack
                    insertContentValues
            );

        } else {
            rId = db().insert(TABLE_VARIABLES, // table
                    null, //nullColumnHack
                    insertContentValues
            );
        }

        if (rId == -1) {
            Log.e("nils", "Could not insert variable " + var.getId());
        } else {
            //Log.d("zorg","Inserted "+var.getId()+" into database. Values: "+values.toString());//==null?"null":(var.getKeyChain().values()==null?"null":var.getKeyChain().values().toString()));

            //If this variable is not local, store the action for synchronization.
            if (syncMePlease) {
                insertAuditEntry(var, createAuditEntry(var, newValue, timeStamp),author, "I",timeStamp);

            }
            //else
            //	Log.d("nils","Variable "+var.getId()+" not inserted in Audit: local");
        }

        //delete lateron
        //Delete any existing value.
        deleteOldVariable(var.getId(),var.getSelection(), rId);

    }


    private void deleteOldVariable(final String name,final Selection s, final long newId) {
        if (s == null) {
            Log.e("vortex", "selection null in deleteOld!!");
            return;
        }
        String[] extendedSelArgs = new String[s.selectionArgs.length + 1];
        String extendedSelection = s.selection + " AND id <> ?";
        System.arraycopy(s.selectionArgs, 0, extendedSelArgs, 0, s.selectionArgs.length);
        String newIdS = null;
        try {
            newIdS = Long.toString(newId);
        } catch (NumberFormatException e) {
            Log.e("vortex", "not an id number in deleteold");
            return;
        }
        extendedSelArgs[extendedSelArgs.length - 1] = newIdS;
//                String stmt = "var = '"+name+"' AND id <> '"+newId+"' AND "+
//                        getDatabaseColumnName("år")+" <> '"+Constants.HISTORICAL_TOKEN_IN_DATABASE+"'";
        //Log.d("vova","selection: "+s.selection);
        //Log.d("vova","selectionArgs: "+print(s.selectionArgs));
        //Log.d("vova","EXT selection: "+extendedSelection);
        //Log.d("vova","EXT selectionArgs: "+print(extendedSelArgs));
        int aff =
                db().delete(TABLE_VARIABLES, //table name
                        extendedSelection,  // selections
                        extendedSelArgs); //selections args
        //if (aff == 0)
        //    Log.e("vortex", "could not delete old value for " + name);
    }
    /*
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {


                if (s == null) {
                    Log.e("vortex","selection null in deleteOld!!");
                    return;
                }
                String[] extendedSelArgs = new String[s.selectionArgs.length+1];
                String extendedSelection = s.selection + " AND id <> ?";
                System.arraycopy(s.selectionArgs, 0, extendedSelArgs, 0, s.selectionArgs.length);
                String newIdS=null;
                try {
                    newIdS = Long.toString(newId);
                } catch (NumberFormatException e) {
                    Log.e("vortex","not an id number in deleteold");
                    return;
                }
                extendedSelArgs[extendedSelArgs.length-1]=newIdS;
//                String stmt = "var = '"+name+"' AND id <> '"+newId+"' AND "+
//                        getDatabaseColumnName("år")+" <> '"+Constants.HISTORICAL_TOKEN_IN_DATABASE+"'";
                //Log.d("vova","selection: "+s.selection);
                //Log.d("vova","selectionArgs: "+print(s.selectionArgs));
                //Log.d("vova","EXT selection: "+extendedSelection);
                //Log.d("vova","EXT selectionArgs: "+print(extendedSelArgs));
                int aff =
                        db.delete(TABLE_VARIABLES, //table name
                                extendedSelection,  // selections
                                extendedSelArgs); //selections args
                if (aff == 0)
                    Log.e("vortex", "could not delete old value for " + name);

            }
        }, 0);

    }
*/
    private void createValueMap(Variable var, String newValue, ContentValues values, long timeStamp,String author) {
        //Add column,value mapping.
        //Log.d("vortex","in createvaluemap");
        Map<String, String> keyChain = var.getKeyChain();
        //If no key column mappings, skip. Variable is global with Id as key.
        if (keyChain != null) {
            //Log.d("nils","keychain for "+var.getLabel()+" has "+keyChain.size()+" elements");
            for (String key : keyChain.keySet()) {
                String value = keyChain.get(key);
                String column = getDatabaseColumnName(key);
                values.put(column, value);
                //Log.d("nils","Adding column "+column+"(key):"+key+" with value "+value);
            }
        } else
            Log.d("nils", "Inserting global variable " + var.getId() + " value: " + newValue);
        values.put("var", var.getId());
        //if (!var.isKeyVariable()) {
        //Log.d("nils","Inserting new value into column "+var.getValueColumnName()+" ("+getDatabaseColumnName(var.getValueColumnName())+")");
        values.put(getDatabaseColumnName(var.getValueColumnName()), newValue);
        //}
        values.put("lag", globalPh.get(PersistenceHelper.LAG_ID_KEY));
        values.put("timestamp", timeStamp);
        values.put("author", author);

    }


    //Adds a value for the variable but does not delete any existing value.
    //This in effect creates an array of values for different timestamps.
    public void insertVariableSnap(ArrayVariable var, String newValue,
                                   boolean syncMePlease) {
        //Log.d("vortex","I am in snap insert for variable "+var.getId()+" that is synced: "+syncMePlease);
        long timeStamp = System.currentTimeMillis();
        ContentValues values = new ContentValues();
        String author = globalPh.get(PersistenceHelper.USER_ID_KEY);
        createValueMap(var, newValue, values, timeStamp,author);

        db().insert(TABLE_VARIABLES, // table
                null, //nullColumnHack
                values
        );
        if (syncMePlease) {
            insertAuditEntry(var, createAuditEntry(var, newValue, timeStamp),author,"A",timeStamp);

        }
    }




    public VariableRowEntry[] exportEverything(UIProvider ui) {
        VariableRowEntry[] vRows = null;
        List<String> values;
        //Ask for everything.
        Cursor c = db().query(TABLE_VARIABLES, null, null,
                null, null, null, null, null);
        if (c != null && c.getCount() > 0 && c.moveToFirst()) {
            vRows = new VariableRowEntry[c.getCount() + 1];
            int cn = 0;
            do {
                values = new ArrayList<String>(NO_OF_KEYS);
                for (int i = 1; i <= NO_OF_KEYS; i++)
                    values.add(c.getString(c.getColumnIndex("L" + i)));

                vRows[cn] = new VariableRowEntry(c.getString(c.getColumnIndex(VARID)),
                        c.getString(c.getColumnIndex(VALUE)),
                        c.getString(c.getColumnIndex(LAG)),
                        c.getString(c.getColumnIndex(TIMESTAMP)),
                        c.getString(c.getColumnIndex(AUTHOR)),
                        values);

                cn++;
                if (ui != null)
                    ui.setInfo(cn + "/" + c.getCount());
            } while (c.moveToNext());
        }
        if (c!=null)
            c.close();
        return vRows;
    }

    public SyncEntry[] getChanges(UIProvider ui) {
        long maxStamp;
        SyncEntry[] sa = null;
        String timestamp = globalPh.get(PersistenceHelper.TIME_OF_LAST_SYNC);
        String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
        if (timestamp.equals(PersistenceHelper.UNDEFINED))
            timestamp = "0";

        //Log.d("nils","Time of last sync is "+timestamp+" in getChanges (dbHelper)");
        Cursor c = db().query(TABLE_AUDIT, null,
                "timestamp > ? AND "+DbHelper.LAG+" = ?", new String[]{timestamp, team}, null, null, "timestamp asc", null);
        if (c != null && c.getCount() > 0 && c.moveToFirst()) {
            int cn = 1;
            sa = new SyncEntry[c.getCount() + 1];
            long entryStamp;
            String action, changes, target,author;
            maxStamp = 0;
            do {
                action = c.getString(c.getColumnIndex("action"));
                changes = c.getString(c.getColumnIndex("changes"));
                entryStamp = c.getLong(c.getColumnIndex("timestamp"));
                target = c.getString(c.getColumnIndex("target"));
                author = c.getString(c.getColumnIndex("author"));


                if (entryStamp > maxStamp)
                    maxStamp = entryStamp;
                sa[cn] = new SyncEntry(SyncEntry.action(action), changes, entryStamp, target,author);
                //Log.d("nils","Added sync entry : "+action+" changes: "+changes+" index: "+cn);
                if (ui != null)
                    ui.setInfo(cn + "/" + c.getCount());
                cn++;
            } while (c.moveToNext());
            SyncEntryHeader seh = new SyncEntryHeader(maxStamp);
            sa[0] = seh;
        } else
            Log.d("nils", "no sync needed...no new entries");
        //mySyncEntries = ret;
        if (c != null)
            c.close();
        return sa;
    }


//Map <Set<String>,String>cachedSelArgs = new HashMap<Set<String>,String>();

    public static class Selection {
        public String[] selectionArgs = null;
        public String selection = null;
    }

    public Selection createSelection(Map<String, String> keySet, String name) {

        Selection ret = new Selection();
        //Create selection String.

        //If keyset is null, the variable is potentially global with only name as a key.
        String selection = "";
        if (keySet != null) {
            //Does not exist...need to create.
            selection = "";
            //1.find the matching column.
            for (String key : keySet.keySet()) {
                key = getDatabaseColumnName(key);

                    selection += key + "= ? and ";

            }
        }
        selection += "var= ?";
        ret.selection = selection;
        //Log.d("nils","created new selection: "+selection);
        ret.selectionArgs = createSelectionArgs(keySet, name);
        //Log.d("nils","CREATE SELECTION RETURNS: "+ret.selection+" "+print(ret.selectionArgs));
        return ret;
    }


    private String[] createSelectionArgs(Map<String, String> keySet, String name) {
        String[] selectionArgs;
        if (keySet == null) {
            selectionArgs = new String[]{name};
        } else {
            selectionArgs = new String[keySet.keySet().size() + 1];
            int c = 0;
            for (String key : keySet.keySet()) {
                selectionArgs[c++] = keySet.get(key);
                //Log.d("nils","Adding selArg "+keySet.get(key)+" for key "+key);
            }
            //add name part
            selectionArgs[keySet.keySet().size()] = name;
        }
        return selectionArgs;
    }


    public Selection createCoulmnSelection(Map<String, String> keySet) {
        Selection ret = new Selection();
        //Create selection String.

        //If keyset is null, the variable is potentially global with only name as a key.
        String selection;
        if (keySet != null) {
            //selection = cachedSelArgs.get(keySet.keySet());
            //if (selection!=null) {
            //	Log.d("nils","found cached selArgs: "+selection);
            //} else {
            //Log.d("nils","selection null...creating");
            //Does not exist...need to create.
            String col;
            selection = "";
            //1.find the matching column.
            List<String> keys = new ArrayList<String>();
            keys.addAll(keySet.keySet());
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);

                col = getDatabaseColumnName(key);
                selection += col + "= ?" + ((i < (keys.size() - 1)) ? " and " : "");


                //		}

                //cachedSelArgs.put(keySet.keySet(), selection);

            }

            ret.selection = selection;
            String[] selectionArgs = new String[keySet.keySet().size()];
            int c = 0;
            for (String key : keySet.keySet())
                selectionArgs[c++] = keySet.get(key);
            ret.selectionArgs = selectionArgs;
        }
        return ret;
    }


    //Try to map ColId.
    //If no mapping exist, return colId.

    public String getDatabaseColumnName(String colId) {
        if (colId == null || colId.length() == 0)
            return null;
        String ret = realColumnNameToDB.get(colId);
        if (ret == null)
            return colId;
        else
            return ret;
    }

    private boolean hasDatabaseColumnName(String c) {
        return realColumnNameToDB.get(c)!=null;
    }


    private String getRealColumnNameFromDatabaseName(String databaseColumnName) {
        if (databaseColumnName == null || databaseColumnName.length() == 0)
            return null;
        String ret = dbColumnNameToReal.get(databaseColumnName);
        if (ret == null)
            return databaseColumnName;
        else
            return ret;
    }


    private int synC = 0;




    public SyncReport insertSyncEntries(SyncReport changes, SyncEntry[] ses, LoggerI o) {

        if (ses == null || ses.length==0) {
            Log.d("sync", "syncentry contained no data");
            return null;
        }
        final VariableCache variableCache = GlobalState.getInstance().getVariableCache();
        //If cache needs to be emptied.
        boolean resetCache = false;
        final String uidCol = getDatabaseColumnName("uid");
        final String subCol = getDatabaseColumnName("sub");
        //String arCol = getDatabaseColumnName("år");
        //SyncStatus syncStatus=new SyncStatus();
        TimeStampedMap tsMap = changes.getTimeStampedMap();
        ContentValues cv;
        //Map<String, String> keySet = new HashMap<String, String>();
        // Map<String, String> keyHash = new HashMap<String, String>();


        //keep track of most current location update per user. For each user, keep a map of most current gps_x,gps_y,gps_accuracy etc
        Map<String,Map<String,SyncEntry>> mostCurrentSyncMessage = new HashMap<>();

        String variableName = null,uid=null, sub=null, syncTeam=null;
        int synC = 1;

        final String team = GlobalState.getInstance().getGlobalPreferences().get(PersistenceHelper.LAG_ID_KEY);

        beginTransaction();

        for (SyncEntry s : ses) {

            uid=null;
            sub=null;
            synC++;
            //Only insert location updates that are newest.
            if (s.isInsertArray()) {
                //Log.d("babbs","array");
                String author=s.getAuthor();
                String variable = s.getTarget();
                //get author of sync message.
                if (author!=null) {
                    Map<String, SyncEntry> variables = mostCurrentSyncMessage.get(author);
                    if (variables == null) {
                        variables = new HashMap<>();
                        mostCurrentSyncMessage.put(author,variables);
                    }
                    //get existing syncentry
                    SyncEntry se = variables.get(variable);
                    //if older, replace.
                    if (se==null || se.getTimeStamp()<s.getTimeStamp()) {
                        variables.put(variable, s);
                        //Log.d("babbs"," Replacing variable "+variable);
                    }
                    //else
                    //    Log.d("babbs"," Discarding variable "+variable);
                } else
                    Log.e("babbs","author null in insertarray");
            }
            else if (s.isInsert() ) {

                cv = createContentValues(s.getKeys(),s.getValues(),team);
                if (cv == null) {
                    Log.e("maggan", "Synkmessage with " + s.getTarget() + " is invalid. Skipping. keys: " + s.getKeys() + " values: " + s.getValues());
                    changes.faults++;
                    changes.faultInValues++;
                    continue;
                }

                //Insert also in cache if not array.
                //
                uid = cv.getAsString(uidCol);  //unique key for object. uid.
                sub = cv.getAsString(subCol); //smaprovyteid
                variableName = cv.getAsString(VARID);

                if (uid != null ) {
                    //Log.d("brakko", "INSERT U: " + uid+ "Target: "+ s.getTarget() + " CH: " + s.getChange()+" TS:"+s.getTimeStamp()+" A:"+s.getAuthor());
                    //Log.d("vortex","added to tsmap: "+uid);
                    tsMap.add(tsMap.getKey(uid,sub),variableName, cv);
                    if (!variableCache.turboRemoveOrInvalidate(uid, sub, variableName, true))
                        resetCache = true;
                } else {
                    //Log.e("bascar", "Inserting RAW" + s.getChange());
                    db().insert(TABLE_VARIABLES, // table
                            null, //nullColumnHack
                            cv
                    );
                    variableCache.invalidateOnName(variableName);
                }
                changes.inserts++;



            }

            else if (s.isDelete()) {

                //Log.d("brakko", "DELETE U: " + uid+ "Target: "+ s.getTarget() + " CH: " + s.getChange()+" TS:"+s.getTimeStamp()+" A:"+s.getAuthor());
                Map<String,String> sKeys = s.getKeys();

                if (sKeys == null) {
                    Log.e("vortex", "no keys in Delete Syncentry");
                    changes.faults++;

                    continue;
                }
                sub = sKeys.get("sub");
                uid = sKeys.get("uid");
                variableName = sKeys.get(VARID);

                Unikey ukey = Unikey.FindKeyFromParts(uid,sub,tsMap.getKeySet());
                tsMap.delete(ukey, variableName);
                    try {
                        //Log.d("bascar","not found in sync cache buffer (TS): "+variableName);
                        int aff = delete(sKeys, s.getTimeStamp(), team);
                        if (aff == 0) {
                            changes.refused++;
                            changes.failedDeletes++;
                        } else {
                            changes.deletes++;
                            if (!variableCache.turboRemoveOrInvalidate(uid, sub,variableName, false))
                                resetCache = true;
                        }
                    } catch (SQLException e) {
                        Log.e("vortex", "Delete failed due to exception in statement");
                        changes.refused++;
                        changes.failedDeletes++;
                    }

            }


            //Erase set of values. Targets incoming sync entries only.
            else if (s.isDeleteMany()) {
                Log.d("bascar","Indeletemany. ");
                Map keyPairs = s.getKeys();
                String pattern = s.getTarget();
                if (keyPairs != null) {
                    //pattern applies to variables.
                    int affectedRows = tsMap.delete(keyPairs,pattern);
                    //
                    changes.deletes += affectedRows;
                    Log.d("bascar","Affected rows in sync cache: "+affectedRows);
                    //cache entries deleted in erase.
                    affectedRows = this.erase(s.getChange(), pattern);
                    Log.d("bascar","Affected rows in database:" +affectedRows);

                } else {
                    o.addRow("");
                    o.addRedText("DB_ERASE Failed. Message corrupt");
                    changes.faults++;
                }
            } else {
                Log.d("vortex","Got here... "+s.getTarget()+" "+s.getAction());
            }

        }

        //insert max location update if any.

        if (!mostCurrentSyncMessage.isEmpty()) {
            for (String name:mostCurrentSyncMessage.keySet()) {
                Map<String, SyncEntry> map = mostCurrentSyncMessage.get(name);
                if (map!=null) {
                    for (String variable:map.keySet()) {
                        SyncEntry se = map.get(variable);
                        //Log.d("babbs","inserting "+variable+" for "+name);
                        cv = createContentValues(se.getKeys(), se.getValues(), team);
                        db().insert(TABLE_VARIABLES, // table
                                null, //nullColumnHack
                                cv
                        );
                        //refresh cache
                        //uid = cv.getAsString(uidCol);  //unique key for object. uid.
                        //if (!variableCache.turboRemoveOrInvalidate(uid,null,variable,true))
                        //    resetCache = true;
                    }
                }


            }
        }

        endTransactionSuccess();

        if (resetCache)
         variableCache.reset();

        return changes;
    }

    private ContentValues createContentValues(Map<String,String> sKeys, Map<String,String> sValues, String team) {
        if (sKeys == null || sValues == null || sKeys.get("var")==null) {
            return null;
        }

        ContentValues cv = new ContentValues();

        for (String key : sKeys.keySet())
            cv.put(getDatabaseColumnName(key), sKeys.get(key));

        for (String key : sValues.keySet()) {
            cv.put(getDatabaseColumnName(key), sValues.get(key));
        }
        //Team must be same as currently configured
        cv.put(LAG,team);

        return cv;
    }

    private final StringBuilder whereClause = new StringBuilder();

    private int delete(Map<String,String> keys, long timeStamp, String team) throws SQLException {
        Log.d("plekk","Delete with "+keys.toString()+" ts "+timeStamp+" team "+team);
        //contains the delete key,value pairs found in the delete entry.
        if (keys ==null)
            return 0;
        int n=0;
        whereClause.setLength(0);
        //Create arguments. Add space for team and timestamp.
        String [] whereArgs = new String[keys.keySet().size()+2];
        for (String key : keys.keySet()) {
            //Put variable name last.
            whereClause.append(getDatabaseColumnName(key) + "= ? AND ");
            whereArgs[n++]=keys.get(key);
        }
        whereArgs[n++]= team;
        whereArgs[n] = timeStamp+"";
        whereClause.append(LAG+" = ? AND "+TIMESTAMP+" <= ?");

        Log.d("plekk","Calling delete with Selection: "+whereClause+" args: "+print(whereArgs));
        //Calling delete with Selection: L4= ? AND L2= ? AND L1= ? AND L3= ? AND timestamp <= ? AND var = ? args: [0]: 2B1AFEF6-6C71-45DC-BB26-AF0B362E9073 [1]: 999994 [2]: 2016 [3]: Angsobete [4]: 1474478318 [5]: null [6]: STATUS:status_angsochbete

        return
                db().delete(TABLE_VARIABLES, //table name
                        whereClause.toString(),  // selections
                        whereArgs); //selections args

    }



    private boolean isStatusVariable(String variableName) {
        return variableName.startsWith("STATUS:status_ruta");
    }

    public SyncReport synchronise(SyncEntry[] ses, UIProvider ui, LoggerI o, SyncStatusListener syncListener) {
        if (ses == null) {
            Log.d("sync", "ses är tom! i synchronize");
            return null;
        }
        Set<String> touchedVariables = new HashSet<String>();
        SyncReport changes = new SyncReport();
        GlobalState gs = GlobalState.getInstance();
        VariableCache vc = gs.getVariableCache();
        Set<String> conflictFlows = new HashSet<String>();

        int size = ses.length - 1;

        //Log.d("sync","LOCK!");
        db().beginTransaction();
        String name = null;
        boolean resetCache=false;

        synC = 0;
        if (ses.length == 0) {
            Log.e("plaz", "either syncarray is short or null. no data to sync.");
            db().endTransaction();
            return null;
        }
        //Log.d("vortex", "In Synchronize with " + ses.length + " arguments.");
        ContentValues cv = new ContentValues();
        Map<String, String> keySet = new HashMap<String, String>();
        Map<String, String> keyHash = new HashMap<String, String>();
        //for (SyncEntry s : ses) {
        //    Log.d("nils","Audit entry (s): "+s.getChange());
        //}
        Cursor c = null;
        SyncStatus syncStatus=new SyncStatus();
        String myTeam = globalPh.get(PersistenceHelper.LAG_ID_KEY);
        try {
            for (SyncEntry s : ses) {

                //Log.d("vortex", "SYNC:");
                // Log.d("plaz", "s.target :" + s.getTarget());
                //Log.d("plaz", "s.changes :" + s.getChange());
                //Log.d("plaz", "s.timestamp :" + s.getTimeStamp());

                synC++;
                if (s.isInsertArray()) {
                    java.util.Date date = new java.util.Date();
                    long current = date.getTime() / 1000;
                    long incoming =s.getTimeStamp();
                    long diff = current - incoming;
                    //Log.d("plaz", "ARRAYOBJECT curr incom: " + current + " " + incoming);
                    if (diffMoreThanThreshold(diff)) {
                        //Log.d("plaz", "discarding arrayobject...too oold");
                        continue;
                    } //else
                    // Log.d("plaz", "not discarding!!");

                }
                if (synC % 10 == 0) {
                    String syncStatusS = synC + "/" + size;
                    if (ui != null)
                        ui.setInfo(syncStatusS);
                    syncStatus.setStatus(syncStatusS);
                    if (syncListener != null)
                        syncListener.send(syncStatus);
                }


                if (s.isInsert() || s.isInsertArray()) {
                    keySet.clear();
                    cv.clear();
                    Map<String,String> sKeys = s.getKeys(), sValues = s.getValues();

                    if (sKeys == null || sValues == null) {
                        Log.e("vortex", "Synkmessage with " + s.getTarget() + " is invalid. Skipping. keys: " + s.getKeys() + " values: " + s.getValues());
                        changes.faults++;
                        continue;
                    }


                    name = sKeys.get("var");
                    for (String key : sKeys.keySet()) {



                        if (key.equals("var")) {
                            name = sKeys.get(key);
                        } else {
                            keySet.put(getDatabaseColumnName(key),sKeys.get(key));
                            keyHash.put(key, sKeys.get(key));
                        }
                        cv.put(getDatabaseColumnName(key), sKeys.get(key));

                    }
                    String myValue = null;
                    myValue=sValues.get("value");

                    for (String value : sValues.keySet()) {
                        cv.put(getDatabaseColumnName(value), sValues.get(value));
                    }
                    Selection sel = this.createSelection(keySet, name);
                    c = getExistingVariableCursor(name, sel);
                    long rId = -1;
                    boolean hasValueAlready = c.moveToNext();
                    if (!hasValueAlready || s.isInsertArray()) {// || gs.getVariableConfiguration().getnumType(row).equals(DataType.array)) {
                        //                  Log.d("sync", "INSERTING NEW (OR ARRAY) " + name);
                        //                  Log.d("sync", "cv: "+cv);
                        //now there should be ContentValues that can be inserted.
                        rId = db().insert(TABLE_VARIABLES, // table
                                null, //nullColumnHack
                                cv
                        );
                        //Insert also in cache if not array.
                        //
                        if (!s.isInsertArray())
                            gs.getVariableCache().insert(name, keyHash, myValue);
                        changes.inserts++;
                    } else {
                        long id = c.getLong(0);
                        long timestamp = c.getLong(1);
                        String value = c.getString(2);
                        String varName = c.getString(3);
                        String author = c.getString(4);

                        //Is the existing entry done by me?
                        //Log.d("vortex", "Existing is author" + author+",val: "+value+",var:"+varName+",timestamp: "+timestamp);
                        if (isMe(author)) {
                            if (varName.startsWith("STATUS:")) {
                                //                          Log.d("vortex", "This is a status variable");
                                String incomingValue = cv.getAsString("value");

                                //                          Log.d("vortex", "my value: " + value + " incoming: " + incomingValue + " values: " + s.getValues());
                                if (value != null && incomingValue != null && !value.equals("0")) {
                                    //                               Log.e("vortex", "found potential conflict between import value and existing for " + varName);
                                    //&& !value.equals(incomingValue)
                                    List<String> row = GlobalState.getInstance().getVariableConfiguration().getCompleteVariableDefinition(varName);
                                    String assocWorkflow = GlobalState.getInstance().getVariableConfiguration().getAssociatedWorkflow(row);

                                    //                             Log.d("vortex", "Assoc workflow is " + assocWorkflow);
                                    if (assocWorkflow != null && !assocWorkflow.isEmpty()) {
                                        //                                 Log.d("vortex", "conflict!");
                                        String ks = "";
                                        if (keyHash != null)
                                            ks = keyHash.toString();
                                        conflictFlows.add(assocWorkflow + " " + ks);
                                        changes.conflicts++;
                                    }

                                }
                            }
                        }

                        //If this is a status variable, and the value is different than existing value, add a conflict.


                        if (timestamp<s.getTimeStamp()) {
                            //                       Log.d("sync", "REPLACING " + name);
                            cv.put("id", id);
                            rId = db().replace(TABLE_VARIABLES, // table
                                    null, //nullColumnHack
                                    cv
                            );
                            gs.getVariableCache().insert(name, keyHash, myValue);
                            changes.inserts++;

                            if (rId != id) {
                                Log.e("sync", "CRY FOUL!!! New Id not equal to found! " + " ID: " + id + " RID: " + rId);

                                Log.e("sync", "varname: " + varName + "value: " + value + " timestamp: " + timestamp + " author: " + author + " ");
                                Log.e("sync", "CV: " + cv);
                            }
                        } else {
                            changes.refused++;
                            //                        o.addRow("");
                            //                        o.addYellowText("DB_INSERT REFUSED: " + name + " Timestamp incoming: " + s.getTimeStamp() + " Time existing: " + timestamp +" value: "+myValue);
                            //                        Log.d("vortex", "DB_INSERT REFUSED: " + name + " Timestamp incoming: " + s.getTimeStamp() + " Time existing: " + timestamp +" value: "+myValue);
                        }

                    }
                    if (rId != -1)
                        touchedVariables.add(name);

                    //Invalidate variables with this id in the cache..

                } else {
                    if (s.isDelete()) {
                        keySet.clear();
                        Log.d("sync", "Got Delete for: " + s.getTarget());
                        String[] sChanges = null;
                        if (s.getChange() != null)
                            sChanges = s.getChange().split("\\|");

                        if (sChanges == null) {
                            Log.e("vortex", "no keys in Delete Syncentry");
                            changes.faults++;
                            continue;
                        }
                        String[] pair;

                        keyHash.clear();
                        for (String keyPair : sChanges) {
                            pair = keyPair.split("=");
                            if (pair.length == 1) {
                                String k = pair[0];
                                pair = new String[2];
                                pair[0] = k;
                                pair[1] = "";
                            }
                            //Log.d("nils","Pair "+(c++)+": Key:"+pair[0]+" Value: "+pair[1]);

                            if (pair[0].equals("var")) {
                                name = pair[1];
                            } else {
                                keySet.put(getDatabaseColumnName(pair[0]), pair[1]);
                                keyHash.put(pair[0], pair[1]);
                            }

                        }
                        Log.d("sync", "DELETE WITH PARAMETER NAMED " + name);
                        Log.d("sync","Keyset:  "+keySet.toString());

                        Selection sel = this.createSelection(keySet, name);
                        //Log.d("sync","Selection:  "+sel.selection);
                        if (sel.selectionArgs != null) {
                            String xor = "";
                            for (String sz : sel.selectionArgs)
                                xor += sz + ",";
                            Log.d("nils", "Selection ARGS: " + xor);
                            Log.d("nils","Calling delete with Selection: "+sel.selection+" args: "+print(sel.selectionArgs));
                            //Check timestamp. If timestamp is older, delete. Otherwise skip.

                            //StoredVariableData sv = this.getVariable(s.getTarget(), sel);
                            c = getExistingVariableCursor(s.getTarget(), sel);
                            boolean hasValueAlready = c.moveToNext();
                            boolean existingTimestampIsMoreRecent = true;

                            if (hasValueAlready) {

                                long timestamp = c.getLong(1);
                                //String value = c.getString(2);
                                //String varName = c.getString(3);
                                //String author = c.getString(4);

                                //Is the existing entry done by me?
                                //                        Log.d("vortex", "Existing is author" + author+",val: "+value+",var:"+varName+",timestamp: "+timestamp);
                                //                        Log.d("vortex","incoming timestamp: "+s.getTimeStamp()+" existingTimestamp: "+timestamp);
                                existingTimestampIsMoreRecent = timestamp > s.getTimeStamp();
                            } else
                                Log.d("vortex", "Did not find variable to delete: " + s.getTarget());
                            if (!existingTimestampIsMoreRecent) {
                                Log.d("sync", "Deleting " + name);
                                this.deleteVariable(s.getTarget(), sel, false);
                                vc.insert(name, keyHash, null);
                                changes.deletes++;
                            } else {
                                changes.refused++;
                                Log.d("sync", "Did not delete.");
                                //                       o.addRow("");
                                //                       o.addYellowText("DB_DELETE REFUSED: " + name);
                                //                       if (hasValueAlready)
                                //                           o.addYellowText(" Timestamp incoming: " + s.getTimeStamp() + " Time existing: " + timestamp);
                                //                       else
                                //                           o.addYellowText(", since this variable has no value in my database");
                            }

                        } else
                            Log.e("sync", "SelectionArgs null in Delete Sync. S: " + s.getTarget() + " K: " + s.getKeys());

                    } else if (s.isDeleteMany()) {
                        String keyPairs = s.getChange();
                        String pattern = s.getTarget();
                        if (keyPairs != null) {
                            Log.d("sync", "Got Erase Many sync message with keyPairs: " + keyPairs);
                            int affectedRows = this.erase(keyPairs, pattern);
                            resetCache = true;
                            //Invalidate Cache...purposeless to invalidate only part.
                            o.addRow("");
                            o.addGreenText("DB_ERASE message executed in sync");
                            changes.deletes += affectedRows;

                        } else {
                            o.addRow("");
                            o.addRedText("DB_ERASE Failed. Message corrupt");
                            changes.faults++;
                        }
                    }
                }

                if (c!=null)
                    c.close();
            }
            //Log.d("vortex", "Touched variables: "+touchedVariables.toString());
            if (ui != null)
                ui.setInfo(synC + "/" + size);
            //Log.d("sync","UNLOCK!");
            endTransactionSuccess();

            //Add instructions in log if conflicts.
            if (changes.conflicts > 0) {
                o.addRow("");
                o.addRedText("You *may* have sync conflicts in the following workflow(s): ");
                int i = 1;
                for (String flow : conflictFlows) {
                    o.addRow("");
                    o.addRedText(i + ".: " + flow);
                    i++;
                }

                o.addRedText("Verify that the values are correct. If not, make corrections and resynchronise!");
            }
            if (resetCache)
                vc.reset();




        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
        //Invalidate all variables touched.
        // for (String varName : touchedVariables)
        //    vc.invalidateOnName(varName);



        return changes;
    }



    private boolean diffMoreThanThreshold(long time) {
        return (time/86400 > 0);
    }

    public static void printTime(long seconds) {

        long days = seconds / 86400;
        long sec = seconds % 60;
        long minutes = seconds % 3600 / 60;
        long hours = seconds % 86400 / 3600;
        System.out.println("Day " + days + " Hour " + hours + " Minute " + minutes + " Seconds " + sec);
    }


    private boolean isMe(String author) {
        if (globalPh != null && author != null)
            return globalPh.get(PersistenceHelper.USER_ID_KEY).equals(author);
        else
            Log.e("vortex", "globalPh or author was null in isme");
        return true;
    }



    public void syncDone(long timeStamp) {
        Log.d("vortex", "in syncdone with timestamp " + timeStamp);
        String lastS = GlobalState.getInstance().getPreferences().get(PersistenceHelper.TIME_OF_LAST_SYNC);
        if (lastS == null || lastS.equals(PersistenceHelper.UNDEFINED))
            lastS = "0";
        long lastTimeStamp = Long.parseLong(lastS);
        if (timeStamp > lastTimeStamp)
            GlobalState.getInstance().getPreferences().put(PersistenceHelper.TIME_OF_LAST_SYNC, Long.toString(timeStamp));
        else
            Log.e("nils", "The timestamp of this sync message is LESS than the current timestamp");
        //else
        //	Log.d("nils","maxstamp 0");
    }


    public int getNumberOfUnsyncedEntries() {
        int ret = 0;
        final String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
        if (GlobalState.getInstance()!=null) {
            Long timestamp = getSendTimestamp(team);
            //Log.d("biff","Time difference from now to my last sync is "+(System.currentTimeMillis()-timestamp)+". Timestamp: "+timestamp+" team: "+team+" tsglobal: "+timestamp2+" app: "+globalPh.get(PersistenceHelper.BUNDLE_NAME));

 //           Cursor c = db().query(TABLE_AUDIT, null,
 //                   "timestamp > ? AND " + DbHelper.LAG + " = ?", new String[]{timestamp.toString(), team}, null, null, "timestamp asc", null);
            Cursor c = db().query(TABLE_AUDIT, null,
                    "timestamp > ? AND lag = ?", new String[]{timestamp.toString(), team}, null, null, "timestamp asc", null);
            ret = c.getCount();
            c.close();
            return ret;
        }
        return 0;
    }


    public int erase(String keyPairs, String pattern) {
        if (keyPairs == null || keyPairs.isEmpty()) {
            Log.e("err", "keypairs null or empty in erase! Not allowed!");
            return 0;
        }

        Log.d("err", "In erase with keyPairs: " + keyPairs+" and pattern "+pattern);

        //map keypairs. Create delete statement.
        StringBuilder delStmt = new StringBuilder("");
        Map<String, String> map = new HashMap<String, String>();
        String pairs[] = keyPairs.split(",");
        String column, value;
        boolean last = false, exact = true;
        int i = 0;

        ArrayList<String> values = new ArrayList<String>();
        String[] valuesA = null;

        for (String pair : pairs) {
            last = i == (pairs.length - 1);
            String[] keyValue = pair.split("=");
            if (keyValue != null && keyValue.length == 2) {
                column = realColumnNameToDB.get(keyValue[0]);
                Log.d("err","column: "+column+" value: "+keyValue[1]);
                if (Constants.NOT_NULL.equals(keyValue[1])) {
                    Log.d("err","match for NN!");
                    delStmt.append(column + " IS NOT NULL");
                    //erase in cache will erase all keys containing pairs that have value.
                    exact = false;
                } else {
                    values.add(keyValue[1]);
                    map.put(keyValue[0], keyValue[1]);
                    delStmt.append(column + "= ?");
                }
            } else
                Log.e("err", "failed to split " + pair);
            if (!last)
                delStmt.append(" AND ");
            i++;
        }
        //Add pattern if there.
        if (pattern != null)
            delStmt.append(" AND var LIKE '" + pattern + "'");

        valuesA = values.toArray(new String[values.size()]);
        //     Log.d("vortex", "Delete statement is now " + delStmt);
        //     Log.d("vortex", "VALUES:"+print(valuesA));
        int affected = db().delete(DbHelper.TABLE_VARIABLES, delStmt.toString(), valuesA);
        //Invalidate affected cache variables
        if (affected > 0) {
            Log.d("bascar", "Deleted rows count: " + affected+" keys: "+keyPairs);

            Log.d("bascar", "cleaning up cache. Exact: " + exact);
            GlobalState.getInstance().getVariableCache().invalidateOnKey(map, exact);
        } //else
        //dr0++;
        return affected;
    }
    //static int dr0 = 0;
    public void eraseDelytor(String currentRuta, String currentProvyta) {

        //create WHERE part of delete statement.
        String deleteStatement = "år=" + Constants.getYear() + ",ruta=" + currentRuta + ",provyta=" + currentProvyta + ",delyta=" + Constants.NOT_NULL;
        Log.d("nils", "In EraseDelytor: [" + deleteStatement + "]");

        //Do it!
        erase(deleteStatement, null);

        //Create sync entry
        insertEraseAuditEntry(deleteStatement, null);

    }


    public void eraseProvyta(String currentRuta, String currentProvyta) {
        String deleteStatement = "år=" + Constants.getYear() + ",ruta=" + currentRuta + ",provyta=" + currentProvyta;
        Log.d("nils", "In EraseProvyta: [" + deleteStatement + "]");

        erase(deleteStatement, null);

        insertEraseAuditEntry(deleteStatement, null);


    }

    public void eraseSmaProvyDelytaAssoc(String currentRuta, String currentProvyta) {
        Log.d("vortex", "Calling erase with r " + currentRuta + ", p " + currentProvyta + " db: " + db);
        String yCol = realColumnNameToDB.get("år");
        String rCol = realColumnNameToDB.get("ruta");
        String pyCol = realColumnNameToDB.get("provyta");
        //		int affRows = db.delete(DbHelper.TABLE_VARIABLES,
        //				yCol+"=? AND "+rCol+"=? AND "+pyCol+"=? AND (var = '"+NamedVariables.BeraknadInomDelyta+"' OR var = '"+NamedVariables.InomDelyta+"')", new String[] {Constants.getYear(),currentRuta,currentProvyta});
        //		Log.d("nils","Affected rows in eraseSmaProvyDelytaAssoc: "+affRows);
    }

    public int deleteAllVariablesUsingKey(Map<String, String> keyHash) {
        if (keyHash == null)
            return -1;


        String queryP = "";
        String[] valA = new String[keyHash.keySet().size()];
        Iterator<String> it = keyHash.keySet().iterator();
        String key;
        int i = 0;
        while (it.hasNext()) {
            key = it.next();
            queryP += getDatabaseColumnName(key) + "= ?";
            if (it.hasNext())
                queryP += " AND ";
            valA[i++] = keyHash.get(key);
        }
        int affRows = db().delete(DbHelper.TABLE_VARIABLES,
                queryP, valA);
        StringBuilder valAs = new StringBuilder();
        for (String v : valA) {
            valAs.append(v + ",");
        }
        Log.e("vortex", "Deleted " + affRows + " entries in deleteAllVariablesUsingKey. Query: " + queryP + " vals " + valAs);
        return affRows;
    }


    private final ContentValues valuez = new ContentValues();
    final static String NULL = "null";


    public boolean deleteHistory() {
        try {
            Log.d("nils", "deleting all historical values");
            int rows = db().delete(TABLE_VARIABLES, getDatabaseColumnName("år") + "= ?", new String[]{Constants.HISTORICAL_TOKEN_IN_DATABASE});
            Log.d("nils", "Deleted " + rows + " rows of history");
        } catch (SQLiteException e) {
            Log.d("nils", "not a nils db");
            return false;
        }
        return true;
    }

    public boolean deleteHistoryEntries(String typeColumn, String typeValue) {
        try {
            Log.d("nils", "deleting historical values of type " + typeValue);
            int rows = db().delete(TABLE_VARIABLES, getDatabaseColumnName("år") + "= ? AND " + getDatabaseColumnName(typeColumn) + "= ? COLLATE NOCASE", new String[]{Constants.HISTORICAL_TOKEN_IN_DATABASE, typeValue});
            Log.d("nils", "Deleted " + rows + " rows of history");
        } catch (SQLiteException e) {
            Log.d("nils", "not a nils db");
            return false;
        }
        return true;
    }

    public boolean fastInsert(Map<String, String> key, String varId, String value) {
        valuez.clear();
        String timeStamp = (System.currentTimeMillis()) + "";

        for (String k : key.keySet())
            valuez.put(getDatabaseColumnName(k), key.get(k));
        valuez.put("var", varId);
        valuez.put("value", value);
        valuez.put("lag", globalPh.get(PersistenceHelper.LAG_ID_KEY));
        valuez.put("timestamp", timeStamp);
        valuez.put("author", globalPh.get(PersistenceHelper.USER_ID_KEY));


        //Log.d("nils","inserting:  "+valuez.toString());
        try {
            db().insert(TABLE_VARIABLES, // table

                    null, //nullColumnHack
                    valuez
            );
        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }
        return true;

    }


    public boolean fastHistoricalInsert(Map<String, String> keys,
                                        String varId, String value) {

        valuez.clear();
        valuez.put(getDatabaseColumnName("år"), Constants.HISTORICAL_TOKEN_IN_DATABASE);


        for (String key : keys.keySet()) {

            if (keys.get(key) != null) {
                if (realColumnNameToDB.get(key) != null)
                    valuez.put(realColumnNameToDB.get(key), keys.get(key));
                else {
                    Log.e("vortex","Could not find key "+key+" in keychain for "+varId);
                    //Column not found. Do not insert!!
                    return false;
                }

            }
        }
        valuez.put("var", varId);
        valuez.put("value", value);
        try {
            db().insert(TABLE_VARIABLES, // table
                    null, //nullColumnHack
                    valuez
            );
        } catch (SQLiteException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public void insertGisObject(GisObject go) {
        Variable gpsCoord = GlobalState.getInstance().getVariableCache().getVariable(go.getKeyHash(), GisConstants.GPS_Coord_Var_Name);
        Variable geoType = GlobalState.getInstance().getVariableCache().getVariable(go.getKeyHash(), GisConstants.Geo_Type);
        if (gpsCoord == null || geoType == null) {
            LoggerI o = GlobalState.getInstance().getLogger();
            o.addRow("");
            o.addRedText("Insert failed for GisObject " + go.getLabel() + " since one or both of the required variables " + GisConstants.GPS_Coord_Var_Name + " and " + GisConstants.Geo_Type + " is missing from Variables.csv. Please add these and check spelling");
            Log.e("vortex", "Insert failed for GisObject " + go.getLabel() + " since one or both of the required variables " + GisConstants.GPS_Coord_Var_Name + " and " + GisConstants.Geo_Type + " is missing from Variables.csv. Please add these and check spelling");
            return;
        }
        insertVariable(gpsCoord, go.coordsToString(), true);
        insertVariable(geoType, go.getGisPolyType().name(), true);
        if (gpsCoord == null || geoType == null) {
            Log.e("vortex", "Insert failed for " + GisConstants.GPS_Coord_Var_Name + ". Hash: " + go.getKeyHash().toString());
        } else
            Log.d("vortex", "succesfully inserted new gisobject");
    }

		/*
		public String getHistoricalValue(String varName,Map<String, String> keyChain) {
			HashMap<String, String> histKeyChain = new HashMap<String,String>(keyChain);
			histKeyChain.put(VariableConfiguration.KEY_YEAR, VariableConfiguration.HISTORICAL_MARKER);
			return getValue(varName,createSelection(histKeyChain,varName),new String[] {VALUE});
		}
		 */
    //Get values for all instances of a given variable, from a keychain with * values.

    public DBColumnPicker getAllVariableInstances(Selection s) {
        Cursor c = db().query(TABLE_VARIABLES, null, s.selection,
                s.selectionArgs, null, null, null, null);//"timestamp DESC","1");
        return new DBColumnPicker(c);
    }

    public DBColumnPicker getLastVariableInstance(Selection s) {
        Cursor c = db().query(TABLE_VARIABLES, null, s.selection,
                s.selectionArgs, null, null, "timestamp DESC", "1");
        return new DBColumnPicker(c);
    }

    //Generates keychains for all instances.
    public Set<Map<String, String>> getKeyChainsForAllVariableInstances(String varID,
                                                                        Map<String, String> keyChain, String variatorColumn) {
        Set<Map<String, String>> ret = null;
        String variatorColTransl = this.getDatabaseColumnName(variatorColumn);
        //Get all instances of variable for variatorColumn.
        Selection s = this.createSelection(keyChain, varID);
        Cursor c = db().query(TABLE_VARIABLES, new String[]{variatorColTransl},
                s.selection, s.selectionArgs, null, null, null, null);
        Map<String, String> varKeyChain;
        if (c != null && c.moveToFirst()) {
            String variatorInstance;
            do {
                variatorInstance = c.getString(0);
                //Log.d("nils","Found instance value "+variatorInstance+" for varID "+varID+" and variatorColumn "+variatorColumn+" ("+variatorColTransl+")");
                varKeyChain = new HashMap<String, String>(keyChain);
                varKeyChain.put(variatorColumn, variatorInstance);
                if (ret == null)
                    ret = new HashSet<Map<String, String>>();
                ret.add(varKeyChain);
            } while (c.moveToNext());


        }
        if (c != null)
            c.close();
        return ret;
    }

    private final Map<String,Map<Map<String,String>,Map<String,String>>> mapCache= new HashMap<String, Map<Map<String, String>, Map<String, String>>>();

    public Map<String, String> preFetchValuesForAllMatchingKey(Map<String, String> keyChain, String namePrefix) {
        Map<String, String> ret = null;
        Map<Map<String, String>, Map<String, String>> map = mapCache.get(namePrefix);
        if (map!=null) {
            ret = map.get(keyChain);

            if (ret!=null) {
                Log.d("baza", "returning cached object.");
                return ret;
            }
        }
        ret = new HashMap<String, String>();
        if (map==null)
            map = new HashMap<Map<String, String>, Map<String, String>>();
        map.put(keyChain,ret);
        mapCache.put(namePrefix,map);

        long timeE = System.currentTimeMillis();
        String query = "SELECT " + VARID + ",value FROM " + TABLE_VARIABLES +
                " WHERE " + VARID + " LIKE '" + namePrefix + "%'";

        //Add keychain parts.
        String[] selArgs = new String[keyChain.size()];
        int i = 0;
        for (String key : keyChain.keySet()) {
            query += " AND " + this.getDatabaseColumnName(key) + "= ?";
            selArgs[i++] = keyChain.get(key);
            Log.d("vortex", "column: " + this.getDatabaseColumnName(key) + " SelArg: " + keyChain.get(key));
        }
        Log.d("vortex", "Selarg: " + selArgs.toString());


        Cursor c = db().rawQuery(query, selArgs);
        Log.d("nils", "Got " + c.getCount() + " results. PrefetchValue." + namePrefix + " with key: " + keyChain.toString());

        if (c != null && c.moveToFirst()) {
            do {
                ret.put(c.getString(0), c.getString(1));
            } while (c.moveToNext());

        }
        if (c != null)
            c.close();
        Log.d("timex","Time spent: "+(System.currentTimeMillis()-timeE));

        return ret;

    }

    public class TmpVal {
        public String hist,norm;

    }

    public Map<String, TmpVal> preFetchValuesForAllMatchingKeyV(Map<String, String> keyChain) {
        final List<String> selectionArgs = new ArrayList<String>();
        final String AR = getDatabaseColumnName("år");
        boolean hist = false;
        StringBuilder selection = new StringBuilder();

        Map<String, String> transMap = new HashMap<String, String>();
        if (keyChain != null) {
            for (String key : keyChain.keySet()) {
                transMap.put(getDatabaseColumnName(key), keyChain.get(key));
            }
            //Make sure key contains current year.
            //transMap.remove(AR);
            //transMap.put(AR,Constants.getYear());
        }


        boolean last = false;
        int arIndex = -1;
        for (int i = 1; i <= NO_OF_KEYS; i++) {
            last = i == NO_OF_KEYS;
            String key = "L" + i;
            //forget year.
            //columns.add(key);
            if (transMap.get(key) != null) {

                String columnValue = transMap.get(key);
                if ("?".equals(columnValue))
                    selection.append(key + " NOT NULL ");
                else {
                    selection.append(key + "=? ");
                    selectionArgs.add(columnValue);
                }
                if (key.equals(AR)) {
                    arIndex = selectionArgs.size() - 1;
                    hist = Constants.HISTORICAL_TOKEN_IN_DATABASE.equals(selectionArgs.get(arIndex));

                }
            } else
                selection.append(key + " IS NULL ");
            if (!last)
                selection.append("AND ");
        }

        int histC = 0;
        //		if (!key.equals(AR)) {
        String[] selArgs = selectionArgs.toArray(new String[selectionArgs.size()]);
//        Log.d("vortex", "selection: " + selection);
//        Log.d("vortex", "selectionArgs: " + selectionArgs);
        Cursor c = db().query(true, TABLE_VARIABLES, new String[]{VARID, "value"}, selection.toString(), selArgs, null, null, null, null);
//        Log.d("vortex", "Got " + c.getCount() + " results in norm ");
        //Now also query the historical values. If any.
        Map<String, TmpVal> tmp = new HashMap<String, TmpVal>();
        while (c.moveToNext()) {
            getTmpVal(c.getString(0), tmp).norm = c.getString(1);
            if (hist)
                getTmpVal(c.getString(0), tmp).hist = c.getString(1);
        }
        c.close();
        if (!hist &&arIndex != -1 ) {
            selectionArgs.set(arIndex, Constants.HISTORICAL_TOKEN_IN_DATABASE);
            //Log.d("vortex","historical selloArgs: "+selectionArgs);
            selArgs = selectionArgs.toArray(new String[selectionArgs.size()]);
            Cursor d = db().query(true, TABLE_VARIABLES, new String[]{VARID, "value"}, selection.toString(), selArgs, null, null, null, null);
            histC = d.getCount();
//            Log.d("vortex", "Got " + histC + " results in hist ");
            while (d.moveToNext())
                getTmpVal(d.getString(0), tmp).hist = d.getString(1);
            d.close();
        }


//        Log.d("vortex", "Tmpval has " + tmp.values().size() );
        /*
			for (String v:tmp.keySet()) {
					TmpVal tv = tmp.get(v);
					Log.e("vortex","VAR: "+v+" NORM: "+tv.norm);
			}

        */


        return tmp;


    }


    private TmpVal getTmpVal(String id, Map<String, TmpVal> tmp) {
        TmpVal x = tmp.get(id);
        if (x == null) {
            x = new TmpVal();
            tmp.put(id, x);
        }
        return x;
    }

    //Fetch all instances of Variables matching namePrefix (group id). Map varId to a Map of Variator, Value.
    public Map<String, Map<String, String>> preFetchValues(Map<String, String> keyChain, String namePrefix, String variatorColumn) {

        Cursor c = getPrefetchCursor(keyChain, namePrefix, variatorColumn);
        Map<String, Map<String, String>> ret = new HashMap<String, Map<String, String>>();
        if (c != null && c.moveToFirst()) {
            Log.d("nils", "In prefetchValues. Got " + c.getCount() + " results. PrefetchValues " + namePrefix + " with key " + keyChain.toString());
            do {
                String varId = c.getString(0);
                if (varId != null) {
                    Map<String, String> varMap = ret.get(varId);
                    if (varMap == null) {
                        varMap = new HashMap<String, String>();
                        ret.put(varId, varMap);
                    }
                    varMap.put(c.getString(1), c.getString(2));
                }
                Log.d("nils", "varid: " + c.getString(0) + " variator: " + c.getString(1) + " value: " + c.getString(2));
            } while (c.moveToNext());

        }
        if (c != null)
            c.close();
        return ret;

    }


    //Fetch all instances of Variables matching namePrefix. Map varId to a Map of Variator, Value.
    public Cursor getPrefetchCursor(Map<String, String> keyChain, String namePrefix, String variatorColumn) {

        String query = "SELECT " + VARID + "," + getDatabaseColumnName(variatorColumn) + ",value FROM " + TABLE_VARIABLES +
                " WHERE " + VARID + " LIKE '" + namePrefix + "%'";
        String[] selArgs=null;
        if (keyChain!=null) {
            //Add keychain parts.
            selArgs = new String[keyChain.size()];
            int i = 0;
            for (String key : keyChain.keySet()) {
                query += " AND " + this.getDatabaseColumnName(key) + "= ?";
                selArgs[i++] = keyChain.get(key);
            }
        }
        Log.d("nils", "Query: " + query);
        //Return cursor.
        return db().rawQuery(query, selArgs);

    }


    //Fetch all instances of Variables matching namePrefix. Map varId to a Map of Variator, Value.
    public Cursor getAllVariablesForKeyMatchingGroupPrefixAndNamePostfix(Map<String, String> keyChain, String namePrefix, String namePostfix) {

        String query = "SELECT " + VARID + ",value FROM " + TABLE_VARIABLES +
                " WHERE " + VARID + " LIKE '" + namePrefix + "%' AND " + VARID + " LIKE " + "'%" + namePostfix + "'";

        //Add keychain parts.
        String[] selArgs = new String[keyChain.size()];
        int i = 0;
        for (String key : keyChain.keySet()) {
            query += " AND " + this.getDatabaseColumnName(key) + "= ?";
            selArgs[i++] = keyChain.get(key);
        }

        Log.d("vortex", "Query: " + query);
        //Return cursor.
        return db().rawQuery(query, selArgs);

    }


    public void beginTransaction() {
        db().beginTransaction();
    }

    public void endTransactionSuccess() {
        db().setTransactionSuccessful();
        db().endTransaction();
    }


    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void processSyncEntriesIfAny() {
        //check and process sync entries.
        final String[] dataColumn = new String[]{"id,data"};
        //get a cursor.
        //		db.beginTransaction();
        Cursor c = db().query(TABLE_SYNC, dataColumn, null, null, null, null, null, null);
        int lastId = -1;
        while (c.moveToNext()) {
            int id = c.getInt(0);
            Log.d("vortex", "id: " + id);
        }
        c.close();
        //		db.endTransaction();
        //String[] lastIdS=new String[]{lastId+""};
        //db.delete(TABLE_SYNC, "id<?", lastIdS);
    }

    /**
     * Scan all sync entry rows in the sync table.
     *
     * @return true if any changes done to this Apps database by any of the sync entries
     */





    public long getSyncRowsLeft() {
        return DatabaseUtils.queryNumEntries(db(),TABLE_SYNC);
        //Cursor c = db().rawQuery("SELECT Count(*) FROM " +  , null);
        //if (c.moveToFirst())
        //    return c.getInt(0);
        //Log.e("vortex","fiasko");
        //return 0;
    }

    public Cursor getSyncDataCursor() {
        return db().rawQuery("SELECT id,data FROM "+TABLE_SYNC+" order by id asc",null);
    }

    public int deleteConsumedSyncEntries(int id) {
        return db().delete(TABLE_SYNC, "id <=" + id, null);
    }


    public void insertIfMax(SyncReport sr) {
        //if sub exist?
        boolean hasSub = hasDatabaseColumnName("sub");
        TimeStampedMap tsMap = sr.getTimeStampedMap();
        VariableCache varCache = GlobalState.getInstance().getVariableCache();
        long t = System.currentTimeMillis();
        Set<Integer> idsToDelete = new HashSet<>();
        String query = "SELECT id,timestamp,"+getDatabaseColumnName("år")+","+VARID+","+getDatabaseColumnName("uid")+
                (hasSub?","+getDatabaseColumnName("sub"):"")+
                " from "+TABLE_VARIABLES+" where "+getDatabaseColumnName("år")+"<>'H' AND "+getDatabaseColumnName("uid")+" NOT NULL";
        //Log.d("kakko","query: "+query);
        Cursor c = db().rawQuery(query,null);
        //Log.d("rosto","C size: "+c.getCount());
        while (c.moveToNext()) {
            String vid = c.getString(3);
            String uid = c.getString(4);
            String sub = (hasSub?c.getString(5):null);
            Unikey ukey = Unikey.FindKeyFromParts(uid,sub,tsMap.getKeySet());
            if (ukey!=null) {
                //Log.d("sync", "Found ukey " + uid + ": " + vid);

                ContentValues cv = tsMap.get(ukey, vid);
                if (cv != null) {
                    //Log.d("sync", "MATCH FOR " + uid + ": " + vid);
                    long existingts = c.getLong(1);
                    long timestamp = cv.getAsLong("timestamp");
                    if (timestamp > existingts) {
                        int id = c.getInt(0);
                        idsToDelete.add(id);
                        //invalidate in cache.

                        //db.execSQL("Delete from " + TABLE_VARIABLES + " where " + VARID + " = '" + vid + "' AND " + getDatabaseColumnName("uid")+"= '"+uid+"'");
                    } else
                        tsMap.delete(ukey, vid);
                }
            }
        }
        c.close();
        //Insert any remaining rows.

        db().beginTransaction();
        if (idsToDelete.size()>0) {
            Log.d("sync","Deleteing "+idsToDelete.size()+" rows");
            sr.deletes+=idsToDelete.size();

            String delStr = String.format("DELETE FROM "+TABLE_VARIABLES+" WHERE id IN (%s)", TextUtils.join(", ", idsToDelete));
            db().execSQL(delStr);
            Log.d("sync","delStr: "+delStr);
        }

        ContentValues cv;
        Set<Unikey> keys = tsMap.getKeySet();

        for (Unikey uid:keys) {
            Map<String,ContentValues> m = tsMap.get(uid);
            if (m!=null) {
                //For each variable...
                for (String vid:m.keySet()) {
                    cv=m.get(vid);
                    db().insert(TABLE_VARIABLES, null, cv);
                    //Log.d("sync","inserted "+cv);
                    varCache.turboRemoveOrInvalidate(uid.getUid(),uid.getSub(),vid,true);
                    sr.inserts++;
                }
            }
        }
        endTransactionSuccess();
        Log.d("berokk","STATS:");
        Log.d("berokk","inserts: "+ sr.inserts);
        Log.d("berokk","insertArrays: "+ sr.insertsArray);
        Log.d("berokk","deletes: "+ sr.deletes);
        Log.d("berokk","failedDel: "+ sr.failedDeletes);
        Log.d("berokk","faultInKeys: "+ sr.faultInKeys);
        Log.d("berokk","faultInValues: "+ sr.faultInValues);
        Log.d("berokk","refused: "+ sr.refused);
        Log.d("berokk","time used: "+(System.currentTimeMillis()-t));

    }




    public void saveTimeStampOfLatestSuccesfulSync(String syncgroup) {
        saveSyncTimestamp(Constants.TIMESTAMP_LATEST_SUCCESFUL_SYNC,syncgroup,new Timestamp(System.currentTimeMillis()).getTime());
    }

    public long getTimestampOfLatestSuccesfulSync(String syncgroup) {
        return getSyncTimestamp(Constants.TIMESTAMP_LATEST_SUCCESFUL_SYNC,syncgroup);
    }

    public long getSendTimestamp(String syncgroup) {
        return getSyncTimestamp(Constants.TIMESTAMP_SYNC_SEND,syncgroup);
    }
    public long getReceiveTimestamp(String syncgroup) {
        return getSyncTimestamp(Constants.TIMESTAMP_SYNC_RECEIVE,syncgroup);
    }
    public void saveReceiveTimestamp(String syncgroup, long ts) {
        saveSyncTimestamp(Constants.TIMESTAMP_SYNC_RECEIVE,syncgroup,ts);
    }

    private void saveSyncTimestamp(String timeStampLabel, String syncgroup,long ts) {
        Log.d("antrax","Saving timestamp "+ts+ "for syncgroup "+syncgroup);
        db().execSQL("INSERT into "+TABLE_TIMESTAMPS+" (SYNCGROUP,LABEL,VALUE) values (?,?,?)",new String[]{syncgroup,timeStampLabel,Long.toString(ts)});
    }

    private long getSyncTimestamp(String timeStampLabel,String syncgroup) {
        long lastEntry;
        Cursor c  = this.getReadableDatabase().rawQuery("SELECT VALUE from "+TABLE_TIMESTAMPS+" where SYNCGROUP = ? AND LABEL = ? ORDER BY id DESC LIMIT 1", new String[]{syncgroup,timeStampLabel});
        if (c.getCount() != 0) {
            c.moveToFirst();
            lastEntry = c.getLong(0);
        } else {
            Log.e("vortex", "failed to find timestamp for " + timeStampLabel + "...returning 0");
            lastEntry = 0;
        }
        c.close();
        return lastEntry;
    }


}