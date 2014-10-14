package com.cowbell.cordova.geofence;

import com.google.android.gms.location.Geofence;
import com.google.gson.Gson;

public class GeoNotification {
	public String id;
	public double latitude;
	public double longitude;
	public int radius;
	public int transitionType;
	
	public Notification notification;

	public GeoNotification()
	{
	}
	
	public Geofence toGeofence() {
        return new Geofence.Builder()
                .setRequestId(id)
                .setTransitionTypes(transitionType)
                .setCircularRegion(
                        latitude, longitude, radius)
                .setExpirationDuration(Long.MAX_VALUE)
                .build();
    }
	
	public String toJson(){
		return new Gson().toJson(this);
	}
	public static GeoNotification fromJson(String json){
		if(json == null) return null;
		return new Gson().fromJson(json, GeoNotification.class);
	}
}
