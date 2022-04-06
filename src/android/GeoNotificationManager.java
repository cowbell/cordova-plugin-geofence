package com.cowbell.cordova.geofence;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CallbackContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class GeoNotificationManager implements OnCompleteListener<Void> {
    private Context context;
    private GeoNotificationStore geoNotificationStore;
    private GeofencingClient mGeofencingClient;
    private ArrayList<Geofence> mGeofenceList;
    private Logger logger;
    private List<Geofence> geoFences;
    private PendingIntent mGeofencePendingIntent;

    public GeoNotificationManager(Context context) {
        this.context = context;
        mGeofenceList = new ArrayList<>();
        geoNotificationStore = new GeoNotificationStore(context);
        logger = Logger.getLogger();
        mGeofencingClient = LocationServices.getGeofencingClient(context);
        if (areGoogleServicesAvailable()) {
            logger.log(Log.DEBUG, "Google play services available");
        } else {
            logger.log(Log.WARN, "Google play services not available. Geofence plugin will not work correctly.");
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    public void loadFromStorageAndInitializeGeofences() {
        List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
        geoFences = new ArrayList<Geofence>();
        for (GeoNotification geo : geoNotifications) {
            geoFences.add(geo.toGeofence());
        }
        if (!geoFences.isEmpty()) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

            // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
            // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
            // is already inside that geofence.
            builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

            // Add the geofences to be monitored by geofencing service.
            builder.addGeofences(geoFences);
            mGeofencingClient.addGeofences(builder.build(), getGeofencePendingIntent())
                    .addOnCompleteListener(this);
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

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(context, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        mGeofencePendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mGeofencePendingIntent;
    }

    public void addGeoNotifications(List<GeoNotification> geoNotifications,
                                    final CallbackContext callback) {
        for (GeoNotification geo : geoNotifications) {
            geoNotificationStore.setGeoNotification(geo);
            mGeofenceList.add(geo.toGeofence());
        }
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnCompleteListener(this);
    }

    public void removeGeoNotifications(List<String> ids, final CallbackContext callback) {
        mGeofencingClient.removeGeofences(ids).addOnCompleteListener(this);
        for (String id : ids) {
            geoNotificationStore.remove(id);
        }
    }

    public void removeAllGeoNotifications(final CallbackContext callback) {
        List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
        if(geoNotifications.size()>0) {
            List<String> geoNotificationsIds = new ArrayList<String>();
            for (GeoNotification geo : geoNotifications) {
                if (geo != null && geo.id != null) {
                    geoNotificationsIds.add(geo.id);
                }
            }
            if (geoNotificationsIds.size() > 0) {
                removeGeoNotifications(geoNotificationsIds, callback);
            }
            callback.success();
        } else {
            callback.success();
        }
    }

    @Override
    public void onComplete(@NonNull Task<Void> task) {
        int mPendingGeofenceTask = 2;
        if (task.isSuccessful()) {
           // Toast.makeText(context, "Geofences Added!", Toast.LENGTH_SHORT).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
           // String errorMessage = GeofenceErrorMessages.getErrorString(this, task.getException());
           // Log.w(TAG, errorMessage);
        }
    }
}