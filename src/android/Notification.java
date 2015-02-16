package com.cowbell.cordova.geofence;

import android.util.Log;

import java.lang.System;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class Notification {
    public int id;
    public String title;
    public String text;
    public Object data;
    public boolean openAppOnClick;
    public int start_date;
    public int end_date;
    public String start_time;
    public String end_time;
    public int period_milliseconds;
    
    
    public static boolean timeValidation (GeoNotification geoNotification){
        Logger logger = Logger.getLogger();

        if(geoNotification==null) return false;

        String[] start_time = geoNotification.notification.start_time.split(":");
        String[] end_time = geoNotification.notification.end_time.split(":");
        long currentDateTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        
        if( geoNotification.notification.start_date < currentDateTime && geoNotification.notification.end_date > currentDateTime){
            Timestamp startTimeTimestamp = parseTime(start_time);
            Timestamp endTimeTimestamp = parseTime(end_time);
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());
            
            return startTimeTimestamp != null && endTimeTimestamp != null && startTimeTimestamp.before(currentTime) && endTimeTimestamp.after(currentTime);
        }
        else return false;
    }

    private static Timestamp parseTime(String[] time){
        Logger logger = Logger.getLogger();

        try {
            Calendar c = Calendar.getInstance();
            int year = c.get(Calendar.YEAR);
            int month = c.get(Calendar.MONTH);
            int day = c.get(Calendar.DAY_OF_MONTH);

            c.set(Calendar.YEAR, year);
            c.set(Calendar.MONTH, month);
            c.set(Calendar.DATE, day);
            c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time[0]));
            c.set(Calendar.MINUTE, Integer.parseInt(time[1]));
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);

            return new Timestamp(c.getTimeInMillis());
        }
        catch(Exception e){
            logger.log(Log.ERROR, "Error parsing time for time: " + time);
            return null;
        }
    }
    
}
