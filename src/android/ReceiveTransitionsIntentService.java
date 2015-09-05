package com.cowbell.cordova.geofence;

import android.app.IntentService;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class ReceiveTransitionsIntentService extends IntentService {

    private static final String circuitLocationEndpoint = "http://circuit-2015-services-p.elasticbeanstalk.com/devices/%s/locationEvents";

    protected GeoNotificationStore store;
    private String deviceId;

    /**
     * Sets an identifier for the service
     */
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
        store = new GeoNotificationStore(this);
        Logger.setLogger(new Logger(GeofencePlugin.TAG, this, false));
    }

    /**
     * Handles incoming intents
     *
     * @param intent The Intent sent by Location Services. This Intent is provided
     *               to Location Services (inside a PendingIntent) when you call
     *               addGeofences()
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Logger logger = Logger.getLogger();
        logger.log(Log.DEBUG, "ReceiveTransitionsIntentService - onHandleIntent");
        // First check for errors
        if (LocationClient.hasError(intent)) {
            // Get the error code with a static method
            int errorCode = LocationClient.getErrorCode(intent);
            // Log the error
            logger.log(Log.ERROR,
                    "Location Services error: " + Integer.toString(errorCode));
            /*
             * You can also send the error code to an Activity or Fragment with
             * a broadcast Intent
             */
            /*
             * If there's no error, get the transition type and the IDs of the
             * geofence or geofences that triggered the transition
             */
        } else {
            String deviceId = intent.getStringExtra("com.cowbell.cordova.geofence.DEVICEID_EXTRA");
            Log.d(GeofencePlugin.TAG, "The device ID: " + deviceId);

            long now = new Date().getTime();
            // Get the type of transition (entry or exit)
            int transitionType = LocationClient.getGeofenceTransition(intent);
            if ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
                    || transitionType == Geofence.GEOFENCE_TRANSITION_EXIT
                    || transitionType == Geofence.GEOFENCE_TRANSITION_DWELL) {
                logger.log(Log.DEBUG, "Geofence transition detected");
                List<Geofence> triggerList = LocationClient
                        .getTriggeringGeofences(intent);

                for (Geofence fence : triggerList) {
                    CircuitLocationEvent locationEvent = new CircuitLocationEvent(fence.getRequestId(), transitionType, now);
                    try {
                        HttpRequests.postJson(String.format(circuitLocationEndpoint, deviceId), locationEvent.toJson());
                    } catch (IOException e) {
                        Log.e(GeofencePlugin.TAG, "IOException occurred while trying to POST to endpoint with message: " + e.getMessage());
                    }
                }
            }
        }
    }
}
