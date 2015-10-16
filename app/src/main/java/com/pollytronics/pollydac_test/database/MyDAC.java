package com.pollytronics.pollydac_test.database;

import android.content.Context;

import com.pollytronics.pollydac_test.pollydac.DAC;

/**
 * Created by pollywog on 10/14/15.
 */
public class MyDAC extends DAC {
    private static final String DB_FILENAME = "asynctest.db";
    private static final int DB_VERSION_NR = 1;

    private static MyDAC instance = null;

    private DbNumberTable numberTable;

    private MyDAC(Context context) {
        registerAllTables();
        initializeDatabase(context, DB_FILENAME, DB_VERSION_NR);
    }

    public static MyDAC get(Context context) {
        if (instance == null) instance = new MyDAC(context);
        return instance;
    }

    protected void registerAllTables() {
        numberTable = new DbNumberTable("numbers", this);
        registerTable(numberTable);
    }

    public DbNumberTable numbers() { return numberTable; }
}
