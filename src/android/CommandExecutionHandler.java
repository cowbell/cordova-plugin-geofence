package com.cowbell.cordova.geofence;

import org.apache.cordova.CallbackContext;
import org.json.JSONException;
import org.json.JSONObject;

public class CommandExecutionHandler implements IGoogleServiceCommandListener {
    private CallbackContext callbackContext;

    public CommandExecutionHandler(CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
    }

    @Override
    public void onCommandExecuted(Throwable error) {
        if (error != null) try {
            JSONObject errorObject = new JSONObject();
            errorObject.put("message", error.getMessage());
            if (error instanceof SecurityException) {
                errorObject.put("code", GeofencePlugin.ERROR_PERMISSION_DENIED);
            } else {
                errorObject.put("code", GeofencePlugin.ERROR_UNKNOWN);
            }
            callbackContext.error(errorObject);
        } catch (JSONException exception) {
            callbackContext.error(exception.getMessage());
            exception.printStackTrace();
        }
        else {
            callbackContext.success();
        }
    }
}
