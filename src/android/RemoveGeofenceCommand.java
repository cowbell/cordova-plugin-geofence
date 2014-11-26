package com.cowbell.cordova.geofence;

import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.location.LocationClient.OnRemoveGeofencesResultListener;

public class RemoveGeofenceCommand extends AbstractGoogleServiceCommand
        implements OnRemoveGeofencesResultListener {
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
    public void onRemoveGeofencesByPendingIntentResult(int arg0,
            PendingIntent arg1) {
        logger.log(Log.DEBUG, "All Geofences removed");
        CommandExecuted();
    }

    @Override
    public void onRemoveGeofencesByRequestIdsResult(int arg0, String[] arg1) {
        logger.log(Log.DEBUG, "Geofences removed");
        CommandExecuted();
    }

    @Override
    protected void ExecuteCustomCode() {
        if (pendingIntent != null) {
            locationClient.removeGeofences(pendingIntent, this);
        //for some reason an exception is thrown when clearing an empty set of geofences
        } else if (geofencesIds != null && geofencesIds.size() > 0) {
            locationClient.removeGeofences(geofencesIds, this);
        } else {
            logger.log(Log.DEBUG, "Tried to remove Geofences when there were none");
            CommandExecuted();
        }
    }

}
