package com.cowbell.cordova.geofence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * {@link SQLiteOpenHelper} that is used as replacement of the localStorage of
 * the webviews.
 * 
 * @details this class should not be used. Everything about the localStorage
 *          through the application is already handled in HTMLFragment.
 * @author Diane taken from
 *         https://github.com/didimoo/AndroidLocalStorage/blob/master
 *         /src/com/example/androidlocalstorage/LocalStorage.java
 */

public class LocalStorageDBHelper extends SQLiteOpenHelper {

    private static LocalStorageDBHelper mInstance;

    /**
     * the name of the table
     */
    public static final String LOCALSTORAGE_TABLE_NAME = "geonotifications";

    /**
     * the id column of the table LOCALSTORAGE_TABLE_NAME
     */
    public static final String LOCALSTORAGE_ID = "_id";

    /**
     * the value column of the table LOCALSTORAGE_TABLE_NAME
     */
    public static final String LOCALSTORAGE_VALUE = "value";

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "geonotifications.db";
    private static final String DICTIONARY_TABLE_CREATE = "CREATE TABLE "
            + LOCALSTORAGE_TABLE_NAME + " (" + LOCALSTORAGE_ID
            + " TEXT PRIMARY KEY, " + LOCALSTORAGE_VALUE + " TEXT NOT NULL);";

    /**
     * Returns an instance of LocalStorage
     * 
     * @param ctx
     *            : a Context used to create the database
     * @return the instance of LocalStorage of the application or a new one if
     *         it has not been created before.
     */
    public static LocalStorageDBHelper getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new LocalStorageDBHelper(ctx);
        }
        return mInstance;
    }

    private LocalStorageDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DICTIONARY_TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(LocalStorageDBHelper.class.getName(),
                "Upgrading database from version " + oldVersion + " to "
                        + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + LOCALSTORAGE_TABLE_NAME);
        onCreate(db);
    }
}