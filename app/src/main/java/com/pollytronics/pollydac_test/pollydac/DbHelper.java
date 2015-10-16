package com.pollytronics.pollydac_test.pollydac;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.List;

class DbHelper extends SQLiteOpenHelper {
    private static final String TAG = "DbHelper";
    private final List<DbTable> allTables;

    DbHelper(Context context, List<DbTable> allTables, String dbFileName, int dbVersionNr) {
        super(context, dbFileName, null, dbVersionNr);
        this.allTables = allTables;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.i(TAG, "onCreate(), creating all tables");
        for (DbTable dbt : allTables) { dbt.createTable(db); }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.i(TAG, "onUpgrade(), dropping all tables and starting over");
        for (DbTable dbt : allTables) { dbt.dropTable(db); }
        onCreate(db);
    }
}
