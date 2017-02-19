package com.cowbell.cordova.geofence;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

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
        return "Notification title: " + getTitle() + " text: " + getText();
    }

    private long[] concat(long[] a, long[] b) {
        long[] c = new long[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }
}
