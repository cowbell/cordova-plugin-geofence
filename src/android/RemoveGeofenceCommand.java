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

    @Override
    void onAddGeofencesResult(int statusCode, String[] arg1) {

    }

    @Override
    protected void ExecuteCustomCode() {
        if (pendingIntent != null) {
            logger.log(Log.DEBUG, "Tried to remove Geofences in first if");
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofencesIds).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        logger.log(Log.DEBUG, "Geofences successfully removed");
            /*
             * Handle successful addition of geofences here. You can send out a
             * broadcast intent or update the UI. geofences into the Intent's
             * extended data.
             */
                    } else {
                        logger.log(Log.DEBUG, "Removing geofences failed");
                        // If adding the geofences failed
            /*
             * Report errors here. You can log the error using Log.e() or update
             * the UI.
             */
                    }
                    CommandExecuted();
                }
            });
            //LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofencesIds, pendingIntent);
        //for some reason an exception is thrown when clearing an empty set of geofences
        } else if (geofencesIds != null && geofencesIds.size() > 0) {
            logger.log(Log.DEBUG, "Tried to remove Geofences in 2nd if");
            //LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofencesIds, pendingIntent);
            LocationServices.GeofencingApi.removeGeofences(mGoogleApiClient, geofencesIds).setResultCallback(new ResultCallback<Status>() {
                @Override
                public void onResult(Status status) {
                    if (status.isSuccess()) {
                        logger.log(Log.DEBUG, "Geofences successfully removed");
            /*
             * Handle successful addition of geofences here. You can send out a
             * broadcast intent or update the UI. geofences into the Intent's
             * extended data.
             */
                    } else {
                        logger.log(Log.DEBUG, "Removing geofences failed");
                        // If adding the geofences failed
            /*
             * Report errors here. You can log the error using Log.e() or update
             * the UI.
             */
                    }
                    CommandExecuted();
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
