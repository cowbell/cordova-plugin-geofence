package com.tsubik.cordova.geofence;
 
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.Context;
 
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
    	GeoNotificationManager manager = new GeoNotificationManager(context);
    	manager.loadFromStorageAndInitializeGeofences();
    }
}