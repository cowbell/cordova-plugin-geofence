package com.cowbell.cordova.geofence;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

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
    @Expose public String color;
    @Expose public Object data;
    @Expose public boolean openAppOnClick;

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
            bmp = null;
        }

        return bmp;
    }

    public int getColor() {
        String hex = this.color;

        if (hex == null)
            return NotificationCompat.COLOR_DEFAULT;

        try {
            hex = stripHex(hex);

            if (hex.matches("[^0-9]*")) {
                return Color.class
                        .getDeclaredField(hex.toUpperCase())
                        .getInt(null);
            }

            int aRGB = Integer.parseInt(hex, 16);
            return aRGB + 0xFF000000;
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        return NotificationCompat.COLOR_DEFAULT;
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
        return "Notification title: " + getTitle() + " text: " + getText();
    }

    private long[] concat(long[] a, long[] b) {
        long[] c = new long[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    private String stripHex(String hex) {
        return (hex.charAt(0) == '#') ? hex.substring(1) : hex;
    }
}
