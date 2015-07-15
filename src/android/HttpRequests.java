package com.cowbell.cordova.geofence;

import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpRequests {

    public static void postJson(String endpoint, String json)
            throws IOException {
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setChunkedStreamingMode(0);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/json;charset=UTF-8");
            conn.setRequestProperty("Content-Length",
                    Integer.toString(json.getBytes().length));
            // post the request
            Log.d(GeofencePlugin.TAG, "Sending POST request to " + endpoint + " with JSON request body: " + json);
            OutputStream out = conn.getOutputStream();
            out.write(json.getBytes());
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status == 500) {
                throw new IOException(conn.getResponseMessage());
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

}
