package com.cowbell.cordova.geofence;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VolleyApi {
    private Context context;
    private Logger logger;
    private RequestQueue queue;
    private LocalStorage localStorage;
    private String BASE_URL = "http://9333-45-249-77-185.ngrok.io/";
    private String baseUrl = BASE_URL+"api/v1/";
    Map<String, String> headers = new HashMap<>();

    public VolleyApi(Context context) {
        this.context = context;
        logger = Logger.getLogger();
        localStorage = new LocalStorage(context);
        // Instantiate the RequestQueue.
        this.queue = Volley.newRequestQueue(context);
        //this.headers.put("Content-type", "application/json");
        //String url ="https://www.google.com";
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

    public void afterRelogin(Boolean success,JSONObject data, JSONObject userData) throws JSONException {
        if(success == true){
            String token = data.getString("token");
            JSONObject user =  data.getJSONObject("user");
            localStorage.setItem("token",token);
            localStorage.setItem("user",userData.toString());
        }
    }

    public JSONObject tryRelogin() throws JSONException {
        JSONObject userData = new JSONObject(localStorage.getItem("user"));
        JSONObject res = postLogin(userData.getJSONObject("user"));
        if(res.get("restype")=="success"){
            afterRelogin(true,res,userData);
        } else {
            afterRelogin(false,res,userData);
        }
        return res;
    }

    public JSONObject volleyPost(String url, JSONObject obj, Boolean except_url){
        String postUrl = getTokens(except_url);//"https://reqres.in/api/users";
       // RequestQueue requestQueue = Volley.newRequestQueue(this);

        JSONObject postData = new JSONObject();
        postData = obj;
        final JSONObject[] res = {new JSONObject()};
//        try {
//            postData.put("name", "Jonathan");
//            postData.put("job", "Software Engineer");
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, postUrl+url, postData, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    response.put("restype","success");
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
                //Map<String, String> responseHeaders = error.networkResponse.headers;
                if(error.networkResponse.statusCode == 401 ||error.networkResponse.statusCode == 403){
                    try {
                        JSONObject resp = tryRelogin();
                        if(resp.get("restype")=="success"){
                            volleyPost(url,obj,except_url);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                logger.log(Log.DEBUG,error.getMessage());
                error.printStackTrace();
            }
        }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
               // Map<String, String> headers = new HashMap<>();
               // headers.put("Authorization", "Bearer XXXXXXXXXX");
                return headers;
            }
        };

        queue.add(jsonObjectRequest);
        return res[0];

    }

    public String getTokens(Boolean except_url){
        if(except_url==true){
            headers.remove("Authorization");
            return BASE_URL;
        } else {
            String token = localStorage.getItem("token");
            headers.put("Authorization", "Bearer " + token);
            return baseUrl;
        }
    }

    public JSONObject postLogin(JSONObject user){
        JSONObject res = volleyPost("user/authenticate",user,true);
        return res;
    }

    public JSONObject postTriggerScene(JSONObject data) {
        JSONObject res = volleyPost("scenes/trigger",data,false);
        return res;
    }

    public JSONObject postTriggerSwitch(JSONObject data) {
        JSONObject res = volleyPost("deviceevents/switch",data,false);
        return res;
    }
}
