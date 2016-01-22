package com.cowbell.cordova.geofence;

import android.util.Log;

import android.content.BroadcastReceiver;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.cowbell.cordova.geofence.Gson;
import com.cowbell.cordova.geofence.GeoNotification;
import com.google.android.gms.location.Geofence;

public class TransitionReceiver extends BroadcastReceiver {
    protected GeoNotificationNotifier notifier;
    protected GeoNotificationStore store;

    @Override
    public void onReceive(Context context, Intent intent) {
        String error = intent.getStringExtra("error");
        Logger logger = Logger.getLogger();

        if (error != null) {
            logger.log(Log.ERROR, error);
        } else {
            String geofencesJson = intent.getStringExtra("transitionData");

        }
    }
}
