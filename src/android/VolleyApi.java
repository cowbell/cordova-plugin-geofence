package com.cowbell.cordova.geofence;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.cowbell.cordova.geofence.GeofencePlugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


public class VolleyApi {
    private Context context;
    private VolleyCallback callback;
    private static Logger logger;
    private static RequestQueue queue;
    private static LocalStorage localStorage;
    private String DEFAULT_BASE_URL = "https://api.kiot.io";
    private static String baseUrlExt = "api/v1/";
    static Map<String, String> headers = new HashMap<>();
    private static String BASE_URL = "";
    private static VolleyApi mThis;

    public  VolleyApi(Context context) throws JSONException {
        this.context = context;
        mThis = this;
        logger = Logger.getLogger();
        localStorage = new LocalStorage(context);
        // Instantiate the RequestQueue.
        this.queue = Volley.newRequestQueue(context);
         BASE_URL = DEFAULT_BASE_URL;
    }

    public static VolleyApi getInstance(){
        return mThis;
    }

    public void volleyGet(){
        String url ="https://www.google.com";

// Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Display the first 500 characters of the response string.
                        logger.log(Log.DEBUG, "Response is: "+ response.substring(0,500));
                        // textView.setText("Response is: "+ response.substring(0,500));
                    }
                }, error -> {
            logger.log(Log.DEBUG, "That didn't work!");
            // textView.setText("That didn't work!");
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public static void afterRelogin(Boolean success, JSONObject data, JSONObject userData) throws JSONException {
        if(success == true){
            String token = data.getString("token");
            JSONObject user =  data.getJSONObject("user");
            localStorage.setItem("token",token);
            localStorage.setItem("user",userData.toString());
        }
    }

    public static JSONObject tryRelogin(VolleyCallback callback) throws JSONException {
        JSONObject userData = new JSONObject(localStorage.getItem("user"));
        JSONObject res = postLogin(userData.getJSONObject("user"),callback);
       // JSONObject res = null;
//        if(res.get("restype")=="success"){
//            afterRelogin(true,res,userData);
//        } else {
//            afterRelogin(false,res,userData);
//        }
        return res;
    }

    public static JSONObject volleyPost(String url, JSONObject obj, Boolean except_url, VolleyCallback callback){
        String postUrl = null;
        try {
            postUrl = getTokens(except_url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject postData = new JSONObject();
        postData = obj;
        final JSONObject[] res = {new JSONObject()};
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl+url, postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    response.put("restype","success");
                    callback.onSuccess(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                res[0] = response;
                logger.log(Log.DEBUG, "Response is: "+ response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                JSONObject err = new JSONObject();
                try {
                    err.put("restype","error");
                    err.put("details",error);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                res[0] = err;
                byte[] bytes;
                String s;
                //Map<String, String> responseHeaders = error.networkResponse.headers;
                if(error.networkResponse != null && error.networkResponse.data != null) {
                     bytes = error.networkResponse.data;
                     s = new String(bytes, StandardCharsets.UTF_8);
                } else {
                     s = "error";
                }

                try {
                    err.put("message",s);
                    if(error.networkResponse.statusCode == 424){
                        callback.onSuccess(err);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if(error.networkResponse.statusCode == 401 ||error.networkResponse.statusCode == 403){
                    try {
                        JSONObject resp = tryRelogin(callback);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                //logger.log(Log.DEBUG,error.getMessage());
                error.printStackTrace();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                return headers;
            }
        };

        queue.add(jsonObjectRequest);
        return res[0];

    }

    public static String getTokens(Boolean except_url) throws JSONException {
        JSONObject baseurlobj = new JSONObject(localStorage.getItem("base_url"));
        BASE_URL = baseurlobj.getString("base_url");
        if(except_url==true){
            headers.remove("Authorization");
            return BASE_URL+'/';
        } else {
            String token = localStorage.getItem("token");
            headers.put("Authorization", "Bearer " + token);
            return BASE_URL +'/'+ baseUrlExt;
        }
    }

    public static JSONObject postLogin(JSONObject user, VolleyCallback callback){
        JSONObject res = volleyPost("user/authenticate",user,true, callback);
        return res;
    }

    public static JSONObject postTriggerScene(JSONObject data, VolleyCallback callback) {
        JSONObject res = volleyPost("scenes/trigger",data,false, callback);
        return res;
    }

    public static JSONObject postTriggerSwitch(JSONObject data, VolleyCallback callback) {
        JSONObject res = volleyPost("deviceevents/switch",data,false, callback);
        return res;
    }
}
