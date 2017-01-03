package com.cowbell.cordova.geofence;

import com.google.android.gms.location.Geofence;
import com.google.gson.annotations.Expose;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class GeoNotification {
    @Expose public String id;
    @Expose public double latitude;
    @Expose public double longitude;
    @Expose public int radius;
    @Expose public int transitionType;
    @Expose public String startTime;
    @Expose public String endTime;

    @Expose public Notification notification;

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
        if (json == null)
            return null;
        return Gson.get().fromJson(json, GeoNotification.class);
    }

    public Date getStartTime() {
        return parseDate(this.startTime);
    }

    public Date getEndTime() {
        return parseDate(this.endTime);
    }

    public boolean isWithinTimeRange() {
        Date now = new Date();
        Date startTime = getStartTime();
        Date endTime = getEndTime();
        boolean greaterThanOrEqualToStartTime = true;
        boolean lessThanEndTime = true;
        if (startTime != null) {
            greaterThanOrEqualToStartTime = now.after(startTime) || now.getTime() == startTime.getTime();
        }
        if (endTime != null) {
            lessThanEndTime = now.before(endTime);
        }
        return greaterThanOrEqualToStartTime && lessThanEndTime;
    }

    private Date parseDate(String date) {
        if (date == null) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(date);
        } catch (ParseException e) {
            return null;
        }
    }
}
