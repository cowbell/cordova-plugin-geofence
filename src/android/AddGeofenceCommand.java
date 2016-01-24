package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.location.LocationManager;
import android.util.Log;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;

public class AddGeofenceCommand extends AbstractGoogleServiceCommand {
    private GeofencingRequest geofenceRequest;
    private PendingIntent pendingIntent;

    public AddGeofenceCommand(Context context, PendingIntent pendingIntent,
            GeofencingRequest geofenceRequest) {
        super(context);
        this.geofenceRequest = geofenceRequest;
        this.pendingIntent = pendingIntent;
    }

    @Override
    public void Execute() {
        logger.log(Log.DEBUG, "Adding geofence");

        if (geofenceRequest != null) {
            LocationServices.GeofencingApi
                    .addGeofences(googleApiClient, geofenceRequest, pendingIntent)
                    .setResultCallback(this);
        }
    }

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            logger.log(Log.DEBUG, "Geofence successfully added");
            CommandExecuted(CommandStatus.SUCCESS);
        } else {
            StringBuilder message = new StringBuilder();
            message.append("Adding geofence failed \n");
            message.append("Status code: " + status.getStatusCode() + "\n");
            message.append("Message: " + status.getStatusMessage() + "\n");
            logger.log(Log.ERROR, message.toString());
            CommandExecuted(new CommandStatus(false, message.toString()));
        }
    }
}
