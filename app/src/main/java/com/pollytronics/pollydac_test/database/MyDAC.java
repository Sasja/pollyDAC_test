package com.pollytronics.pollydac_test.database;

import android.content.Context;

import com.pollytronics.pollydac_test.pollydac.DAC;
import com.pollytronics.pollydac_test.pollydac.DbTable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by pollywog on 10/14/15.
 */
public class MyDAC extends DAC {
    private static final String DB_FILENAME = "asynctest.db";
    private static final int DB_VERSION_NR = 1;

    private static MyDAC instance = null;

    private DbNumberTable numberTable = new DbNumberTable("numbers", this);

    private MyDAC(Context context) {
        super(DB_FILENAME, DB_VERSION_NR);
        List<DbTable> allTables = new ArrayList<>();
        allTables.add(numberTable);
        initializeDatabase(context, allTables);
    }

    public static MyDAC get(Context context) {
        if (instance == null) instance = new MyDAC(context);
        return instance;
    }

    public DbNumberTable numbers() { return numberTable; }
}
