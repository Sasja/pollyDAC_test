package com.pollytronics.pollydac_test.pollydac;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic abstract base Class for database tables.
 *
 * Extend this class in order to associate a database table to an object type.
 * Then register an instance with the DAC object before initializing it.
 * This class provides basic CRUD and can optionally be extended for more advanced operations.
 * All database operations lock up the database using the DAC associated with it. Keep this
 * in mind when extending this class with custom methods.
 *
 * Created by pollywog on 10/9/15.
 */
public abstract class DbTable<E> {
    private final String tableName;
    private final DAC dao;
    private final List<String> columnNamesInt = new ArrayList<>();
    private final List<String> columnNamesReal = new ArrayList<>();
    private final List<String> columnNamesText = new ArrayList<>();

    /**
     * Implement this method to derive contentvalues from an object in order to store it in the table.
     * The contentvalues should contain the value and the full table collumn name.
     * Row-id's should not be handled manually but left to this package.
     * @param e an object (no shit)
     * @return contentvalues representing the input object.
     */
    abstract protected ContentValues getContentValues(E e);

    /**
     * Implement this method to create a new E object from a cursor.
     * It should look for the column names and use the correstponding values to create a new E object.
     * Row-id's should not be handled manually but left to this package.
     * @param cursor a cursor that is pointing to the row to be read.
     * @return a new object of class E with the values derived from the cursor
     */
    abstract protected E fromCursor(Cursor cursor);

    /**
     * Implement this method to define all the columns that need to be present in the table.
     * Use the following methods to do so:
     *      defineColumnInt(String columnName)
     *      defineColumnDouble(String columnName)
     *      defineColumnString(String columnName)
     */
    abstract protected void defineColumns();

    /**
     * Defines an integer-column in the table, only use this method from within defineColumns().
     * This corresponds to an "integer" entry in the SQLite db.
     * @param columnName name of the column
     */
    protected void defineColumnInt(String columnName)    { columnNamesInt.add(columnName); }

    /**
     * Defines an double-column in the table, only use this method from within defineColumns().
     * This corresponds to a "real" entry in the SQLite db.
     * @param columnName name of the column
     */
    protected void defineColumnDouble(String columnName) { columnNamesReal.add(columnName); }

    /**
     * Defines an string-column in the table, only use this method from within defineColumns().
     * This corresponds to an "text" entry in the SQLite db.
     * @param columnName name of the column
     */
    protected void defineColumnString(String columnName) { columnNamesText.add(columnName); }

    private DbEntry<E> dbEntryFromCursor(Cursor cursor) {
        E e = fromCursor(cursor);
        long _id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID));
        return new DbEntry<>(e, _id, this);
    }

    /**
     * Creator, that really doesn't do anything besides storing the tableName and a reference to its dao.
     * @param tableName
     * @param dao
     */
    protected DbTable(String tableName, DAC dao) {
        this.dao = dao;
        this.tableName = tableName;
    }

    /**
     * Gets all the rows and returns them as DbEntry's that contain the corresponding objects.
     * @return
     */
    public List<DbEntry<E>> getAllEntries() {
        SQLiteDatabase db = dao.lockAndGetDb();
        Cursor cur = db.query(
                tableName,
                null, null, null, null, null, null
        );
        List<DbEntry<E>> result = new ArrayList<>();
        while (cur.moveToNext()) {
            result.add(dbEntryFromCursor(cur));
        }
        cur.close();
        dao.unlockAndReleaseDb();
        return result;
    }

    /**
     * Gets all the rows and returns them as objects.
     * If you need update the objects in the db or refer to their table-entries in general,
     * then use getAllEntries() instead.
     * @return
     */
    public List<E> getAllObjects() {
        List<E> allObjects = new ArrayList<>();
        for (DbEntry<E> entry : getAllEntries()) {
            allObjects.add(entry.getObject());
        }
        return allObjects;
    }

    /**
     * Stores an object of type E in the table.
     * @param e the object to be stored
     * @return a table entry object containing the e object itself.
     */
    public DbEntry<E> insert(E e) {
        SQLiteDatabase db = dao.lockAndGetDb();
        long _id = db.insert(
                tableName,
                null,
                getContentValues(e)
        );
        dao.unlockAndReleaseDb();
        return new DbEntry<>(e, _id, this);
    }

    /**
     * Updates the database entry to the values of the object contained in the passed DbEntry.
     * @param dbEntry an existing table entry containing an object with values than need to be stored.
     * @return the input so calls can be chained.
     */
    public DbEntry<E> update(DbEntry<E> dbEntry) {
        SQLiteDatabase db = dao.lockAndGetDb();
        db.update(
                tableName,
                getContentValues(dbEntry.getObject()),
                BaseColumns._ID + " = " + dbEntry._get_id(),
                null
        );
        dao.unlockAndReleaseDb();
        return dbEntry;
    }

    /**
     * Remove the entry from the database
     * @param dbe the entry to be removed
     */
    public void delete(DbEntry<E> dbe) {
        SQLiteDatabase db = dao.lockAndGetDb();
        db.delete(
                tableName,
                BaseColumns._ID + " = " + dbe._get_id(),
                null
        );
        dao.unlockAndReleaseDb();
    }

    void createTable(SQLiteDatabase db) {
        defineColumns();
        String sql = "create table " + tableName + " (" +
                BaseColumns._ID + " integer primary key";
        for (String c : columnNamesInt) sql += ", " + c + " integer";
        for (String c : columnNamesReal) sql += ", " + c + " real";
        for (String c : columnNamesText) sql += ", " + c + " text";
        sql += ")";
        db.execSQL(sql);
    }

    void dropTable(SQLiteDatabase db) {
        db.execSQL("drop table if exists " + tableName);
    }
}
