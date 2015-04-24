package com.cowbell.cordova.geofence;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.Long;
import java.lang.System;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import com.google.gson.annotations.Expose;

public class Notification {
    private Context context;
    private AssetUtil assets;

    @Expose public int id;
    @Expose public String title;
    @Expose public String text;
    @Expose public long[] vibrate = new long[] { 1000 };
    @Expose public String icon = "";
    @Expose public String smallIcon = "";
    @Expose public Object data;
    @Expose public boolean openAppOnClick;
    @Expose public int start_date;
    @Expose public int end_date;
    @Expose public String start_time;
    @Expose public String end_time;
    @Expose public long period_milliseconds;
    @Expose private SharedPreferences sharedPreferences;

    public void setContext(Context context) {
        this.context = context;
        this.assets = AssetUtil.getInstance(context);
    }

    public String getText() {
        return this.text;
    }

    public String getTitle() {
        return this.title;
    }

    public int getSmallIcon() {
        int resId = assets.getResIdForDrawable(this.smallIcon);

        if (resId == 0) {
            resId = android.R.drawable.ic_menu_mylocation;
        }

        return resId;
    }

    public Bitmap getLargeIcon() {
        Bitmap bmp;

        try{
            Uri uri = assets.parse(this.icon);
            bmp = assets.getIconFromUri(uri);
        } catch (Exception e){
            bmp = assets.getIconFromDrawable(this.icon);
        }

        return bmp;
    }

    public String getDataJson() {
        if (this.data == null) {
            return "";
        }

        return Gson.get().toJson(this.data);
    }

    public long[] getVibrate() {
        return concat(new long[] {0}, vibrate);
    }

    public String toString() {
        return "Notification title: " + getTitle()
            + " text: " + getText();
    }

    private long[] concat(long[] a, long[] b) {
        long[] c = new long[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static boolean timeValidation (GeoNotification geoNotification, Context context){
        Logger logger = Logger.getLogger();

        if(geoNotification==null) return false;

        String[] start_time = geoNotification.notification.start_time.split(":");
        String[] end_time = geoNotification.notification.end_time.split(":");
        long currentDateTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

        if( geoNotification.notification.start_date < currentDateTime && geoNotification.notification.end_date > currentDateTime){
            Timestamp startTimeTimestamp = parseTime(start_time);
            Timestamp endTimeTimestamp = parseTime(end_time);
            Timestamp currentTime = new Timestamp(System.currentTimeMillis());

            return startTimeTimestamp != null && endTimeTimestamp != null && startTimeTimestamp.before(currentTime) && endTimeTimestamp.after(currentTime) && checkRateLimit((Context) context, geoNotification);
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

    private static boolean checkRateLimit(Context context, GeoNotification geoNotification){
        SharedPreferences settings = context.getSharedPreferences(context.getPackageName(), 0);
        SharedPreferences.Editor editor = settings.edit();
        long time;

        String lastTime = settings.getString(geoNotification.id.concat("_time"),null);
        String message = settings.getString(geoNotification.id.concat("_message"),null);

        if(lastTime == null){
            editor.putString(geoNotification.id.concat("_time"), Long.toString(System.currentTimeMillis()));
            editor.putString(geoNotification.id.concat("_message"), geoNotification.notification.text);
            editor.apply();
        }
        else{

            if(message.equals(geoNotification.notification.text)){
                time = Long.parseLong(lastTime)/1000 + geoNotification.notification.period_milliseconds/1000;
                if (time > TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())){
                    return false;
                }
                else{
                    editor.putString(geoNotification.id, Long.toString(System.currentTimeMillis()));
                    editor.apply();
                }
            }
            else{
                editor.putString(geoNotification.id.concat("_time"), Long.toString(System.currentTimeMillis()));
                editor.putString(geoNotification.id.concat("_message"), geoNotification.notification.text);
                editor.apply();
            }
        }

        return true;
    }


}
