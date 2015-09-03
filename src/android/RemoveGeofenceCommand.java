package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

//import com.google.android.gms.location.LocationClient.OnRemoveGeofencesResultListener;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class RemoveGeofenceCommand extends AbstractGoogleServiceCommand{
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

    //@Override
    public void onRemoveGeofencesByPendingIntentResult(int arg0,
            PendingIntent arg1) {
        logger.log(Log.DEBUG, "All Geofences removed");
        CommandExecuted();
    }

    //@Override
    public void onRemoveGeofencesByRequestIdsResult(int arg0, String[] arg1) {
        logger.log(Log.DEBUG, "Geofences removed");
        CommandExecuted();
    }

    @Override
    protected void ExecuteCustomCode() {
        if (pendingIntent != null) {
            PendingResult<Status> pendingResult = LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofencesIds);
            pendingResult.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    //onRemoveGeofencesByRequestIdsResult(status.getStatusCode());
                }
            });
            //LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofencesIds, pendingIntent);
        //for some reason an exception is thrown when clearing an empty set of geofences
        } else if (geofencesIds != null && geofencesIds.size() > 0) {
            //LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofencesIds, pendingIntent);
            PendingResult<Status> pendingResult = LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofencesIds);
            pendingResult.setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    //onRemoveGeofencesByRequestIdsResult(status.getStatusCode());
                }
            });
        } else {
            logger.log(Log.DEBUG, "Tried to remove Geofences when there were none");
            CommandExecuted();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
}
