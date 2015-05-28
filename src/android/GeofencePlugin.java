package com.cowbell.cordova.geofence;

import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.Context;
import android.util.Log;

public class GeofencePlugin extends CordovaPlugin {
    public static final String TAG = "GeofencePlugin";
    private static GeoNotificationManager geoNotificationManager;
    private static Context context;
    private static Context appcontext;
    protected static Boolean isInBackground = true;
    private static CordovaWebView webView = null;
    private static CallbackContext callbackContext;

    /**
     * @param cordova
     *            The context of the main Activity.
     * @param webView
     *            The associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        GeofencePlugin.webView = webView;
        context = this.cordova.getActivity().getApplicationContext();
        Logger.setLogger(new Logger(TAG, context, false));
        geoNotificationManager = new GeoNotificationManager(context);
    }

    @Override
    public boolean execute(String action, JSONArray args,
            CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "GeofencePlugin execute action: " + action + " args: "
                + args.toString());

        if (action.equals("addOrUpdate")) {
            List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
            for (int i = 0; i < args.length(); i++) {
                GeoNotification not = parseFromJSONObject(args.getJSONObject(i));
                if (not != null) {
                    geoNotifications.add(not);
                }
            }
            geoNotificationManager.addGeoNotifications(geoNotifications,
                    callbackContext);
        } else if (action.equals("remove")) {
            List<String> ids = new ArrayList<String>();
            for (int i = 0; i < args.length(); i++) {
                ids.add(args.getString(i));
            }
            geoNotificationManager.removeGeoNotifications(ids, callbackContext);
        } else if (action.equals("removeAll")) {
            geoNotificationManager.removeAllGeoNotifications(callbackContext);
        } else if (action.equals("getWatched")) {
            List<GeoNotification> geoNotifications = geoNotificationManager
                    .getWatched();
            callbackContext.success(Gson.get().toJson(geoNotifications));
        } else if (action.equals("initialize")) {
            callbackContext.success();

        } else if (action.equals("deviceReady")) {
            deviceReady();
        } else {
            return false;
        }
        return true;

    }

    private GeoNotification parseFromJSONObject(JSONObject object) {
        GeoNotification geo = null;
        geo = GeoNotification.fromJson(object.toString());
        return geo;
    }

    public static void onTransitionReceived(List<GeoNotification> notifications) {
        String js = "setTimeout('geofence.onTransitionReceived("
                + Gson.get().toJson(notifications) + ")',0)";
        if (webView == null) {
            Log.d(TAG, "Webview is null");
        } else {
            webView.sendJavascript(js);
        }
    }

    public static void registar(List<GeoNotification> notifications) {
	List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
	
	// Geofence.initialize() が呼び出される前に発火したときは、
        // geoNotificationManager が未初期化で null で本メソッドが
	// 呼び出される。
	if (geoNotificationManager == null) {
	    return;
	}

	Log.d(TAG, "GeofencePlugin#registar(): enter");
	Log.d(TAG, "GeofencePlugin#registar(): check-1");
	for (GeoNotification geoNotification : notifications) {
	    Log.d(TAG, "GeofencePlugin#registar(): check-2");
	    if (geoNotification.period.isRepeat() == true) {
		Log.d(TAG, "GeofencePlugin#registar(): check-3");
		geoNotifications.add(geoNotification);
	    }
	    Log.d(TAG, "GeofencePlugin#registar(): check-4");
	}
	Log.d(TAG, "GeofencePlugin#registar(): check-5");

	if (geoNotifications.size() > 0) {
	    geoNotificationManager.addGeoNotifications2(geoNotifications);
	}
	Log.d(TAG, "GeofencePlugin#registar(): leave");
    }

    private void deviceReady() {
        Intent intent = cordova.getActivity().getIntent();
        String data = intent.getStringExtra("geofence.notification.data");
        String js = "setTimeout('geofence.onNotificationClicked("
                + data + ")',0)";

        if (data == null) {
            Log.d(TAG, "No notifications clicked.");
        } else {
            webView.sendJavascript(js);
        }
    }
}
