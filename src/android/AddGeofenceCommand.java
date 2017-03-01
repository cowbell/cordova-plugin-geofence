package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class AddGeofenceCommand extends AbstractGoogleServiceCommand {
    private List<Geofence> geofencesToAdd;
    private PendingIntent pendingIntent;

    public AddGeofenceCommand(Context context, PendingIntent pendingIntent,
                              List<Geofence> geofencesToAdd) {
        super(context);
        this.geofencesToAdd = geofencesToAdd;
        this.pendingIntent = pendingIntent;
    }

    @Override
    public void ExecuteCustomCode() {
        logger.log(Log.DEBUG, "Adding new geofences");
        if (geofencesToAdd != null && geofencesToAdd.size() > 0) {
            try {
                LocationServices.GeofencingApi
                    .addGeofences(mGoogleApiClient, geofencesToAdd, pendingIntent)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(Status status) {
                            if (status.isSuccess()) {
                                logger.log(Log.DEBUG, "Geofences successfully added");
                            } else {
                                logger.log(Log.DEBUG, "Adding geofences failed");
                            }
                            CommandExecuted();
                        }
                    });
            } catch (Exception exception) {
                logger.log("Adding geofence failed", exception);
                CommandExecuted();
            }
        }
    }
}
