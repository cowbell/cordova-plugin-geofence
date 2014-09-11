package com.tsubik.cordova.geofence;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationClient.OnRemoveGeofencesResultListener;

public class GeoNotificationManager
{
	private Context context;
	private GeoNotificationStore geoNotificationStore;
	private LocationClient locationClient;
    private LocationRequest locationRequest;
    private Logger logger;
    private boolean connectionInProgress = false;
    private List<Geofence> geoFences;
    private PendingIntent pendingIntent;
    private GoogleServiceCommandExecutor googleServiceCommandExecutor;
    
    
	public GeoNotificationManager(Context context){
		this.context = context;
		geoNotificationStore = new GeoNotificationStore(context);
		logger = Logger.getLogger();
		googleServiceCommandExecutor = new GoogleServiceCommandExecutor();
		pendingIntent = getTransitionPendingIntent();
		if(areGoogleServicesAvailable()){
			logger.log(Log.DEBUG, "Google play services available");
		}
		else{
			logger.log(Log.DEBUG, "Google play services not available");
		}
	}
	
	public void loadFromStorageAndInitializeGeofences(){
		List<GeoNotification> geoNotifications = geoNotificationStore.getAll();
		geoFences = new ArrayList<Geofence>();
		for(GeoNotification geo: geoNotifications){
			geoFences.add(geo.toGeofence());
		}
		googleServiceCommandExecutor.QueueToExecute(new RemoveGeofenceCommand(context, pendingIntent));
		googleServiceCommandExecutor.QueueToExecute(new AddGeofenceCommand(context,pendingIntent, geoFences));
	}
	
	private boolean areGoogleServicesAvailable() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            return true;
        } else {
            return false;
        }
    }
	public void addGeoNotifications(List<GeoNotification> geoNotifications){
		List<Geofence> newGeofences = new ArrayList<Geofence>();
		for(GeoNotification geo: geoNotifications){
			geoNotificationStore.setGeoNotification(geo);
			newGeofences.add(geo.toGeofence());
		}
		googleServiceCommandExecutor.QueueToExecute(new AddGeofenceCommand(context, pendingIntent,newGeofences));
	}
	
	public void removeGeoNotification(String id){
		List<String> ids = new ArrayList<String>();
		ids.add(id);
		removeGeoNotifications(ids);
	}
	
	public void removeGeoNotifications(List<String> ids){
		googleServiceCommandExecutor.QueueToExecute(new RemoveGeofenceCommand(context, ids));
	}
	
	public void removeAllGeoNotifications(){
		googleServiceCommandExecutor.QueueToExecute(new RemoveGeofenceCommand(context, pendingIntent));
	}

	/*
     * Create a PendingIntent that triggers an IntentService in your
     * app when a geofence transition occurs.
     */
    private PendingIntent getTransitionPendingIntent() {
        // Create an explicit Intent
        Intent intent = new Intent(context,
                ReceiveTransitionsIntentService.class);
        /*
         * Return the PendingIntent
         */
        return PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }
	
}
