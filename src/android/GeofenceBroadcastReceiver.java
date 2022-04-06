package com.cowbell.cordova.geofence;
//package com.google.android.gms.location.sample.geofencing;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receiver for geofence transition changes.
 * <p>
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a JobIntentService
 * that will handle the intent in the background.
 */
public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    /**
     * Receives incoming intents.
     *
     * @param context the application context.
     * @param intent  sent by Location Services. This Intent is provided to Location
     *                Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Enqueues a JobIntentService passing the context and intent as parameters
        GeofenceTransitionsJobIntentService.enqueueWork(context, intent);
    }
}
