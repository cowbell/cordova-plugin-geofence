package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.Geofence;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import java.util.ArrayList;
import java.util.List;

public class GeoNotificationManager {
    private Context context;
    private GeoNotificationStore geoNotificationStore;
    private Logger logger;
    private List<Geofence> geoFences;
    private PendingIntent pendingIntent;
    private GoogleServiceCommandExecutor googleServiceCommandExecutor;

    public GeoNotificationManager(Context context) {
        this.context = context;
        geoNotificationStore = new GeoNotificationStore(context);
        logger = Logger.getLogger();
        googleServiceCommandExecutor = new GoogleServiceCommandExecutor();
        pendingIntent = getTransitionPendingIntent();
        if (areGoogleServicesAvailable()) {
            logger.log(Log.DEBUG, "Google play services available");
        } else {
            logger.log(Log.WARN, "Google play services not available. Geofence plugin will not work correctly.");
        }
    }

    public void loadFromStorageAndInitializeGeofences() {
        List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
        geoFences = new ArrayList<Geofence>();
        for (GeoNotification geo : geoNotifications) {
            geoFences.add(geo.toGeofence());
        }
        if (!geoFences.isEmpty()) {
            googleServiceCommandExecutor.QueueToExecute(
                new AddGeofenceCommand(context, pendingIntent, geoFences)
            );
        }
    }

    public List<GeoNotification> getWatched() {
        List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
        return geoNotifications;
    }

    private boolean areGoogleServicesAvailable() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int resultCode = api.isGooglePlayServicesAvailable(context);

        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            return false;
        }
    }

    public void addGeoNotifications(List<GeoNotification> geoNotifications,
                                    final CallbackContext callback) {
        List<Geofence> newGeofences = new ArrayList<Geofence>();
        for (GeoNotification geo : geoNotifications) {
            geoNotificationStore.setGeoNotification(geo);
            newGeofences.add(geo.toGeofence());
        }
        AddGeofenceCommand geoFenceCmd = new AddGeofenceCommand(
            context,
            pendingIntent,
            newGeofences
        );
        if (callback != null) {
            geoFenceCmd.addListener(new CommandExecutionHandler(callback));
        }
        googleServiceCommandExecutor.QueueToExecute(geoFenceCmd);
    }

    public void removeGeoNotifications(List<String> ids, final CallbackContext callback) {
        RemoveGeofenceCommand cmd = new RemoveGeofenceCommand(context, ids);
        if (callback != null) {
            cmd.addListener(new CommandExecutionHandler(callback));
        }
        for (String id : ids) {
            geoNotificationStore.remove(id);
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
        Intent intent = new Intent(context, ReceiveTransitionsIntentService.class);
        logger.log(Log.DEBUG, "Geofence Intent created!");
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

}
