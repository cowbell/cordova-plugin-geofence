package com.cowbell.cordova.geofence;

import com.google.android.gms.location.Geofence;
import com.google.gson.annotations.Expose;

import android.util.Log;

public class GeoNotification {
    @Expose public String id;
    @Expose public double latitude;
    @Expose public double longitude;
    @Expose public int radius;
    @Expose public int transitionType;

    @Expose public Notification notification;
    @Expose public Period period;

    public GeoNotification() {
    }

    public Geofence toGeofence() {
        return new Geofence.Builder().setRequestId(id)
        .setTransitionTypes(transitionType)
        .setCircularRegion(latitude, longitude, radius)
        .setExpirationDuration(Long.MAX_VALUE).build();
    }

    public String toJson() {
        return Gson.get().toJson(this);
    }

    public static GeoNotification fromJson(String json) {
        Logger logger = Logger.getLogger();
        logger.log(Log.DEBUG, "GeoNotificatoin#fromJson: json = " + json);

        if (json == null)
            return null;
        return Gson.get().fromJson(json, GeoNotification.class);
    }
}
