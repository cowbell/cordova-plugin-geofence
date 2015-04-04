package com.cowbell.cordova.geofence;

public class Notification {
    public int id;
    public String title;
    public String text;
    public long[] vibrate = new long[] { 1000 };
    public Object data;
    public boolean openAppOnClick;
}
