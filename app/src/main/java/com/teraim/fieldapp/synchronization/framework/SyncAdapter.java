package com.teraim.fieldapp.synchronization.framework;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.teraim.fieldapp.non_generics.Constants;
import com.teraim.fieldapp.synchronization.EndOfStream;
import com.teraim.fieldapp.synchronization.SyncEntry;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.Tools;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.SyncFailedException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Handle the transfer of data between a server and an
 * app, using the Android sync adapter framework.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {

    private final ContentResolver mContentResolver;
    private final int MaxSyncableRowsServer;
    private final int MaxSyncableEntriesClient;
    private Messenger mClient;
    static final Uri BASE_CONTENT_URI;
    private static final Uri TIMESTAMPS_URI;
    private static final Uri SYNC_DATA_URI;

    static {
        BASE_CONTENT_URI = Uri.parse("content://" + SyncContentProvider.AUTHORITY);
        TIMESTAMPS_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(DbHelper.TABLE_TIMESTAMPS)
                .build();
        SYNC_DATA_URI = BASE_CONTENT_URI.buildUpon()
                .appendPath(DbHelper.TABLE_SYNC)
                .build();
    }





    private String mApp=null;
    private String mUser=null;
    private String mTeam=null;
    private String mUUID=null;
    private String mSyncMethod=null;
    private long mTimestamp_receive;


    private boolean USER_STOPPED_SYNC = false;
    private boolean LOCKED = true;

    private class StampedData {
        Object data;
        long timestamp;
        boolean hasMore;

        StampedData(Object data, long timestamp, boolean hasMore) {
            this.data = data;
            this.timestamp=timestamp;
            this.hasMore=hasMore;
        }
    }


    SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
        Log.d("sync", "SyncAdapter Constructor was called at "+ Tools.getCurrentTime());
        //Number of Rows of x Entries from others.
        MaxSyncableRowsServer = 10;
        //Number of SyncEntries per row.
        MaxSyncableEntriesClient = 100;
    }

    void init_session(Messenger client,
                      String team,
                      String user,
                      String app,
                      String uuid,
                      long timestamp_receive,
                      String syncMethod
    ) {

        Log.d("sync", "SYNC INIT was called at "+ Tools.getCurrentTime());
        mTeam = team;
        mUser = user;
        mApp = app;
        mUUID = uuid;
        mClient = client;
        mSyncMethod = syncMethod;
        //Log.d("sync","mTeam now: "+mTeam);
        mTimestamp_receive=timestamp_receive;
        USER_STOPPED_SYNC = false;
        LOCKED = false;
    }

    @Override
    public void onPerformSync(Account accounts, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {

        //Log.d("sync", "************onPerformSync [" + mUser + "]");

        if (LOCKED) {
            Log.e("sync", "Locked...exit");
            return;
        }

        else if (USER_STOPPED_SYNC) {
            Log.e("sync", "User has stopped the sync...exit");
            return;
        }

        LOCKED = true;
        boolean hasMoreToDo=false, hasDataToInsert=false;

        //Initiate sync, check that client is alive.
        try {
            mClient.send(Message.obtain(null, SyncService.MSG_SYNC_RUN_STARTED));
        } catch (RemoteException e) {
            e.printStackTrace();
            Log.d("sync","client not responding..exit");
            syncResult.hasHardError();
            return;
        }

        int errorCode = SyncService.NO_ERROR;

        //Set up a HTTP session with the server.
        //Send and receive data. Update timestamps.
        URLConnection conn=null;
        ObjectInputStream in=null;
        ObjectOutputStream out=null;

        try {
            //send data
            try {
                //get my Sync Entries
                StampedData dataOut = getMyUnsavedEntries();
                //establish session
                conn = getSyncConnection();
                //create pipe (-->server)
                out = new ObjectOutputStream(conn.getOutputStream());
                //header (username, uuid, team)
                sendHeader(out);
                //send data ([] of sync entries or EndofStream if empty)
                out.writeObject(dataOut.data);
                //timestamp -1 means that no data was sent
                if (dataOut.timestamp != -1) {
                    updateCounter(dataOut.timestamp, Constants.TIMESTAMP_SYNC_SEND);
                    hasMoreToDo = dataOut.hasMore;
                } else
                    Log.d("sync", "No data was sent.");

            } catch (SyncFailedException e) {
                Log.d("sync", "Sync failed exception during send");
                errorCode = SyncService.ERR_SYNC_ERROR;
                e.printStackTrace();
            } catch (IOException e) {
                Log.d("sync", "IO exception during send.");
                errorCode = SyncService.ERR_SEND_FAILED;
                e.printStackTrace();
            }
            //receive data if no error
            if (errorCode == SyncService.NO_ERROR) {
                Object reply;

                try {

                    //write timestamp of earliest entry to read.
                    out.writeObject(mTimestamp_receive);
                    //create pipe (Server-->me)
                    in = new ObjectInputStream(conn.getInputStream());

                    int numberOfEntriesFromServer = Integer.parseInt((String) in.readObject());

                    if (numberOfEntriesFromServer == 0) {
                        Log.d("sync", "No new data");
                    } else {
                        //receive data
                        ContentValues[] dataToInsert = new ContentValues[numberOfEntriesFromServer];
                        for (int i = 0; i < numberOfEntriesFromServer; i++) {
                            reply = in.readObject();
                            ContentValues cv = new ContentValues();
                            cv.put("data", (byte[]) reply);
                            dataToInsert[i] = cv;
                        }

                        //insert to temporary table
                        int rowsInserted = mContentResolver.bulkInsert(SYNC_DATA_URI,dataToInsert);
                        //Log.d("sync","Rows inserted: "+rowsInserted+" rows received: "+numberOfEntriesFromServer);
                        hasDataToInsert = rowsInserted>0;

                    }
                    //new timestamp = timestamp to read from next time.
                    Long newTimestamp = (Long) in.readObject();
                    //Log.d("sync", "Timestamp server --> me: " + newTimestamp);

                    updateCounter(newTimestamp,Constants.TIMESTAMP_SYNC_RECEIVE);
                    mTimestamp_receive=newTimestamp;
                    hasMoreToDo = hasMoreToDo || numberOfEntriesFromServer == MaxSyncableRowsServer;

                } catch (Exception e) {
                    e.printStackTrace();
                    errorCode = SyncService.ERR_SYNC_ERROR;
                }

            }

        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null)
                    in.close();
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

        if (hasMoreToDo) {
            //Log.d("sync","has more to send or read. Request new sync.");
            syncResult.fullSyncRequested = true;
        }

        //if any error, inform the client
        if (errorCode != SyncService.NO_ERROR) {
            Message errorMessage = Message.obtain(null, SyncService.MSG_SYNC_ERROR_STATE);
            try {
                mClient.send(errorMessage);
            } catch (RemoteException e) {
                e.printStackTrace();
                syncResult.hasHardError();
            }
        } else
            //Check if there are unprocessed rows in sync table.
            if (hasDataToInsert) {
                //Log.d("sync", "sync data exists to insert");
                try {
                    mClient.send(Message.obtain(null, SyncService.MSG_SYNC_DATA_READY_FOR_INSERT));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    mClient.send(Message.obtain(null, SyncService.MSG_SYNC_RUN_ENDED));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

        LOCKED = false;
    }


    private void updateCounter(long timestamp, String label) {
        ContentValues cv = new ContentValues();
        cv.put(DbHelper.Table_Timestamps.LABEL.name(), label);
        cv.put(DbHelper.Table_Timestamps.VALUE.name(), timestamp);
        cv.put(DbHelper.Table_Timestamps.SYNCGROUP.name(), mTeam);
        mContentResolver.insert(TIMESTAMPS_URI,cv);
    }

    private StampedData getMyUnsavedEntries() throws SyncFailedException {
        SyncEntry[] syncEntries=null;
        long maxStamp = -1; boolean hasMore=false;

        if (!"Read".equals(mSyncMethod)) {
            Cursor c = mContentResolver.query(BASE_CONTENT_URI, null, null, null, null);
            if (c == null)
                throw new SyncFailedException("ContentResolver null in getMyUnsavedEntries");
            //StringBuilder targets=new StringBuilder();
            long entryStamp;
            int rows, maxToSync, rowCount = 0;
            String action, changes, variable;
            rows = c.getCount();
            //Log.d("sync",rows+" rows to sync from ["+mUser+"] to ["+mTeam+"]");

            maxToSync = Math.min(c.getCount(), MaxSyncableEntriesClient);
            hasMore = maxToSync == MaxSyncableEntriesClient;
            syncEntries = new SyncEntry[maxToSync];
            while (c.moveToNext() && rowCount < maxToSync) {
                action = c.getString(c.getColumnIndex("action"));
                changes = c.getString(c.getColumnIndex("changes"));
                entryStamp = c.getLong(c.getColumnIndex("timestamp"));
                variable = c.getString(c.getColumnIndex("target"));
                //Keep track of the highest timestamp in the set!
                //Log.d("sync","variable: "+variable);
                if (entryStamp > maxStamp)
                    maxStamp = entryStamp;
                syncEntries[rowCount++] = new SyncEntry(SyncEntry.action(action), changes, entryStamp, variable, mUser);
                //targets.append(variable);
                //targets.append(",");
            }
            c.close();
        }
        if (syncEntries == null || syncEntries.length == 0 ) {
            Log.d("sync","no data , returning endofstream");
            return new StampedData(new EndOfStream(), maxStamp, false);
        } else {
            Log.d("sync","returning stamped entries: "+syncEntries.length);
            return new StampedData(syncEntries,maxStamp,hasMore);
        }


    }

    private URLConnection getSyncConnection() throws IOException {
        //Log.d("sync","getSyncConn called for "+uri);
        URL url ;
        URLConnection conn;
        url = new URL(Constants.SyncDataURI);
        conn = url.openConnection();
        conn.setConnectTimeout(10*1000);
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        return conn;
    }

    private void sendHeader(ObjectOutputStream out) throws IOException {
        //syncgroup
        out.writeObject(mTeam);
        //user
        out.writeObject(mUser);
        //user uuid
        out.writeObject(mUUID);
        //app name
        out.writeObject(mApp);

        //Log.d("sync","sent header ");
    }

    void userAbortedSync() {
        Log.d("sync","USER_STOPPED now true");
        USER_STOPPED_SYNC = true;
    }

    public static void forceSyncToHappen() {
        Log.d("sync","trying to force sync");
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(null, SyncContentProvider.AUTHORITY, bundle);
    }

}