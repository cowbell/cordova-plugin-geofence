package com.cowbell.cordova.geofence;

import android.util.Log;
import android.text.format.Time;

import com.google.android.gms.location.Geofence;
import com.google.gson.annotations.Expose;

public class GeoNotification {
    @Expose public String id;
    @Expose public double latitude;
    @Expose public double longitude;
    @Expose public int radius;
    @Expose public int transitionType;

    @Expose public Notification notification;

    public GeoNotification() {
    }
    
    public boolean isScheduled(){
		Schedule schedule = new Schedule(this.notification.getScheduleDataJson());
		
		if(schedule.week == null)
			return true;
		
		Time time = new Time();
		time.setToNow();
		
		Schedule.WeekDay day = schedule.week[time.weekDay];
		
		if(day == null)
			return true;
		
		if(time.hour >= day.on.hour && time.hour <= day.off.hour){
			if(time.minute >= day.on.minute && time.minute <= day.off.minute){
				Log.i("Geofence", "GeoFence is active.");
				return true;
			}
		}
		Log.i("Geofence", "GeoFence is inactive.");
		return false;
	}
	
	public boolean isFrequencyOk(){
		Time time = new Time();
		time.setToNow();
		return (this.notification.lastTriggered + this.notification.frequency * 1000 < time.toMillis(false));
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
}
