package com.teraim.fieldapp.synchronization;

public class SyncTimestamp {
    private long time;
    private int seq_no;

    public SyncTimestamp(long time, int seq_no) {
        this.time = time;
        this.seq_no = seq_no;
    }

    public long getTime() {
        return time;
    }

    public int getSequence() {
        return seq_no;
    }
}
