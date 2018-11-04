package com.cowbell.cordova.geofence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used as a substitution of the local storage in Android webviews
 *
 * @author Diane taken from
 *         https://github.com/didimoo/AndroidLocalStorage/blob/master
 *         /src/com/example/androidlocalstorage/MainFragment.java
 */
public class LocalStorage {
    private Context mContext;
    private LocalStorageDBHelper localStorageDBHelper;
    private SQLiteDatabase database;

    public LocalStorage(Context c) {
        mContext = c;
        localStorageDBHelper = LocalStorageDBHelper.getInstance(mContext);
    }

    public List<String> getAllItems() {
        ArrayList<String> results = new ArrayList<String>();
        database = localStorageDBHelper.getReadableDatabase();
        Cursor cursor = database.query(
                LocalStorageDBHelper.LOCALSTORAGE_TABLE_NAME, null, null, null,
                null, null, null);
        while (cursor.moveToNext()) {
            results.add(cursor.getString(1));
        }
        cursor.close();
        return results;
    }

    /**
     * This method allows to get an item for the given key
     * 
     * @param key
     *            : the key to look for in the local storage
     * @return the item having the given key
     */
    public String getItem(String key) {
        String value = null;
        if (key != null) {
            database = localStorageDBHelper.getReadableDatabase();
            Cursor cursor = database.query(
                    LocalStorageDBHelper.LOCALSTORAGE_TABLE_NAME, null,
                    LocalStorageDBHelper.LOCALSTORAGE_ID + " = ?",
                    new String[] { key }, null, null, null);
            if (cursor.moveToFirst()) {
                value = cursor.getString(1);
            }
            cursor.close();
        }
        return value;
    }

    /**
     * set the value for the given key, or create the set of datas if the key
     * does not exist already.
     * 
     * @param key
     * @param value
     */
    public void setItem(String key, String value) {
        if (key != null && value != null) {
            String oldValue = getItem(key);
            database = localStorageDBHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(LocalStorageDBHelper.LOCALSTORAGE_ID, key);
            values.put(LocalStorageDBHelper.LOCALSTORAGE_VALUE, value);
            if (oldValue != null) {
                database.update(LocalStorageDBHelper.LOCALSTORAGE_TABLE_NAME,
                        values, LocalStorageDBHelper.LOCALSTORAGE_ID + "='"
                                + key + "'", null);
            } else {
                database.insert(LocalStorageDBHelper.LOCALSTORAGE_TABLE_NAME,
                        null, values);
            }
        }
    }

    /**
     * removes the item corresponding to the given key
     * 
     * @param key
     */
    public void removeItem(String key) {
        if (key != null) {
            database = localStorageDBHelper.getWritableDatabase();
            database.delete(LocalStorageDBHelper.LOCALSTORAGE_TABLE_NAME,
                    LocalStorageDBHelper.LOCALSTORAGE_ID + "='" + key + "'",
                    null);
        }
    }

    /**
     * clears all the local storage.
     */
    public void clear() {
        database = localStorageDBHelper.getWritableDatabase();
        database.delete(LocalStorageDBHelper.LOCALSTORAGE_TABLE_NAME, null,
                null);
    }
}