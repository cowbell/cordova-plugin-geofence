package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
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
            logger.log(Log.DEBUG, "Adding geofence failed");
            CommandExecuted(new CommandStatus(false, status.getStatusMessage()));
        }
    }
}
