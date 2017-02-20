package com.cowbell.cordova.geofence;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;
import android.Manifest;

import com.google.gson.annotations.Expose;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class GeofencePlugin extends CordovaPlugin {
    public static final String TAG = "GeofencePlugin";
    private GeoNotificationManager geoNotificationManager;
    private Context context;
    protected static Boolean isInBackground = true;
    public static CordovaWebView webView = null;

    private class Action {
        public String action;
        public JSONArray args;
        public CallbackContext callbackContext;

        public Action(String action,JSONArray args,CallbackContext callbackContext) {
            this.action = action;
            this.args = args;
            this.callbackContext = callbackContext;
        }
    }
    
    //FIXME: what about many executedActions at once
    private Action executedAction;

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
        executedAction = new Action(action, args, callbackContext);

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
            initialize(callbackContext);
        } else if (action.equals("deviceReady")) {
            deviceReady();
        } else {
            return false;
        }

        return true;
    }

    public boolean execute(Action action) throws JSONException {
        return execute(action.action, action.args, action.callbackContext);
    }

    private GeoNotification parseFromJSONObject(JSONObject object) {
        GeoNotification geo = null;
        geo = GeoNotification.fromJson(object.toString());
        return geo;
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

    private void deviceReady() {
        Intent intent = cordova.getActivity().getIntent();
        String data = intent.getStringExtra("geofence.notification.data");
        String js = "setTimeout('geofence.onNotificationClicked("
                + data + ")',0)";

        if (data == null) {
            Log.d(TAG, "No notifications clicked.");
        } else {
			intent.removeExtra("geofence.notification.data");
            webView.sendJavascript(js);
        }
    }

    private void initialize(CallbackContext callbackContext) throws JSONException {
        String[] permissions = {
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
        };

        if (!hasPermissions(permissions)) {
            PermissionHelper.requestPermissions(this, 0, permissions);
        } else {
            callbackContext.success();
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (!PermissionHelper.hasPermission(this, permission)) {
                return false;
            }
        }

        return true;
    }

    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        PluginResult result;

        if (executedAction != null) {
            for (int r:grantResults) {
                if (r == PackageManager.PERMISSION_DENIED) {
                    Log.d(TAG, "Permission Denied!");
                    result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    executedAction.callbackContext.sendPluginResult(result);
                    executedAction = null;
                    return;
                }
            }
            Log.d(TAG, "Permission Granted!");

            //try to execute method once again
            execute(executedAction);
            executedAction = null;
        }
    }
}
