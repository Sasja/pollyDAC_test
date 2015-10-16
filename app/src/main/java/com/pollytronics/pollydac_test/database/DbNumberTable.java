package com.pollytronics.pollydac_test.database;

import android.content.ContentValues;
import android.database.Cursor;

import com.pollytronics.pollydac_test.MyNumber;
import com.pollytronics.pollydac_test.pollydac.DAC;
import com.pollytronics.pollydac_test.pollydac.DbTable;

/**
 * Created by pollywog on 10/9/15.
 */
public class DbNumberTable extends DbTable<MyNumber> {
    public static final String COLUMN_VALUE = "value";

    DbNumberTable(String tableName, DAC dao) {
        super(tableName, dao);
    }

    @Override
    protected void defineColumns() {
        defineColumnInt(COLUMN_VALUE);
    }

    @Override
    protected ContentValues getContentValues(MyNumber myNumber) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_VALUE, myNumber.getValue());
        return values;
    }

    @Override
    protected MyNumber fromCursor(Cursor cursor) {
        int value = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VALUE));
        return new MyNumber(value);
    }
}
