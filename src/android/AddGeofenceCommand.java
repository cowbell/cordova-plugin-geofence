package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.LocationServices;

import org.apache.cordova.LOG;

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
        logger.log(Log.DEBUG, "Adding new geofences...");
        if (geofencesToAdd != null && geofencesToAdd.size() > 0) try {
            LocationServices.GeofencingApi
                .addGeofences(mGoogleApiClient, geofencesToAdd, pendingIntent)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            logger.log(Log.DEBUG, "Geofences successfully added");
                            CommandExecuted();
                        } else {
                            // TODO: pass this codes or create own standard
                            // check codes for iOS too

                            // Status Codes
                            // GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE
                            // GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES
                            // GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS

                            String message = "Adding geofences failed - SystemCode: " + status.getStatusCode();
                            logger.log(Log.ERROR, message);
                            CommandExecuted(new Error(message));
                        }
                    }
                });
        } catch (Exception exception) {
            logger.log(LOG.ERROR, "Exception while adding geofences");
            exception.printStackTrace();
            CommandExecuted(exception);
        }
    }
}
