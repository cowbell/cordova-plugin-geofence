package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofencingRequest;

import org.apache.cordova.CallbackContext;

import java.util.ArrayList;
import java.util.List;

public class GeoNotificationManager {
    private Context context;
    private GeoNotificationStore geoNotificationStore;
    private Logger logger;
    private boolean connectionInProgress = false;
    private PendingIntent pendingIntent;
    private GoogleServiceCommandExecutor googleServiceCommandExecutor;

    public GeoNotificationManager(Context context) {
        this.context = context;
        geoNotificationStore = new GeoNotificationStore(context);
        logger = Logger.getLogger();
        googleServiceCommandExecutor = new GoogleServiceCommandExecutor(context);
        pendingIntent = getTransitionPendingIntent();
        if (areGoogleServicesAvailable()) {
            logger.log(Log.DEBUG, "Google play services available");
        } else {
            logger.log(Log.WARN, "Google play services not available. Geofence plugin will not work correctly.");
        }
    }

    public void loadFromStorageAndInitializeGeofences() {
        List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
        List<GeofencingRequest> geoFenceRequestList = new ArrayList<GeofencingRequest>();

        for (GeoNotification geo : geoNotifications) {
            googleServiceCommandExecutor.QueueToExecute(new AddGeofenceCommand(
                    context, pendingIntent, geo.toGeofenceRequest()));
        }
    }

    public List<GeoNotification> getWatched() {
        List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
        return geoNotifications;
    }

    private boolean areGoogleServicesAvailable() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            return false;
        }
    }

    public void addGeoNotification(final GeoNotification geoNotification,
            final CallbackContext callback) {
        GeofencingRequest geofencingRequest = geoNotification.toGeofenceRequest();
        AddGeofenceCommand geoFenceCmd = new AddGeofenceCommand(context, pendingIntent, geofencingRequest);

        if (callback != null) {
            geoFenceCmd.addListener(new IGoogleServiceCommandListener() {
                public void onCommandExecuted(CommandStatus status) {
                    if (status.isSuccess()) {
                        geoNotificationStore.setGeoNotification(geoNotification);
                        callback.success();
                    } else {
                        callback.error(status.getMessage());
                    }
                }
            });
        }

        googleServiceCommandExecutor.QueueToExecute(geoFenceCmd);
    }

    public void removeGeoNotification(String id, final CallbackContext callback) {
        List<String> ids = new ArrayList<String>();
        ids.add(id);
        removeGeoNotifications(ids, callback);
    }

    public void removeGeoNotifications(final List<String> ids,
            final CallbackContext callback) {
        RemoveGeofenceCommand cmd = new RemoveGeofenceCommand(context, ids);
        if (callback != null) {
            cmd.addListener(new IGoogleServiceCommandListener() {
                public void onCommandExecuted(CommandStatus status) {
                    if (status.isSuccess()) {
                        for (String id : ids) {
                            geoNotificationStore.remove(id);
                        }
                        callback.success();
                    } else {
                        callback.error(status.getMessage());
                    }
                }
            });
        }

        googleServiceCommandExecutor.QueueToExecute(cmd);
    }

    public void removeAllGeoNotifications(final CallbackContext callback) {
        List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
        List<String> geoNotificationsIds = new ArrayList<String>();
        for (GeoNotification geo : geoNotifications) {
            geoNotificationsIds.add(geo.id);
        }
        removeGeoNotifications(geoNotificationsIds, callback);
    }

    /*
     * Create a PendingIntent that triggers an IntentService in your app when a
     * geofence transition occurs.
     */
    private PendingIntent getTransitionPendingIntent() {
        // Create an explicit Intent

        Intent intent = new Intent(context,
                ReceiveTransitionsIntentService.class);
        /*
         * Return the PendingIntent
         */
        logger.log(Log.DEBUG, "Geofence Intent created!");
        return PendingIntent.getService(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
