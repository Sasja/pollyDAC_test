package com.pollytronics.pollydac_test.pollydac;

import android.content.Context;
import android.database.ContentObservable;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Database Access Object class that handles all SQLite stuff and provides threadsafety.
 * You should only ever instantiate one object of this class.
 * After instantiation initialize with initializeDatabase()
 * It's a good approach to perform this in the constructor of a derived class that
 * implements the singleton pattern. It's also a good idea for this derived class to provide
 * getters for those tables:
 *      so that eg. {@code MyDAC.cats()} : returns the cat-table.
 *      then eg. {@code MyDAC.cats().insert(new DbEntry<Cat>(darwin))} to store Darwin into the cat-table.
 *
 * Created by pollywog on 9/26/15.
 */
public class DAC extends ContentObservable {
    private static final String TAG = "DAC";
    private static boolean singletonGuard_isInstanciated = false;

    private SQLiteDatabase db = null;
    private DbHelper dbHelper = null;
    private final ReentrantLock dbLock = new ReentrantLock();
    private String dbFileName;
    private int dbVersionNr;

    /**
     * The standard constructor.
     * Throws an Error when more than one objects are instanciated to enforce the singleton pattern.
     */
    protected DAC(String dbFileName, int dbVersionNr) {
        this.dbFileName = dbFileName;
        this.dbVersionNr = dbVersionNr;
        if (singletonGuard_isInstanciated)
            throw new Error("creation of second instance of DAC detected, use singleton pattern");
        singletonGuard_isInstanciated = true;
    }

    /**
     * This method needs to be called before accessing the database.
     * A good approach is to call it in the constructor of a derived class.
     * @param context
     */
    protected void initializeDatabase(Context context, List<DbTable> tables) {
        if(dbHelper != null) throw new Error("DAC object has allready been initialized");
        dbHelper = new DbHelper(context, tables, dbFileName, dbVersionNr);
    }

    @Override
    /**
     * This overridden destructor throws an Error when the DAC is destroyed while the lock is not
     * released.
     */
    protected void finalize() throws Throwable {
        if(dbLock.isHeldByCurrentThread()) throw new Error("GC is destroying a DAC instance that is holding an unreleased lock!");
        super.finalize();
    }

    /**
     * Keeps the database locked and its connection open until the next call to close().
     * This allows sequencing multiple database operations without the possibility of another thread
     * interfering. This can also substantially speed up sequential operations.
     * It is not needed however for each db operations as each operation will lock the db itself.
     * Forgetting to call close() or an improperly caught exception can lock the db down...
     */
    public void keepOpen() {
        lockAndGetDb();
    }

    /**
     * Release the database lock and disconnect. So other threads can proceed to connect.
     * Use this with a try finally statement to prevent a deadlock on an exception.
     */
    public void close() {
        unlockAndReleaseDb();
    }


    SQLiteDatabase lockAndGetDb() {
        if(dbHelper == null) throw new Error("you must call initializeDatabase() before use");
        dbLock.lock();
        if(dbLock.getHoldCount() == 1) {
            Log.i(TAG, "getting writable database");
            db = dbHelper.getWritableDatabase();
        }
        if(dbLock.getHoldCount() > 2) {
            throw new Error("dao.dbLock.getHoldCount() > 2, forgot to release a lock?");
        }
        return db;
    }

    void unlockAndReleaseDb() {
        if(dbLock.getHoldCount() == 1) {
            Log.i(TAG, "releasing writable database");
            dbHelper.close();
        }
        dbLock.unlock();
    }
}
