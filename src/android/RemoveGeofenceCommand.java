package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class RemoveGeofenceCommand extends AbstractGoogleServiceCommand {
    private PendingIntent pendingIntent;
    private List<String> geofencesIds;

    public RemoveGeofenceCommand(Context context, PendingIntent pendingIntent) {
        super(context);
        this.pendingIntent = pendingIntent;
    }

    public RemoveGeofenceCommand(Context context, List<String> geofencesIds) {
        super(context);
        this.geofencesIds = geofencesIds;
    }

    @Override
    protected void ExecuteCustomCode() {
        // TODO: refactor
        if (pendingIntent != null) {
            logger.log(Log.DEBUG, "Tried to remove Geofences in first if");
            LocationServices.GeofencingApi
                .removeGeofences(mGoogleApiClient, geofencesIds)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            logger.log(Log.DEBUG, "Geofences successfully removed");
                        } else {
                            logger.log(Log.DEBUG, "Removing geofences failed");
                        }
                        CommandExecuted();
                    }
                });
        } else if (geofencesIds != null && geofencesIds.size() > 0) {
            logger.log(Log.DEBUG, "Tried to remove Geofences in 2nd if");
            LocationServices.GeofencingApi
                .removeGeofences(mGoogleApiClient, geofencesIds)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.isSuccess()) {
                            logger.log(Log.DEBUG, "Geofences successfully removed");
                        } else {
                            logger.log(Log.DEBUG, "Removing geofences failed");
                        }
                        CommandExecuted();
                    }
                });
        } else {
            logger.log(Log.DEBUG, "Tried to remove Geofences when there were none");
            CommandExecuted();
        }
    }
}
