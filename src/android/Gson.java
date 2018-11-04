package com.cowbell.cordova.geofence;

import com.google.gson.GsonBuilder;

public class Gson {
    private static final com.google.gson.Gson gson;

    static {
        gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    }

    public static com.google.gson.Gson get() {
        return gson;
    }
}
