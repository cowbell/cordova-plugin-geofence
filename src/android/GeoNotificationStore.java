package com.cowbell.cordova.geofence;

import java.util.ArrayList;
import java.util.List;

import org.apache.cordova.JSONUtils;
import org.json.JSONObject;

import com.google.gson.Gson;

import android.content.Context;

public class GeoNotificationStore {
	private LocalStorage storage;
	
	public GeoNotificationStore(Context context){
		storage = new LocalStorage(context);
	}
	
	public void setGeoNotification(GeoNotification geoNotification){
		storage.setItem(geoNotification.id, new Gson().toJson(geoNotification));
	}
	public GeoNotification getGeoNotification(String id){
		String objectJson = storage.getItem(id);
		return GeoNotification.fromJson(objectJson);
	}
	public List<GeoNotification> getAll(){
		List<String> objectJsonList = storage.getAllItems();
		List<GeoNotification> result = new ArrayList<GeoNotification>();
		for(String json: objectJsonList){
			result.add(GeoNotification.fromJson(json));
		}
		return result;
	}
	public void remove(String id){
		storage.removeItem(id);
	}
	public void clear(){
		storage.clear();
	}
}

