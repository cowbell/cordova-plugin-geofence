package com.cowbell.cordova.geofence;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

public class Logger {
    protected String TAG;
    protected Context context;
    protected boolean isToastEnabled;

    public Logger(String TAG, Context context, boolean isToastEnabled) {
        this.TAG = TAG;
        this.context = context;
        this.isToastEnabled = isToastEnabled;
    }

    public void log(int priority, String message) {
        Log.println(priority, TAG, message);
        showOnToastIfEnabled(message);
    }

    public void log(String message, Throwable exception) {
        Log.e(TAG, message, exception);
        showOnToastIfEnabled(message);
    }

    public void showOnToastIfEnabled(String message) {
        if (isToastEnabled) {
            Toast.makeText(context, message, 2000).show();
        }
    }

    private static Logger logger = null;
    private static Object mutex = new Object();

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        synchronized (mutex) {
            Logger.logger = logger;
        }
    }
}
