package com.cowbell.cordova.geofence;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.gson.annotations.Expose;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GeoNotification {
    @Expose public String _id;
    @Expose public String id;
    @Expose public String name;
    @Expose public String home_id;
    @Expose public String user_id;
    @Expose public String w_actions;
    @Expose public double latitude;
    @Expose public double longitude;
    @Expose public int radius;
    @Expose public int transitionType;

    @Expose public Notification notification;

    public GeoNotification() {
    }

    public Geofence toGeofence() {
       // List geofenceArrayList;
        List<Geofence> geofenceArrayList = new ArrayList<>();
        geofenceArrayList.add(new Geofence.Builder()
                .setRequestId(id)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER|Geofence.GEOFENCE_TRANSITION_EXIT|Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(10000)
                .setCircularRegion(latitude, longitude, 10)
                .setExpirationDuration(Long.MAX_VALUE).build());
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofences(geofenceArrayList);
        builder.build();
        return new Geofence.Builder()
            .setRequestId(id)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER|Geofence.GEOFENCE_TRANSITION_EXIT|Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay(10000)
            .setCircularRegion(latitude, longitude, 10)
            .setExpirationDuration(Long.MAX_VALUE).build();
    }

    public String toJson() {
        return Gson.get().toJson(this);
    }

    public static GeoNotification fromJson(String json) {
        if (json == null) return null;
        return Gson.get().fromJson(json, GeoNotification.class);
    }
}
