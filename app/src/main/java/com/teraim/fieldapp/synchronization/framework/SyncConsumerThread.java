package com.teraim.fieldapp.synchronization.framework;

import android.content.Intent;
import android.database.Cursor;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.teraim.fieldapp.GlobalState;
import com.teraim.fieldapp.dynamic.Executor;
import com.teraim.fieldapp.synchronization.SyncEntry;
import com.teraim.fieldapp.synchronization.SyncReport;
import com.teraim.fieldapp.utils.DbHelper;
import com.teraim.fieldapp.utils.Tools;

public class SyncConsumerThread extends Thread {

    private final Handler mCaller;

    public SyncConsumerThread(Handler caller) {
        mCaller = caller;
    }

    @Override
    public void run() {
        GlobalState gs = GlobalState.getInstance();
        if (gs !=null) {
            DbHelper dbHelper = gs.getDb();
            Cursor c = dbHelper.getSyncDataCursor();
            byte[] row;
            int id=-1,nSyncEntriesTotal=0;
            SyncReport syncReport = new SyncReport();
            while (gs !=null && c.moveToNext()) {
                row = c.getBlob(1);
                id = c.getInt(0);
                if (row != null) {
                    Object o = Tools.bytesToObject(row);
                    if (o != null) {
                        SyncEntry[] ses = (SyncEntry[]) o;
                        nSyncEntriesTotal += ses.length;
                        dbHelper.insertSyncEntries(syncReport, ses, gs.getLogger());
                        syncReport.currentRow++;
                        Log.d("sync","map now has "+syncReport.getTimeStampedMap().size() +" entries");
                    } else {
                        Log.e("sync", "Corrupted row in sync data");
                    }
                }
            }
            c.close();
            if (id!=-1) {
                dbHelper.insertIfMax(syncReport);
                Log.d("sync", "Deleting entries in table_sync with id less than or equal to " + id);
                dbHelper.deleteConsumedSyncEntries(id);
                //send a message to refresh any ui currently drawn


            } else
                Log.e("sync","No changes in syncreport, not calling insertifmax");
           // mCaller.handleMessage(Message.obtain(null, SyncService.MSG_SYNC_DATA_CONSUMED));
        }
        Intent intent = new Intent();
        intent.setAction(Executor.REDRAW_PAGE);
        if (gs!=null)
            gs.sendSyncEvent(intent);
        mCaller.handleMessage(Message.obtain(null, SyncService.MSG_SYNC_RUN_ENDED));

    }
};


