package com.tsubik.cordova.geofence;

import com.google.android.gms.location.Geofence;
import com.google.gson.Gson;

public class GeoNotification {
	private String id;
	private double latitude;
	private double longitude;
	private int radius;
	private int transitionType;
	private boolean openAppOnClick;
	
	private String notificationTitle;
	private String notificationText;
	private Object data;
	
	public GeoNotification setId(String id)
	{
		this.id = id;
		return this;
	}
	public String getId()
	{
		return this.id;
	}
	public GeoNotification setLatitude(double lat){
		this.latitude = lat;
		return this;
	}
	public double getLatitude()
	{
		return this.latitude;
	}
	public GeoNotification setLongitute(double lng){
		this.longitude = lng;
		return this;
	}
	public double getLongitude(){
		return this.longitude;
	}
	public GeoNotification setRadius(int radius){
		this.radius = radius;
		return this;
	}
	public int getRadius(){
		return this.radius;
	}
	public GeoNotification setTransitionType(int transitionType){
		this.transitionType = transitionType;
		return this;
	}
	public int getTransitionType(){
		return this.transitionType;
	}
	public GeoNotification setNotificationText(String text){
		this.notificationText = text;
		return this;
	}
	public String getNotificationText(){
		return this.notificationText;
	}
	public GeoNotification setNotificationTitle(String title){
		this.notificationTitle = title;
		return this;
	}
	public String getNotificationTitle(){
		if(this.notificationText == null || this.notificationText.isEmpty()){
			return "Geofence detector";
		}
		return this.notificationTitle;
	}
	public GeoNotification setOpenAppOnClick(boolean openAppOnClick){
		this.openAppOnClick = openAppOnClick;
		return this;
	}
	public boolean getOpenAppOnClick(){
		return this.openAppOnClick;
	}

	public GeoNotification setData(Object data){
		this.data = data;
		return this;
	}
	public Object getData(){
		return this.data;
	}
	
	public GeoNotification()
	{
	}
	
	public Geofence toGeofence() {
        return new Geofence.Builder()
                .setRequestId(getId())
                .setTransitionTypes(getTransitionType())
                .setCircularRegion(
                        getLatitude(), getLongitude(), getRadius())
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
