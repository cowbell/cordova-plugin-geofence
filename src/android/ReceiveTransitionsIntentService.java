package com.cowbell.cordova.geofence;

import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;

public class ReceiveTransitionsIntentService extends IntentService {
    protected BeepHelper beepHelper;
    protected GeoNotificationNotifier notifier;
    protected GeoNotificationStore store;

    /**
     * Sets an identifier for the service
     */
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
        beepHelper = new BeepHelper();
        store = new GeoNotificationStore(this);
        Logger.setLogger(new Logger(GeofencePlugin.TAG, this, false));
    }

    /**
     * Handles incoming intents
     *
     * @param intent
     *            The Intent sent by Location Services. This Intent is provided
     *            to Location Services (inside a PendingIntent) when you call
     *            addGeofences()
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        notifier = new GeoNotificationNotifier(
                (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE),
                this
        );

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
            // Get the type of transition (entry or exit)
            int transitionType = LocationClient.getGeofenceTransition(intent);
            if ((transitionType == Geofence.GEOFENCE_TRANSITION_ENTER)
                    || (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT)) {
                logger.log(Log.DEBUG, "Geofence transition detected");
                List<Geofence> triggerList = LocationClient
                        .getTriggeringGeofences(intent);
                List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
		Calendar now = Calendar.getInstance();

                for (Geofence fence : triggerList) {
                    String fenceId = fence.getRequestId();
                    GeoNotification geoNotification = store
                            .getGeoNotification(fenceId);

                    if ((geoNotification == null)
			|| (geoNotification.notification == null) 
			|| (geoNotification.period == null)) {
			continue;
		    }

		    if (geoNotification.period.isFiredInCurrentPeriod(now)
			== true) {
			logger.log(Log.ERROR, "check-1");
			notifier.notify(geoNotification.notification);
			logger.log(Log.ERROR, "check-2");
		    }
		    logger.log(Log.ERROR, "check-3");
		    geoNotifications.add(geoNotification);
		    logger.log(Log.ERROR, "check-4");
		}

                if (geoNotifications.size() > 0) {
		    List<GeoNotification> reregistrations = 
			    new ArrayList<GeoNotification>();
                    GeofencePlugin.onTransitionReceived(geoNotifications);
                    GeofencePlugin.registar(geoNotifications);
                }
            } else {
                logger.log(Log.ERROR, "Geofence transition error: "
                        + transitionType);
            }
        }
    }
}
