package com.teraim.fieldapp.utils;

final class DbSchema {
    private DbSchema() {
    }

    static final int DATABASE_VERSION = 18;

    static final String TABLE_VARIABLES = "variabler";
    static final String TABLE_TIMESTAMPS = "timestamps";
    static final String TABLE_AUDIT = "audit";
    static final String TABLE_SYNC = "sync";

    static final String COL_VARID = "var";
    static final String COL_VALUE = "value";
    static final String COL_TIMESTAMP = "timestamp";
    static final String COL_LAG = "lag";
    static final String COL_AUTHOR = "author";
    static final String COL_YEAR = "år";

    static final String[] VAR_COLS = new String[]{
            COL_TIMESTAMP,
            COL_AUTHOR,
            COL_LAG,
            COL_VALUE
    };
}
