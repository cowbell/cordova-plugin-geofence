package com.cowbell.cordova.geofence;

import com.google.gson.annotations.Expose;

public class Schedule {
	public class WeekDay{
		public class On{
			@Expose public int hour;
			@Expose public int minute;
		}
		public class Off{
			@Expose public int hour;
			@Expose public int minute;
		}
		
		@Expose public On on;
		@Expose public Off off;
		
		public WeekDay(String json){
			on = Gson.get().fromJson(json, On.class);
			off = Gson.get().fromJson(json, Off.class);
		}
	}

    @Expose public WeekDay[] week;

    public Schedule(String json){
		week = Gson.get().fromJson(json, WeekDay[].class);
	}
    
    
}
