package com.cowbell.cordova.geofence;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
    @Expose public String event;
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
        return new Geofence.Builder()
                .setRequestId(id)
                .setTransitionTypes(transitionType == 1 ? Geofence.GEOFENCE_TRANSITION_ENTER: transitionType == 2 ?Geofence.GEOFENCE_TRANSITION_EXIT
                        :Geofence.GEOFENCE_TRANSITION_ENTER |Geofence.GEOFENCE_TRANSITION_EXIT )
                .setLoiteringDelay(10000)
                .setCircularRegion(latitude, longitude, radius)
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