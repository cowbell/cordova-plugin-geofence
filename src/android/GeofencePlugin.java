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

import com.google.android.gms.location.Geofence;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.util.Log;

public class GeofencePlugin extends CordovaPlugin {
    public static final String TAG = "GeofencePlugin";
    private GeoNotificationManager geoNotificationManager;
    private Context context;
    
    /**
     * @param cordova The context of the main Activity.
     * @param webView The associated CordovaWebView.
     */
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = this.cordova.getActivity().getApplicationContext();
        Logger.setLogger(new Logger(TAG, context, false));
        geoNotificationManager = new GeoNotificationManager(context);
    }
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        Log.d(TAG, "BTWPlugin execute action: "+ action + " args: " + args.toString());
        
        if(action.equals("addOrUpdate")){
            List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
            for(int i=0; i<args.length();i++){
            	GeoNotification not = parseFromJSONObject(args.getJSONObject(i));
            	if(not != null){
                    geoNotifications.add(not);	
            	}
            }
            geoNotificationManager.addGeoNotifications(geoNotifications, callbackContext);
        }
        else if(action.equals("remove")){ 
            List<String> ids = new ArrayList<String>();
            for(int i=0; i<args.length();i++){
                ids.add(args.getString(i));
            }
            geoNotificationManager.removeGeoNotifications(ids, callbackContext);
        }
        else if(action.equals("removeAll")){ 
            geoNotificationManager.removeAllGeoNotifications(callbackContext);
        }
        else{ 
            return false;
        }
        return true;

    }
    
    private GeoNotification parseFromJSONObject(JSONObject object){
        GeoNotification geo = null;
        try {
             geo = new GeoNotification()
                .setId(object.getString("id"))
                .setLatitude(object.getDouble("latitude"))
                .setLongitute(object.getDouble("longitude"))
                .setRadius(object.getInt("radius"))
                .setTransitionType((object.getInt("transitionType") == 1) ? Geofence.GEOFENCE_TRANSITION_ENTER : Geofence.GEOFENCE_TRANSITION_EXIT);
            JSONObject notificationObject = object.getJSONObject("notification");
            geo
                .setNotificationId(JSONHelper.getIntOrDefault(notificationObject, "id", 0))
            	.setNotificationText(JSONHelper.getStringOrDefault(notificationObject, "text", ""))
            	.setNotificationTitle(JSONHelper.getStringOrDefault(notificationObject, "title", ""))
            	.setOpenAppOnClick(JSONHelper.getBooleanOrDefault(notificationObject, "openAppOnClick", true))
                .setData(JSONHelper.getStringOrDefault(notificationObject, "data", null));
            
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return geo;
    }
    
} 