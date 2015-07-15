package com.cowbell.cordova.geofence;

import org.json.JSONException;
import org.json.JSONObject;

public class JSONHelper {
    public static String getStringOrDefault(JSONObject obj, String property,
            String def) {
        String result = def;
        try {
            result = obj.getString(property);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static Boolean getBooleanOrDefault(JSONObject obj, String property,
            Boolean def) {
        Boolean result = def;
        try {
            result = obj.getBoolean(property);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static int getIntOrDefault(JSONObject obj, String property, int def) {
        int result = def;
        try {
            result = obj.getInt(property);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
