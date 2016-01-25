package com.cowbell.cordova.geofence;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GeofencePlugin extends CordovaPlugin {

    public static final String TAG = "GeofencePlugin";

    private GeoNotificationManager geoNotificationManager;
    private static Context context;
    public static CordovaWebView webView;
    protected static Boolean isInForeground = true;
    protected static Boolean isDeviceReady = false;
    private GeoNotificationStore geoNotificationStore;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        GeofencePlugin.webView = webView;
        isInForeground = true;
        Logger.setLogger(new Logger(TAG, context, false));
        context = this.cordova.getActivity().getApplicationContext();
        geoNotificationManager = new GeoNotificationManager(context);
        geoNotificationStore = new GeoNotificationStore(context);
    }

    @Override
    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        Log.v(TAG, "Executing action=" + action);

        if (action.equals("addOrUpdate")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();

                    for (int i = 0; i < args.length(); i++) {
                        GeoNotification notification = null;

                        try{
                            notification = parseFromJSONObject(args.getJSONObject(i));
                        }catch(JSONException ex){

                        }

                        if (notification != null) {
                            geoNotifications.add(notification);
                        }
                    }

                    geoNotificationManager.addGeoNotifications(geoNotifications, callbackContext);
                }
            });
        } else if (action.equals("remove")) {
            List<String> ids = new ArrayList<String>();

            for (int i = 0; i < args.length(); i++) {
                ids.add(args.getString(i));
            }

            geoNotificationManager.removeGeoNotifications(ids, callbackContext);
        } else if (action.equals("removeAll")) {
            geoNotificationManager.removeAllGeoNotifications(callbackContext);
            ;
        } else if (action.equals("getWatched")) {
            List<GeoNotification> geoNotifications = geoNotificationManager.getWatched();
            callbackContext.success(Gson.get().toJson(geoNotifications));
        } else if (action.equals("initialize")) {
            callbackContext.success();
        } else if (action.equals("deviceReady")) {
            isDeviceReady = true;
            callbackContext.success();
        } else {
            return false;
        }
        return true;
    }

    /**
     * Called when the activity is paused.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        isInForeground = false;
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app
     */
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        isInForeground = true;
        isDeviceReady = true;
    }

    /**
     * Called when the activity receives a new intent.
     */
    @Override
    public void onNewIntent(Intent intent) {
        isInForeground = true;
        Log.d(TAG, "New Intent Found!");
        wasNotificationClicked(intent);
    }

    /**
     * The final call you receive before your activity is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        isDeviceReady = false;
        isInForeground = false;
        webView = null;
    }

    public static void onTransitionReceived(List<GeoNotification> notifications) {
        Log.d(TAG, "Transition Event Received!");
        String js = "setTimeout('geofence.onTransitionReceived("
                + Gson.get().toJson(notifications) + ")',0)";
        if (webView == null) {
            Log.d(TAG, "Webview is null");
        } else {
            webView.sendJavascript(js);
        }
    }

    private GeoNotification parseFromJSONObject(JSONObject object) {
        GeoNotification geo = null;
        geo = GeoNotification.fromJson(object.toString());
        return geo;
    }

    protected void wasNotificationClicked(Intent intent) {
        Log.d(TAG, "Notification Clicked");

        String data = intent.getStringExtra("geofence.notification.data");

        if(data != null && !data.isEmpty()) {
            Log.d(TAG, "Data found for notification click");

            String js = "setTimeout('geofence.onNotificationClicked("
                    + data + ")',0)";

            Log.d(TAG, String.valueOf(isInForeground));

            if(isInForeground) {
                Log.d(TAG, "Firing JS");
                webView.sendJavascript(js);
            }
        }
    }

    public static boolean isInForeground() {
        return isInForeground;
    }

    public static boolean isActive() {
        return webView != null;
    }
}