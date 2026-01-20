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
import android.text.TextUtils;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.types.ArrayVariable;
import com.teraim.fieldapp.dynamic.types.Table;
import com.teraim.fieldapp.dynamic.types.Variable;
import com.teraim.fieldapp.dynamic.types.VariableCache;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisConstants;
import com.teraim.fieldapp.dynamic.workflow_realizations.gis.GisObject;
import com.teraim.fieldapp.log.LogRepository;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.stream.Collectors;

public class DbHelper extends SQLiteOpenHelper {

    private static final String TAG = "DbHelper";

    /* Database Version*/ private static final int DATABASE_VERSION = DbSchema.DATABASE_VERSION;/* Books table name*/
    private static final String TABLE_VARIABLES = DbSchema.TABLE_VARIABLES;
    public static final String TABLE_TIMESTAMPS = DbSchema.TABLE_TIMESTAMPS;


    public static final String TABLE_AUDIT = DbSchema.TABLE_AUDIT;
    public static final String TABLE_SYNC = DbSchema.TABLE_SYNC;


    private static final String VARID = DbSchema.COL_VARID;
    private static final String VALUE = DbSchema.COL_VALUE;
    private static final String TIMESTAMP = DbSchema.COL_TIMESTAMP;
    public static final String LAG = DbSchema.COL_LAG;
    public static final String AUTHOR = DbSchema.COL_AUTHOR;
    public static final String YEAR = DbSchema.COL_YEAR;
    private static final String[] VAR_COLS = DbSchema.VAR_COLS;
    //	private static final Set<String> MY_VALUES_SET = new HashSet<String>(Arrays.asList(VAR_COLS));

    private static final int NO_OF_KEYS = 10;
    private static final String SYNC_SPLIT_SYMBOL = "_$_";
    public SQLiteDatabase db;
    private PersistenceHelper globalPh = null;

    private final ColumnMapper columnMapper = new ColumnMapper();

    private final Context ctx;


    public static enum Table_Timestamps {
        LABEL,
        VALUE,
        SYNCGROUP
    }

    private static class ColumnMapper {
        private final Map<String, String> realToDb = new HashMap<String, String>();
        private final Map<String, String> dbToReal = new HashMap<String, String>();

        void addMapping(String realColumnName, String dbColumnName) {
            realToDb.put(realColumnName, dbColumnName);
            dbToReal.put(dbColumnName, realColumnName);
        }

        boolean containsRealKey(String realColumnName) {
            return realToDb.containsKey(realColumnName);
        }

        String getDbNameOrNull(String realColumnName) {
            return realToDb.get(realColumnName);
        }

        String getDbNameOrSelf(String realColumnName) {
            String mapped = realToDb.get(realColumnName);
            return mapped != null ? mapped : realColumnName;
        }

        String getRealNameOrSelf(String dbColumnName) {
            String mapped = dbToReal.get(dbColumnName);
            return mapped != null ? mapped : dbColumnName;
        }

        Set<String> getRealKeys() {
            return realToDb.keySet();
        }

        int size() {
            return realToDb.size();
        }
    }

    private static class SelectionBuilder {
        private final StringBuilder selection = new StringBuilder();
        private final List<String> args = new ArrayList<String>();
        private boolean hasClause = false;

        void addEquals(String columnName, String value) {
            if (value == null) {
                return;
            }
            if (!ensureColumn(columnName)) {
                return;
            }
            appendAndIfNeeded();
            selection.append(columnName).append(" = ?");
            args.add(value);
        }

        void addEqualsWithCollateNoCase(String columnName, String value) {
            if (value == null) {
                return;
            }
            if (!ensureColumn(columnName)) {
                return;
            }
            appendAndIfNeeded();
            selection.append(columnName).append(" = ? COLLATE NOCASE");
            args.add(value);
        }

        void addNotEquals(String columnName, String value) {
            if (value == null) {
                return;
            }
            if (!ensureColumn(columnName)) {
                return;
            }
            appendAndIfNeeded();
            selection.append(columnName).append(" <> ?");
            args.add(value);
        }

        void addIsNotNull(String columnName) {
            if (!ensureColumn(columnName)) {
                return;
            }
            appendAndIfNeeded();
            selection.append(columnName).append(" IS NOT NULL");
        }

        void addLike(String columnName, String pattern) {
            if (pattern == null) {
                return;
            }
            if (!ensureColumn(columnName)) {
                return;
            }
            appendAndIfNeeded();
            selection.append(columnName).append(" LIKE ?");
            args.add(pattern);
        }

        private void appendAndIfNeeded() {
            if (hasClause) {
                selection.append(" AND ");
            } else {
                hasClause = true;
            }
        }

        String buildSelection() {
            return selection.toString();
        }

        String[] buildArgs() {
            return args.isEmpty() ? null : args.toArray(new String[0]);
        }

        private boolean ensureColumn(String columnName) {
            if (columnName == null) {
                Log.e("DbHelper", "SelectionBuilder column is null");
                return false;
            }
            return true;
        }
    }
    //This function attempts to recover a newer  uuid from a old uuid using the variable GlobalID as key.
    public String findUIDFromGlobalId(String uid) {
        String uidColumn = this.getDatabaseColumnName("uid");
        SelectionBuilder selectionBuilder = new SelectionBuilder();
        selectionBuilder.addEquals(VALUE, "{" + uid + "}");
        selectionBuilder.addEquals(VARID, GisConstants.GlobalGid);
        selectionBuilder.addEquals(getDatabaseColumnName("år"), Constants.HISTORICAL_TOKEN_IN_DATABASE);

        String res = null;
        try (Cursor resultSet = db().query(
                TABLE_VARIABLES,
                new String[]{uidColumn},
                selectionBuilder.buildSelection(),
                selectionBuilder.buildArgs(),
                null,
                null,
                null,
                "1")) {
            if (resultSet.moveToNext()) {
                res = resultSet.getString(0);
            }
        }
        return res;
    }

    public String findVarFromUID(String uid,String variableName) {
        SelectionBuilder selectionBuilder = new SelectionBuilder();
        selectionBuilder.addEquals(this.getDatabaseColumnName("uid"), uid);
        selectionBuilder.addEquals(VARID, variableName);
        selectionBuilder.addEquals(getDatabaseColumnName("år"), Constants.HISTORICAL_TOKEN_IN_DATABASE);

        String result = null;
        try (Cursor cursor = db().query(
                TABLE_VARIABLES,
                new String[]{VALUE},
                selectionBuilder.buildSelection(),
                selectionBuilder.buildArgs(),
                null,
                null,
                null,
                "1")) {
            if (cursor.moveToNext()) {
                result = cursor.getString(0);
            }
        }
        return result;
    }

    public Map<String,String> createNotNullSelection(Map<String, String> myKeyHash) {
        String ar = null;
        String arval = null;
        Map<String, String> newKeyHash = new HashMap<>();
        List<String> columnsToSelect = new ArrayList<>();
        SelectionBuilder selectionBuilder = new SelectionBuilder();

        for (String key : myKeyHash.keySet()) {
            if (key.equalsIgnoreCase("ÅR")) {
                ar = key;
                arval = myKeyHash.get(key);
                continue;
            }
            String value = myKeyHash.get(key);
            String col = getDatabaseColumnName(key);
            Log.d(TAG, "createNotNullSelection: db column " + col + " for key " + key);
            columnsToSelect.add(col);
            if ("?".equals(value)) {
                selectionBuilder.addIsNotNull(col);
            } else {
                selectionBuilder.addEquals(col, value);
            }
        }

        String selection = selectionBuilder.buildSelection();
        String[] selectionArgs = selectionBuilder.buildArgs();
        String[] columns = columnsToSelect.toArray(new String[0]);

        try (Cursor c = db().query(true, TABLE_VARIABLES, columns, selection, selectionArgs, null, null, null, null)) {
            if (c.moveToNext()) {
                for (int i = 0; i < c.getColumnCount(); i++) {
                    Log.d(TAG, "createNotNullSelection: column " + c.getColumnName(i) + " value " + c.getString(i));
                    newKeyHash.put(getRealColumnNameFromDatabaseName(c.getColumnName(i)), c.getString(i));
                }

            }
            if (c.moveToNext()) {
                Log.e(TAG, "createNotNullSelection: more than one result");
            }
        }
        Log.d(TAG, "createNotNullSelection: resolved keyhash " + newKeyHash);
        if (!newKeyHash.isEmpty()) {
            if (ar != null)
                newKeyHash.put(ar, arval);
            return newKeyHash;
        }
        Log.e(TAG, "createNotNullSelection: failed to resolve unknown");
        LogRepository o = LogRepository.getInstance();
        o.addCriticalText("Failed to resolve unknown in context " + myKeyHash);
        return null;
    }

    public Map<String, String> createNotNullSelection(String[] rowKHA, Map<String, String> myKeyHash) {

        // The new key-hash to return
        Map<String, String> myNewKeyHash = new HashMap<>();

        List<String> columnsToSelect = new ArrayList<>();
        SelectionBuilder selectionBuilder = new SelectionBuilder();

        for (String key : rowKHA) {
            String dbColumnName = getDatabaseColumnName(key);
            columnsToSelect.add(dbColumnName);

            String existingValue = myKeyHash.get(key);

            if (existingValue == null) {
                selectionBuilder.addIsNotNull(dbColumnName);
            } else {
                selectionBuilder.addEquals(dbColumnName, existingValue);
            }
        }

        String finalSelection = selectionBuilder.buildSelection();
        String[] finalColumns = columnsToSelect.toArray(new String[0]);
        String[] finalSelectionArgs = selectionBuilder.buildArgs();

        // Use try-with-resources to ensure the Cursor is always closed automatically.
        try (Cursor c = db().query(true, TABLE_VARIABLES, finalColumns, finalSelection, finalSelectionArgs, null, null, null, "1")) {

            if (c.moveToNext()) {
                for (String colName : c.getColumnNames()) {
                    // Get the value using the column name for clarity
                    String value = c.getString(c.getColumnIndexOrThrow(colName));
                    myNewKeyHash.put(getRealColumnNameFromDatabaseName(colName), value);
                }
            }

            // The original code logged an error if more than one result was found.
            // This check is maintained. The "LIMIT 1" in the query makes this less likely,
            // but it's good practice to keep the verification.
            if (c.moveToNext()) {
                Log.e(TAG, "createNotNullSelection: more than one result");
            }

        } catch (Exception e) {
            Log.e(TAG, "createNotNullSelection: query failed", e);
            // Depending on requirements, you might want to return null or an empty map on error.
        }

        Log.d(TAG, "createNotNullSelection: returning keyhash " + myNewKeyHash);
        return myNewKeyHash;
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
            Set<String> keys = columnMapper.getRealKeys();
            String col = null;
            for (String key : keys) {
                col = columnMapper.getDbNameOrNull(key);
                if (col == null)
                    col = key;
                if (pick(col)!= null)
                    ret.put(key, pick(col));
            }
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
            Log.d(TAG, "db: opening writable database");
            db = this.getWritableDatabase();
        }
        return db;
    }

    public DbHelper(Context context, Table t, PersistenceHelper globalPh, PersistenceHelper appPh, String bundleName) {
        super(context, bundleName, null, DATABASE_VERSION);
        db = this.getWritableDatabase();
        Log.d(TAG, "DbHelper: bundle " + bundleName + " database version " + DATABASE_VERSION);
        ctx = context;


        this.globalPh = globalPh;
        if (t != null)
            init(t.getKeyParts(), appPh);
        else {
            Log.d(TAG, "DbHelper: table missing, init postponed");
        }


    }

    public void closeDatabaseBeforeExit() {
        if (db != null) {
            db.close();
            Log.d(TAG, "closeDatabaseBeforeExit: database closed");
        }
    }

    private void init(ArrayList<String> keyParts, PersistenceHelper appPh) {

        //check if keyParts are known or if a new is needed.

        //Load existing map from sharedStorage.
        String colKey;
        Log.d(TAG, "init: start");
        for (int i = 1; i <= NO_OF_KEYS; i++) {

            colKey = appPh.get("L" + i);
            //If empty, I'm done.
            if (colKey.equals(PersistenceHelper.UNDEFINED)) {
                Log.d(TAG, "init: missing key L" + i);
                break;
            } else {
                columnMapper.addMapping(colKey, "L" + i);
            }
        }
        //Now check the new keys. If a new key is found, add it.
        if (keyParts == null) {
            Log.e(TAG, "init: keyParts were null");
        } else {
            for (int i = 0; i < keyParts.size(); i++) {
                if (columnMapper.containsRealKey(keyParts.get(i))) {
                    Log.d(TAG, "init: key " + keyParts.get(i) + " already exists");
                } else if (staticColumn(keyParts.get(i))) {
                    Log.d(TAG, "init: key " + keyParts.get(i) + " is static");

                } else {
                    Log.d(TAG, "init: new column key " + keyParts.get(i));
                    if (keyParts.get(i).isEmpty()) {
                        Log.d(TAG, "init: empty keypart skipped");
                    } else {
                        String colId = "L" + (columnMapper.size() + 1);
                        //Add key to memory
                        columnMapper.addMapping(keyParts.get(i), colId);
                        //Persist new column identifier.
                        appPh.put(colId, keyParts.get(i));
                    }
                }

            }
        }
        Log.d(TAG, "init: keys added");
        Set<String> s = columnMapper.getRealKeys();
        for (String e : s)
            Log.d(TAG, "init: key " + e + " value " + columnMapper.getDbNameOrNull(e));

    }

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

        Log.d(TAG, "onCreate: database created");
    }

    @Override
    public void onUpgrade(SQLiteDatabase _db, int oldVersion, int newVersion) {
        Log.d(TAG, "onUpgrade: upgrading from " + oldVersion + " to " + newVersion);
        // Drop older books table if existed
        _db.execSQL("DROP TABLE IF EXISTS variabler");
        _db.execSQL("DROP TABLE IF EXISTS audit");
        _db.execSQL("DROP TABLE IF EXISTS sync");
        _db.execSQL("DROP TABLE IF EXISTS "+TABLE_TIMESTAMPS);
        // create fresh table
        this.onCreate(_db);
    }

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
        Log.d(TAG, "export: started file=" + exportFileName + " context=" + context);
        String selection = "";

        if (exporter instanceof GeoJSONExporter) {
            Log.d(TAG, "export: GeoJSON mode");
            selection = (getDatabaseColumnName("uid") + " NOT NULL " + (context != null ? "AND " : ""));
        }

        List<String> selArgs = null;
        if (context != null) {
            Log.d(TAG, "export: context=" + context);
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
        Log.d(TAG, "export: selection=" + selection);
        Log.d(TAG, "export: selectionArgs=" + selArgs);
        String[] selArgsA = null;
        if (selArgs != null)
            selArgsA = selArgs.toArray(new String[selArgs.size()]);
        Cursor c=null;
        try {
            c = db().query(TABLE_VARIABLES, null, selection,
                    selArgsA, null, null, null, null);
        } catch (SQLiteException e) {
            Log.d(TAG, "export: query failed selection=" + selection + " args=" + print(selArgsA));
        }

        if (c != null) {
            Log.d(TAG, "export: variables found for context " + context);
            //Wrap the cursor in an object that understand how to pick it!
            Report r = exporter.writeVariables(new DBColumnPicker(c));
            if (r != null && r.noOfVars > 0) {
                final Report res;
                if (Tools.writeToFile(exportFolder + exportFileName + "." + exporter.getType(), r.getData())) {
                    Log.d(TAG, "export: file written successfully");
                    LogRepository logger = LogRepository.getInstance();
                    logger.addText("Exported to folder: "+exportFolder);
                    c.close();
                    res = r;
                } else {
                    Log.e("nils", "Export of file failed");
                    c.close();
                    LogRepository.getInstance().addText("EXPORT FILENAME: [" + exportFolder + exportFileName + "." + exporter.getType() + "]");
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

    public void deleteVariable(String name, Selection s, boolean isSynchronized) {
        int aff =
                db().delete(TABLE_VARIABLES, //table name
                        s.selection,  // selections
                        s.selectionArgs); //selections args

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
        if (Constants.SYNC_FEATURE_DISABLED)
            return;
        //package the value array.
        String dd = "";

        if (s.selectionArgs != null) {
            String realColNames[] = new String[s.selectionArgs.length];
            //get the true column names.
            String selection = s.selection;
            if (selection == null) {
                Log.e(TAG, "insertDeleteAuditEntry: selection was null");
                return;
            }
            String selA = "";
            for (String ss : s.selectionArgs)
                selA += ss + ",";
            String zel[] = selection.split("=");
            for (int ii = 0; ii < s.selectionArgs.length; ii++) {
                String z = zel[ii];
                int iz = z.indexOf("L");
                if (iz == -1) {
                    if (!z.isEmpty()) {
                        int li = z.lastIndexOf(" ");
                        String last = z.substring(li + 1, z.length());
                        realColNames[ii] = last;

                    }

                } else {
                    String col = z.substring(iz, z.length());
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
        Log.d(TAG, "insertDeleteAuditEntry: args " + dd);
    }

    public void insertEraseAuditEntry(String keyPairs, String pattern) {
        if (Constants.SYNC_FEATURE_DISABLED)
            return;
        storeAuditEntry("M", keyPairs, pattern, System.currentTimeMillis(),globalPh.get(PersistenceHelper.USER_ID_KEY));
        Log.d(TAG, "insertEraseAuditEntry: keyPairs " + keyPairs + " pattern " + pattern);

    }


    private void insertAuditEntry(Variable v, Map<String, String> valueSet, String author, String action,long timestamp) {
        if (Constants.SYNC_FEATURE_DISABLED)
            return;
        String changes = "";
        //First the keys.
        Log.d(TAG, "insertAuditEntry: variable " + v.getId());
        Map<String, String> keyChain = v.getKeyChain();
        Iterator<Entry<String, String>> it;
        if (keyChain != null) {
            it = keyChain.entrySet().iterator();
            while (it.hasNext()) {
                Entry<String, String> entry = it.next();
                String value = entry.getValue();
                //KOKKO
                String column = entry.getKey();
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
        Cursor c = db().query(TABLE_VARIABLES, new String[]{"id", "timestamp", "value", "var", "author"},
                s.selection, s.selectionArgs, null, null, null, null);
        return c;
    }

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

        String[] substCols = new String[columns.length];

        for (int i = 0; i < columns.length; i++)
            substCols[i] = getDatabaseColumnName(columns[i]);
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
                    //only add row if one of the values is not null.
                    ret.add(row);
                }
            } while (c.moveToNext());
            if (ret.size()==0)
                Log.d(TAG, "getUniqueValues: found no values");
            c.close();
            return ret;
        }
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
        Cursor c = null;
        if (checkForNulls(s.selectionArgs)) {
            c = db().query(TABLE_VARIABLES, valueCol,
                    s.selection, s.selectionArgs, null, null, "timestamp DESC", "1");
            if (c != null && c.moveToFirst()) {
                String value = c.getString(0);
                c.close();

                return value;
            }
        }

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
        Cursor c = db().query(TABLE_VARIABLES, new String[]{"id"},
                s.selection, s.selectionArgs, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            int value = c.getInt(0);
            c.close();
            return value;
        }
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
        //Key need to be updated?
        if (var.isKeyVariable()) {
            var.getSelection().selectionArgs = createSelectionArgs(var.getKeyChain(), var.getId());
            //Check if the new chain leads to existing variable.
            int id = getId(var.getSelection());
            //Found match. Replace.
            if (id != -1) {
                Log.d(TAG, "insertVariable: existing variable " + var.getId());
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
            Log.e(TAG, "insertVariable: could not insert variable " + var.getId());
        } else {
            //If this variable is not local, store the action for synchronization.
            if (syncMePlease) {
                insertAuditEntry(var, createAuditEntry(var, newValue, timeStamp),author, "I",timeStamp);

            }
        }

        //delete lateron
        //Delete any existing value.
        deleteOldVariable(var.getId(),var.getSelection(), rId);

    }


    private void deleteOldVariable(final String name,final Selection s, final long newId) {
        if (s == null) {
            Log.e(TAG, "deleteOldVariable: selection is null");
            return;
        }
        String[] extendedSelArgs = new String[s.selectionArgs.length + 1];
        String extendedSelection = s.selection + " AND id <> ?";
        System.arraycopy(s.selectionArgs, 0, extendedSelArgs, 0, s.selectionArgs.length);
        String newIdS = null;
        try {
            newIdS = Long.toString(newId);
        } catch (NumberFormatException e) {
            Log.e(TAG, "deleteOldVariable: newId is not a number");
            return;
        }
        extendedSelArgs[extendedSelArgs.length - 1] = newIdS;
        int aff =
                db().delete(TABLE_VARIABLES, //table name
                        extendedSelection,  // selections
                        extendedSelArgs); //selections args
    }
    private void createValueMap(Variable var, String newValue, ContentValues values, long timeStamp,String author) {
        //Add column,value mapping.
        Map<String, String> keyChain = var.getKeyChain();
        //If no key column mappings, skip. Variable is global with Id as key.
        if (keyChain != null) {
            for (String key : keyChain.keySet()) {
                String value = keyChain.get(key);
                String column = getDatabaseColumnName(key);
                values.put(column, value);
            }
        } else
            Log.d(TAG, "createValueMap: inserting global variable " + var.getId() + " value " + newValue);
        values.put("var", var.getId());
        values.put(getDatabaseColumnName(var.getValueColumnName()), newValue);
        values.put("lag", globalPh.get(PersistenceHelper.LAG_ID_KEY));
        values.put("timestamp", timeStamp);
        values.put("author", author);

    }


    //Adds a value for the variable but does not delete any existing value.
    //This in effect creates an array of values for different timestamps.
    public void insertVariableSnap(ArrayVariable var, String newValue,
                                   boolean syncMePlease) {
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

    public SyncEntry[] getChanges(UIProvider ui) {
        long maxStamp;
        SyncEntry[] sa = null;
        String timestamp = globalPh.get(PersistenceHelper.TIME_OF_LAST_SYNC);
        String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
        if (timestamp.equals(PersistenceHelper.UNDEFINED))
            timestamp = "0";

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
                if (ui != null)
                    ui.setInfo(cn + "/" + c.getCount());
                cn++;
            } while (c.moveToNext());
            SyncEntryHeader seh = new SyncEntryHeader(maxStamp);
            sa[0] = seh;
        } else
            Log.d(TAG, "getChanges: no sync needed");
        //mySyncEntries = ret;
        if (c != null)
            c.close();
        return sa;
    }


    public static class Selection {
        public String[] selectionArgs = null;
        public String selection = null;
    }

    public Selection createSelection(Map<String, String> keySet, String name) {

        Selection ret = new Selection();
        //Create selection String.

        //If keyset is null, the variable is potentially global with only name as a key.
        StringBuilder selection = new StringBuilder();
        if (keySet != null) {
            //Does not exist...need to create.
            //1.find the matching column.
            for (String key : keySet.keySet()) {
                key = getDatabaseColumnName(key);

                selection.append(key);
                selection.append("= ? and ");

            }
        }
        selection.append("var= ?");
        ret.selection = selection.toString();
        ret.selectionArgs = createSelectionArgs(keySet, name);
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
        return columnMapper.getDbNameOrSelf(colId);
    }

    private boolean hasDatabaseColumnName(String c) {
        return columnMapper.containsRealKey(c);
    }


    private String getRealColumnNameFromDatabaseName(String databaseColumnName) {
        if (databaseColumnName == null || databaseColumnName.length() == 0)
            return null;
        return columnMapper.getRealNameOrSelf(databaseColumnName);
    }


    private int synC = 0;




    public SyncReport insertSyncEntries(SyncReport changes, SyncEntry[] ses, LogRepository o) {

        if (ses == null || ses.length==0) {
            Log.d(TAG, "insertSyncEntries: no sync entries");
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
                    }
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
                    tsMap.add(tsMap.getKey(uid,sub),variableName, cv);
                    if (!variableCache.turboRemoveOrInvalidate(uid, sub, variableName, true))
                        resetCache = true;
                } else {
                    db().insert(TABLE_VARIABLES, // table
                            null, //nullColumnHack
                            cv
                    );
                    variableCache.invalidateOnName(variableName);
                }
                changes.inserts++;



            }

            else if (s.isDelete()) {

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
                Log.d(TAG, "insertSyncEntries: delete-many start");
                Map keyPairs = s.getKeys();
                String pattern = s.getTarget();
                if (keyPairs != null) {
                    //pattern applies to variables.
                    int affectedRows = tsMap.delete(keyPairs,pattern);
                    //
                    changes.deletes += affectedRows;
                    Log.d(TAG, "insertSyncEntries: sync cache rows affected " + affectedRows);
                    //cache entries deleted in erase.
                    affectedRows = this.erase(s.getChange(), pattern);
                    Log.d(TAG, "insertSyncEntries: database rows affected " + affectedRows);

                } else {
                    o.addCriticalText("DB_ERASE Failed. Message corrupt");
                    changes.faults++;
                }
            } else {
                Log.d(TAG, "insertSyncEntries: unhandled action " + s.getAction() + " target " + s.getTarget());
            }

        }

        //insert max location update if any.

        if (!mostCurrentSyncMessage.isEmpty()) {
            for (String name:mostCurrentSyncMessage.keySet()) {
                Map<String, SyncEntry> map = mostCurrentSyncMessage.get(name);
                if (map!=null) {
                    for (String variable:map.keySet()) {
                        SyncEntry se = map.get(variable);
                        cv = createContentValues(se.getKeys(), se.getValues(), team);
                        db().insert(TABLE_VARIABLES, // table
                                null, //nullColumnHack
                                cv
                        );
                        //refresh cache
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
        Log.d(TAG, "delete: keys=" + keys + " timestamp=" + timeStamp + " team=" + team);
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

        Log.d(TAG, "delete: selection=" + whereClause + " args=" + print(whereArgs));
        //Calling delete with Selection: L4= ? AND L2= ? AND L1= ? AND L3= ? AND timestamp <= ? AND var = ? args: [0]: 2B1AFEF6-6C71-45DC-BB26-AF0B362E9073 [1]: 999994 [2]: 2016 [3]: Angsobete [4]: 1474478318 [5]: null [6]: STATUS:status_angsochbete

        return
                db().delete(TABLE_VARIABLES, //table name
                        whereClause.toString(),  // selections
                        whereArgs); //selections args

    }



    public SyncReport synchronise(SyncEntry[] ses, UIProvider ui, LogRepository o, SyncStatusListener syncListener) {
        if (ses == null) {
            Log.d(TAG, "synchronise: empty sync entry array");
            return null;
        }
        Set<String> touchedVariables = new HashSet<String>();
        SyncReport changes = new SyncReport();
        GlobalState gs = GlobalState.getInstance();
        VariableCache vc = gs.getVariableCache();
        Set<String> conflictFlows = new HashSet<String>();

        int size = ses.length - 1;

        db().beginTransaction();
        String name = null;
        boolean resetCache=false;

        synC = 0;
        if (ses.length == 0) {
            Log.e("plaz", "either syncarray is short or null. no data to sync.");
            db().endTransaction();
            return null;
        }
        ContentValues cv = new ContentValues();
        Map<String, String> keySet = new HashMap<String, String>();
        Map<String, String> keyHash = new HashMap<String, String>();
        Cursor c = null;
        SyncStatus syncStatus=new SyncStatus();
        String myTeam = globalPh.get(PersistenceHelper.LAG_ID_KEY);
        try {
            for (SyncEntry s : ses) {

                synC++;
                if (s.isInsertArray()) {
                    java.util.Date date = new java.util.Date();
                    long current = date.getTime() / 1000;
                    long incoming =s.getTimeStamp();
                    long diff = current - incoming;
                    if (diffMoreThanThreshold(diff)) {
                        continue;
                    } //else

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
                        if (isMe(author)) {
                            if (varName.startsWith("STATUS:")) {
                                String incomingValue = cv.getAsString("value");

                                if (value != null && incomingValue != null && !value.equals("0")) {
                                    //&& !value.equals(incomingValue)
                                    List<String> row = GlobalState.getInstance().getVariableConfiguration().getCompleteVariableDefinition(varName);
                                    String assocWorkflow = GlobalState.getInstance().getVariableConfiguration().getAssociatedWorkflow(row);

                                    if (assocWorkflow != null && !assocWorkflow.isEmpty()) {
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
                            //                        o.addText("");
                            //                        o.addYellowText("DB_INSERT REFUSED: " + name + " Timestamp incoming: " + s.getTimeStamp() + " Time existing: " + timestamp +" value: "+myValue);
                        }

                    }
                    if (rId != -1)
                        touchedVariables.add(name);

                    //Invalidate variables with this id in the cache..

                } else {
                    if (s.isDelete()) {
                        keySet.clear();
                        Log.d(TAG, "synchronise: delete entry for " + s.getTarget());
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

                            if (pair[0].equals("var")) {
                                name = pair[1];
                            } else {
                                keySet.put(getDatabaseColumnName(pair[0]), pair[1]);
                                keyHash.put(pair[0], pair[1]);
                            }

                        }
                        Log.d(TAG, "synchronise: delete variable " + name);
                        Log.d(TAG, "synchronise: delete keySet " + keySet);

                        Selection sel = this.createSelection(keySet, name);
                        if (sel.selectionArgs != null) {
                            String xor = "";
                            for (String sz : sel.selectionArgs)
                                xor += sz + ",";
            Log.d(TAG, "synchronise: selection args " + xor);
            Log.d(TAG, "synchronise: delete selection " + sel.selection + " args " + print(sel.selectionArgs));
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

                                existingTimestampIsMoreRecent = timestamp > s.getTimeStamp();
                            } else
                                Log.d(TAG, "synchronise: no variable to delete for " + s.getTarget());
                            if (!existingTimestampIsMoreRecent) {
                                Log.d(TAG, "synchronise: deleting " + name);
                                this.deleteVariable(s.getTarget(), sel, false);
                                vc.insert(name, keyHash, null);
                                changes.deletes++;
                            } else {
                                changes.refused++;
                                Log.d(TAG, "synchronise: delete refused");
                            }

                        } else
                            Log.e(TAG, "synchronise: selectionArgs null in delete sync for " + s.getTarget());

                    } else if (s.isDeleteMany()) {
                        String keyPairs = s.getChange();
                        String pattern = s.getTarget();
                        if (keyPairs != null) {
                            Log.d(TAG, "synchronise: erase many with keyPairs " + keyPairs);
                            int affectedRows = this.erase(keyPairs, pattern);
                            resetCache = true;
                            //Invalidate Cache...purposeless to invalidate only part.
                            o.addGreenText("DB_ERASE message executed in sync");
                            changes.deletes += affectedRows;

                        } else {
                            o.addCriticalText("DB_ERASE Failed. Message corrupt");
                            changes.faults++;
                        }
                    }
                }

                if (c!=null)
                    c.close();
            }
            if (ui != null)
                ui.setInfo(synC + "/" + size);
            endTransactionSuccess();

            //Add instructions in log if conflicts.
            if (changes.conflicts > 0) {
                o.addText("");
                o.addCriticalText("You *may* have sync conflicts in the following workflow(s): ");
                int i = 1;
                for (String flow : conflictFlows) {
                    o.addText("");
                    o.addCriticalText(i + ".: " + flow);
                    i++;
                }

                o.addCriticalText("Verify that the values are correct. If not, make corrections and resynchronise!");
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


    private boolean isMe(String author) {
        if (globalPh != null && author != null)
            return globalPh.get(PersistenceHelper.USER_ID_KEY).equals(author);
        else
            Log.e("vortex", "globalPh or author was null in isme");
        return true;
    }



    public void syncDone(long timeStamp) {
        Log.d(TAG, "syncDone: timestamp " + timeStamp);
        String lastS = GlobalState.getInstance().getPreferences().get(PersistenceHelper.TIME_OF_LAST_SYNC);
        if (lastS == null || lastS.equals(PersistenceHelper.UNDEFINED))
            lastS = "0";
        long lastTimeStamp = Long.parseLong(lastS);
        if (timeStamp > lastTimeStamp)
            GlobalState.getInstance().getPreferences().put(PersistenceHelper.TIME_OF_LAST_SYNC, Long.toString(timeStamp));
        else
            Log.e(TAG, "syncDone: timestamp is older than current");
    }


    public int getNumberOfUnsyncedEntries() {
        int ret = 0;
        final String team = globalPh.get(PersistenceHelper.LAG_ID_KEY);
        if (GlobalState.getInstance()!=null) {
            Long timestamp = getSendTimestamp(team);
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
            Log.e(TAG, "erase: keyPairs null or empty");
            return 0;
        }

        Log.d(TAG, "erase: keyPairs " + keyPairs + " pattern " + pattern);

        //map keypairs. Create delete statement.
        Map<String, String> map = new HashMap<String, String>();
        String pairs[] = keyPairs.split(",");
        String column, value;
        boolean exact = true;
        SelectionBuilder selectionBuilder = new SelectionBuilder();

        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue != null && keyValue.length == 2) {
                column = columnMapper.getDbNameOrNull(keyValue[0]);
                if (Constants.NOT_NULL.equals(keyValue[1])) {
                    selectionBuilder.addIsNotNull(column);
                    //erase in cache will erase all keys containing pairs that have value.
                    exact = false;
                } else {
                    map.put(keyValue[0], keyValue[1]);
                    selectionBuilder.addEquals(column, keyValue[1]);
                }
            } else
                Log.e(TAG, "erase: failed to split " + pair);
        }
        //Add pattern if there.
        selectionBuilder.addLike(VARID, pattern);

        String selection = selectionBuilder.buildSelection();
        String[] selectionArgs = selectionBuilder.buildArgs();
        int affected = db().delete(DbHelper.TABLE_VARIABLES, selection, selectionArgs);
        //Invalidate affected cache variables
        if (affected > 0) {
            Log.d(TAG, "erase: deleted " + affected + " rows for keys " + keyPairs);
            Log.d(TAG, "erase: cleaning up cache, exact " + exact);
            GlobalState.getInstance().getVariableCache().invalidateOnKey(map, exact);
        } //else
        return affected;
    }
    public void eraseDelytor(String currentRuta, String currentProvyta) {

        //create WHERE part of delete statement.
        String deleteStatement = "år=" + Constants.getYear() + ",ruta=" + currentRuta + ",provyta=" + currentProvyta + ",delyta=" + Constants.NOT_NULL;
        Log.d(TAG, "eraseDelytor: delete statement " + deleteStatement);

        //Do it!
        erase(deleteStatement, null);

        //Create sync entry
        insertEraseAuditEntry(deleteStatement, null);

    }


    public void eraseSmaProvyDelytaAssoc(String currentRuta, String currentProvyta) {
        Log.d(TAG, "eraseSmaProvyDelytaAssoc: ruta=" + currentRuta + " provyta=" + currentProvyta + " db=" + db);
        String yCol = columnMapper.getDbNameOrNull("år");
        String rCol = columnMapper.getDbNameOrNull("ruta");
        String pyCol = columnMapper.getDbNameOrNull("provyta");
    }

    public int deleteAllVariablesUsingKey(Map<String, String> keyHash) {
        if (keyHash == null)
            return -1;

        SelectionBuilder selectionBuilder = new SelectionBuilder();
        for (Map.Entry<String, String> entry : keyHash.entrySet()) {
            selectionBuilder.addEquals(getDatabaseColumnName(entry.getKey()), entry.getValue());
        }
        String queryP = selectionBuilder.buildSelection();
        String[] valA = selectionBuilder.buildArgs();

        int affRows = db().delete(DbHelper.TABLE_VARIABLES, queryP, valA);
        StringBuilder valAs = new StringBuilder();
        if (valA != null) {
            for (String v : valA) {
                valAs.append(v).append(",");
            }
        }
        return affRows;
    }


    private final ContentValues valuez = new ContentValues();
    final static String NULL = "null";


    public boolean deleteHistory() {
        try {
            Log.d(TAG, "deleteHistory: deleting historical values");
            SelectionBuilder selectionBuilder = new SelectionBuilder();
            selectionBuilder.addEquals(getDatabaseColumnName("år"), Constants.HISTORICAL_TOKEN_IN_DATABASE);
            int rows = db().delete(TABLE_VARIABLES, selectionBuilder.buildSelection(), selectionBuilder.buildArgs());
        } catch (SQLiteException e) {
            Log.d(TAG, "deleteHistory: not a NILS database");
            return false;
        }
        return true;
    }

    public boolean deleteHistoryEntries(String typeColumn, String typeValue) {
        try {
            Log.d(TAG, "deleteHistoryEntries: deleting historical values type=" + typeValue);
            SelectionBuilder selectionBuilder = new SelectionBuilder();
            selectionBuilder.addEquals(getDatabaseColumnName("år"), Constants.HISTORICAL_TOKEN_IN_DATABASE);
            selectionBuilder.addEqualsWithCollateNoCase(getDatabaseColumnName(typeColumn), typeValue);
            int rows = db().delete(
                    TABLE_VARIABLES,
                    selectionBuilder.buildSelection(),
                    selectionBuilder.buildArgs()
            );
            Log.d(TAG, "deleteHistoryEntries: deleted " + rows + " historical rows");
        } catch (SQLiteException e) {
            Log.d(TAG, "deleteHistoryEntries: not a NILS database");
            return false;
        }
        return true;
    }
    public void cleanDatabase() {
        //Erase all old status variables
        Map<String, String> keyHash = new HashMap<String, String>();
        keyHash.put("år", Constants.getYear());
        keyHash.put("var", "STATUS:status_trakter");
        deleteAllVariablesUsingKey(keyHash);
        Set<String> pyTypes = GlobalState.getInstance().getProvYtaTypes();
        if (pyTypes != null) {
            for (String pyType : pyTypes) {
                keyHash.put("var", "STATUS:status_" + pyType);
                deleteAllVariablesUsingKey(keyHash);
                Log.d(TAG, "cleanDatabase: erased status variables for " + pyType);
            }
        }
    }

    public boolean fastInsert(Map<String, String> key, String varId, String value) {
        valuez.clear();
        String timeStamp = (System.currentTimeMillis()) + "";

        for (String k : key.keySet())
            valuez.put(getDatabaseColumnName(k), key.get(k));
        valuez.put(getDatabaseColumnName("år"), Constants.getYear());
        valuez.put("var", varId);
        valuez.put("value", value);
        valuez.put("lag", globalPh.get(PersistenceHelper.LAG_ID_KEY));
        valuez.put("timestamp", timeStamp);
        valuez.put("author", globalPh.get(PersistenceHelper.USER_ID_KEY));


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
                if (columnMapper.getDbNameOrNull(key) != null)
                    valuez.put(columnMapper.getDbNameOrNull(key), keys.get(key));
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
            LogRepository o = LogRepository.getInstance();
            o.addCriticalText("Insert failed for GisObject " + go.getLabel() + " since one or both of the required variables " + GisConstants.GPS_Coord_Var_Name + " and " + GisConstants.Geo_Type + " is missing from Variables.csv. Please add these and check spelling");
            Log.e("vortex", "Insert failed for GisObject " + go.getLabel() + " since one or both of the required variables " + GisConstants.GPS_Coord_Var_Name + " and " + GisConstants.Geo_Type + " is missing from Variables.csv. Please add these and check spelling");
            return;
        }
        insertVariable(gpsCoord, go.coordsToString(), true);
        insertVariable(geoType, go.getGisPolyType().name(), true);
        if (gpsCoord == null || geoType == null) {
            Log.e("vortex", "Insert failed for " + GisConstants.GPS_Coord_Var_Name + ". Hash: " + go.getKeyHash().toString());
        } else
            Log.d(TAG, "insertGisObject: inserted new GIS object");
    }

    //Get values for all instances of a given variable, from a keychain with * values.

    public DBColumnPicker getAllVariableInstances(Selection s) {
        Cursor c = db().query(TABLE_VARIABLES, null, s.selection,
                s.selectionArgs, null, null, null, null);//"timestamp DESC","1");
        return new DBColumnPicker(c);
    }
    public DBColumnPicker getLatestVariableInstancesByUid(Selection s) {
        String uidColumnName = this.getDatabaseColumnName("uid");
        // Assuming your timestamp column (inserted by fastInsertStat) is named "timestamp"
        String timestampColumnName = "timestamp";

        // Build the SQL query using ROW_NUMBER()
        // This query selects all columns from TABLE_VARIABLES
        // It partitions (groups) the rows by the unique UID,
        // orders them by timestamp in descending order (latest first),
        // and assigns a row number. Finally, it filters to keep only the first row (rn = 1)
        // for each UID group.
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM (");
        queryBuilder.append("SELECT *, ROW_NUMBER() OVER (PARTITION BY ").append(uidColumnName);
        queryBuilder.append(" ORDER BY ").append(timestampColumnName).append(" DESC) AS rn ");
        queryBuilder.append("FROM ").append(TABLE_VARIABLES);

        String[] finalSelectionArgs = null;

        // Apply the selection 's' as a WHERE clause to filter the initial set of rows
        // before determining the latest by UID.
        if (s != null && s.selection != null && !s.selection.isEmpty()) {
            queryBuilder.append(" WHERE ").append(s.selection);
            finalSelectionArgs = s.selectionArgs; // Pass the arguments for the WHERE clause
        }
        queryBuilder.append(") WHERE rn = 1"); // Filter to get only the latest row for each UID

        String sqlQuery = queryBuilder.toString();

        Cursor c = null;
        try {
            SQLiteDatabase database = db();
            // rawQuery required for window function + dynamic SQL
            c = database.rawQuery(sqlQuery, finalSelectionArgs);
        } catch (Exception e) {
            Log.e("DB_HELPER", "Error fetching latest variable instances by UID: " + e.getMessage(), e);
            if (c != null) {
                c.close();
            }
            return null; // Or throw an exception, depending on your error handling policy
        }

        return new DBColumnPicker(c);
    }

    public DBColumnPicker getLastVariableInstance(Selection s) {
        Log.d(TAG, "getLastVariableInstance: selectionArgs " + print(s.selectionArgs));
        Log.d(TAG, "getLastVariableInstance: selection " + s.selection);
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
                Log.d(TAG, "preFetchValuesForAllMatchingKey: returning cached object");
                return ret;
            }
        }
        ret = new HashMap<String, String>();
        if (map==null)
            map = new HashMap<Map<String, String>, Map<String, String>>();
        map.put(keyChain,ret);
        mapCache.put(namePrefix,map);

        long timeE = System.currentTimeMillis();
        SelectionBuilder selectionBuilder = new SelectionBuilder();
        selectionBuilder.addLike(VARID, namePrefix + "%");
        //Add keychain parts.
        for (String key : keyChain.keySet()) {
            selectionBuilder.addEquals(this.getDatabaseColumnName(key), keyChain.get(key));
            Log.d(TAG, "preFetchValuesForAllMatchingKey: column " + this.getDatabaseColumnName(key) + " arg " + keyChain.get(key));
        }

        String selection = selectionBuilder.buildSelection();
        String[] selectionArgs = selectionBuilder.buildArgs();

        try (Cursor c = db().query(
                TABLE_VARIABLES,
                new String[]{VARID, VALUE},
                selection,
                selectionArgs,
                null,
                null,
                null,
                null)) {
            Log.d(TAG, "preFetchValuesForAllMatchingKey: got " + c.getCount() + " results for " + namePrefix + " with key " + keyChain);

            if (c.moveToFirst()) {
                do {
                    ret.put(c.getString(0), c.getString(1));
                } while (c.moveToNext());
            }
        }
        Log.d(TAG, "preFetchValuesForAllMatchingKey: time spent " + (System.currentTimeMillis() - timeE));

        return ret;

    }

    public class TmpVal {
        public String hist,norm;

    }

    public Map<String, TmpVal> preFetchValuesForAllMatchingKeyV(Map<String, String> keyChain) {
        // 1. Preparation: Translate external keys to database column names.
        final String yearColumn = getDatabaseColumnName("år");
        final Map<String, String> dbColumnFilters = (keyChain != null)
                ? keyChain.entrySet().stream()
                .collect(Collectors.toMap(e -> getDatabaseColumnName(e.getKey()), Map.Entry::getValue))
                : new HashMap<>();

        // 2. Query Building: Create the WHERE clause and arguments dynamically and safely.
        final List<String> selectionParts = new ArrayList<>();
        final List<String> selectionArgs = new ArrayList<>();
        final boolean isHistoricalQuery = Constants.HISTORICAL_TOKEN_IN_DATABASE.equals(dbColumnFilters.get(yearColumn));

        for (int i = 1; i <= NO_OF_KEYS; i++) {
            String key = "L" + i;
            String filterValue = dbColumnFilters.get(key);

            if (filterValue != null) {
                // Handle the year column specially to query both normal and historical values in one go.
                if (key.equals(yearColumn) && !isHistoricalQuery) {
                    selectionParts.add(key + " IN (?, ?)");
                    selectionArgs.add(Constants.getYear()); // Current year
                    selectionArgs.add(Constants.HISTORICAL_TOKEN_IN_DATABASE); // Historical token
                } else if ("?".equals(filterValue)) {
                    selectionParts.add(key + " NOT NULL");
                } else {
                    selectionParts.add(key + " = ?");
                    selectionArgs.add(filterValue);
                }
            } else {
                selectionParts.add(key + " IS NULL");
            }
        }

        String selection = String.join(" AND ", selectionParts);
        String[] finalSelectionArgs = selectionArgs.toArray(new String[0]);
        String[] columnsToFetch = {VARID, "value", yearColumn}; // Fetch the year column to differentiate results.

        Map<String, TmpVal> results = new HashMap<>();

        // 3. Execution: Use try-with-resources to guarantee the Cursor is always closed.
        try (Cursor cursor = db().query(true, TABLE_VARIABLES, columnsToFetch, selection, finalSelectionArgs, null, null, null, null)) {
            int varIdIndex = cursor.getColumnIndexOrThrow(VARID);
            int valueIndex = cursor.getColumnIndexOrThrow("value");
            int yearIndex = cursor.getColumnIndexOrThrow(yearColumn);

            while (cursor.moveToNext()) {
                String varId = cursor.getString(varIdIndex);
                String value = cursor.getString(valueIndex);
                String yearValue = cursor.getString(yearIndex);

                // Using computeIfAbsent is a cleaner way to get-or-create the TmpVal object.
                TmpVal tmpVal = results.computeIfAbsent(varId, k -> new TmpVal());

                // Populate either the historical or normal value based on the year column's data.
                if (Constants.HISTORICAL_TOKEN_IN_DATABASE.equals(yearValue)) {
                    tmpVal.hist = value;
                } else {
                    tmpVal.norm = value;
                }

                // If the original query was for historical, the historical value is also the "normal" one.
                if (isHistoricalQuery) {
                    tmpVal.norm = tmpVal.hist;
                }
            }
        } catch (Exception e) {
            // Log the exception, e.g., Log.e("DatabaseError", "Failed to fetch values", e);
            // Depending on requirements, you might want to re-throw or return an empty map.
            return new HashMap<>(); // Return empty map on failure
        }

        return results;
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
            Log.d(TAG, "preFetchValues: got " + c.getCount() + " results for " + namePrefix + " with key " + keyChain);
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
                Log.d(TAG, "preFetchValues: varId=" + c.getString(0) + " variator=" + c.getString(1) + " value=" + c.getString(2));
            } while (c.moveToNext());

        }
        if (c != null)
            c.close();
        return ret;

    }


    //Fetch all instances of Variables matching namePrefix. Map varId to a Map of Variator, Value.
    public Cursor getPrefetchCursor(Map<String, String> keyChain, String namePrefix, String variatorColumn) {

        SelectionBuilder selectionBuilder = new SelectionBuilder();
        selectionBuilder.addLike(VARID, namePrefix + "%");
        if (keyChain != null) {
            for (String key : keyChain.keySet()) {
                selectionBuilder.addEquals(this.getDatabaseColumnName(key), keyChain.get(key));
            }
        }
        String selection = selectionBuilder.buildSelection();
        String[] selectionArgs = selectionBuilder.buildArgs();
        Log.d(TAG, "getPrefetchCursor: selection " + selection);
        //Return cursor.
        return db().query(
                TABLE_VARIABLES,
                new String[]{VARID, getDatabaseColumnName(variatorColumn), VALUE},
                selection,
                selectionArgs,
                null,
                null,
                null,
                null
        );

    }


    //Fetch all instances of Variables matching namePrefix. Map varId to a Map of Variator, Value.
    public Cursor getAllVariablesForKeyMatchingGroupPrefixAndNamePostfix(Map<String, String> keyChain, String namePrefix, String namePostfix) {

        SelectionBuilder selectionBuilder = new SelectionBuilder();
        selectionBuilder.addLike(VARID, namePrefix + "%");
        selectionBuilder.addLike(VARID, "%" + namePostfix);
        for (String key : keyChain.keySet()) {
            selectionBuilder.addEquals(this.getDatabaseColumnName(key), keyChain.get(key));
        }

        String selection = selectionBuilder.buildSelection();
        String[] selectionArgs = selectionBuilder.buildArgs();

        Log.d(TAG, "getAllVariablesForKeyMatchingGroupPrefixAndNamePostfix: selection " + selection);
        //Return cursor.
        return db().query(
                TABLE_VARIABLES,
                new String[]{VARID, VALUE},
                selection,
                selectionArgs,
                null,
                null,
                null,
                null
        );

    }


    public void beginTransaction() {
        db().beginTransaction();
    }

    public void endTransactionSuccess() {
        db().setTransactionSuccessful();
        db().endTransaction();
    }


    /**
     * Scan all sync entry rows in the sync table.
     *
     * @return true if any changes done to this Apps database by any of the sync entries
     */





    public long getSyncRowsLeft() {
        return DatabaseUtils.queryNumEntries(db(),TABLE_SYNC);
    }

    public Cursor getSyncDataCursor() {
        return db().query(
                TABLE_SYNC,
                new String[]{"id", "data"},
                null,
                null,
                null,
                null,
                "id asc",
                null
        );
    }

    public int deleteConsumedSyncEntries(int id) {
        return db().delete(TABLE_SYNC, "id <= ?", new String[]{String.valueOf(id)});
    }


    public void insertIfMax(SyncReport sr) {
        //if sub exist?
        boolean hasSub = hasDatabaseColumnName("sub");
        TimeStampedMap tsMap = sr.getTimeStampedMap();
        VariableCache varCache = GlobalState.getInstance().getVariableCache();
        long t = System.currentTimeMillis();
        Set<Integer> idsToDelete = new HashSet<>();
        SelectionBuilder selectionBuilder = new SelectionBuilder();
        selectionBuilder.addNotEquals(getDatabaseColumnName("år"), "H");
        selectionBuilder.addIsNotNull(getDatabaseColumnName("uid"));
        String[] columns = hasSub
                ? new String[]{"id", "timestamp", getDatabaseColumnName("år"), VARID, getDatabaseColumnName("uid"), getDatabaseColumnName("sub")}
                : new String[]{"id", "timestamp", getDatabaseColumnName("år"), VARID, getDatabaseColumnName("uid")};
        Cursor c = db().query(
                TABLE_VARIABLES,
                columns,
                selectionBuilder.buildSelection(),
                selectionBuilder.buildArgs(),
                null,
                null,
                null,
                null
        );
        while (c.moveToNext()) {
            String vid = c.getString(3);
            String uid = c.getString(4);
            String sub = (hasSub?c.getString(5):null);
            Unikey ukey = Unikey.FindKeyFromParts(uid,sub,tsMap.getKeySet());
            if (ukey!=null) {
                ContentValues cv = tsMap.get(ukey, vid);
                if (cv != null) {
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
            Log.d(TAG, "insertIfMax: deleting " + idsToDelete.size() + " rows");
            sr.deletes+=idsToDelete.size();

            String delStr = String.format("DELETE FROM "+TABLE_VARIABLES+" WHERE id IN (%s)", TextUtils.join(", ", idsToDelete));
            db().execSQL(delStr);
            Log.d(TAG, "insertIfMax: delete SQL " + delStr);
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
                    varCache.turboRemoveOrInvalidate(uid.getUid(),uid.getSub(),vid,true);
                    sr.inserts++;
                }
            }
        }
        endTransactionSuccess();
        Log.d(TAG, "insertIfMax: stats");
        Log.d(TAG, "insertIfMax: inserts=" + sr.inserts);
        Log.d(TAG, "insertIfMax: insertArrays=" + sr.insertsArray);
        Log.d(TAG, "insertIfMax: deletes=" + sr.deletes);
        Log.d(TAG, "insertIfMax: failedDeletes=" + sr.failedDeletes);
        Log.d(TAG, "insertIfMax: faultInKeys=" + sr.faultInKeys);
        Log.d(TAG, "insertIfMax: faultInValues=" + sr.faultInValues);
        Log.d(TAG, "insertIfMax: refused=" + sr.refused);
        Log.d(TAG, "insertIfMax: elapsedMs=" + (System.currentTimeMillis() - t));

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
    private void saveSyncTimestamp(String timeStampLabel, String syncgroup,long ts) {
        Log.d(TAG, "saveSyncTimestamp: saving timestamp " + ts + " for syncgroup " + syncgroup);
        db().execSQL("INSERT into "+TABLE_TIMESTAMPS+" (SYNCGROUP,LABEL,VALUE) values (?,?,?)",new String[]{syncgroup,timeStampLabel,Long.toString(ts)});
    }

    private long getSyncTimestamp(String timeStampLabel,String syncgroup) {
        String selection = "SYNCGROUP = ? AND LABEL = ?";
        String[] selectionArgs = new String[]{syncgroup, timeStampLabel};
        try (Cursor c = this.getReadableDatabase().query(
                TABLE_TIMESTAMPS,
                new String[]{Table_Timestamps.VALUE.name()},
                selection,
                selectionArgs,
                null,
                null,
                "id DESC",
                "1"
        )) {
            if (c.moveToFirst()) {
                return c.getLong(0);
            }
        }
        Log.e("vortex", "failed to find timestamp for " + timeStampLabel + "...returning 0");
        return 0;
    }


}