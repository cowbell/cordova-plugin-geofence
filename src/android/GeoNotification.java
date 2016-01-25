package com.cowbell.cordova.geofence;

import com.google.android.gms.location.Geofence;
import com.google.gson.annotations.Expose;

import java.util.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class GeoNotification {
    @Expose public String id;
    @Expose public double latitude;
    @Expose public double longitude;
    @Expose public int radius;
    @Expose public int transitionType;
    @Expose public String endDate;
    @Expose public long lastShown = 0;
    @Expose public int frequency = 0;
    @Expose public int loiteringDelay = 120000;

    @Expose public Notification notification;

    public GeoNotification() {
    }

    public Geofence toGeofence() {
        return new Geofence.Builder().setRequestId(id)
                .setTransitionTypes(transitionType)
                .setExpirationDuration(getExpireTime())
                .setLoiteringDelay(loiteringDelay)
                .setCircularRegion(latitude, longitude, radius)
                .build();
    }

    public String toJson() {
        return Gson.get().toJson(this);
    }

    public long getExpireTime() {
        if(endDate == null || endDate.isEmpty() || endDate == "false") {
            return Geofence.NEVER_EXPIRE;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date();

        try {
            date = sdf.parse(endDate);
        }catch(ParseException ex) {
            return Geofence.NEVER_EXPIRE;
        }

        Date now = new Date();
        long dateMill = date.getTime();
        long nowMill = now.getTime();

        long diff = dateMill - nowMill;

        if(diff < 0) {
            return 0;
        }else {
            return diff;
        }
    }

    public static GeoNotification fromJson(String json) {
        if (json == null)
            return null;
        return Gson.get().fromJson(json, GeoNotification.class);
    }
}
