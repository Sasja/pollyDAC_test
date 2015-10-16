package com.pollytronics.pollydac_test.pollydac;

/**
 * Generic wrapper class around any object to interface with DbTable to store them in a SQLite db.
 *
 * Created by pollywog on 10/9/15.
 */
public class DbEntry<T> {

    private final T theObject;
    private final long _id;
    private final DbTable<T> dbTable;

    DbEntry(T e, long _id, DbTable<T> dbTable) {
        this.theObject = e;
        this._id = _id;
        this.dbTable = dbTable;
    }

    long _get_id() {
        return _id;
    }

    /**
     * Get the wrapped object
     * @return the wrapped object
     */
    public T getObject() {
        return theObject;
    }

    /**
     * Update the SQLite-entry to the current values of the wrapped object
     * This will simply call the update(DbEntry) method on table associated with this entry.
     */
    public void update() { dbTable.update(this); }
    /**
     * Remover the SQLite-entry from its associated table.
     * This will simply call the delete(DbEntry) method on table associated with this entry.
     */
    public void delete() { dbTable.delete(this); }

}
