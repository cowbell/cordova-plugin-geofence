package com.cowbell.cordova.geofence;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;
import com.google.android.gms.common.api.Status;
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

    @Override
    public void onResult(Status status) {
        if (status.isSuccess()) {
            logger.log(Log.DEBUG, "Geofences successfully removed");
            CommandExecuted(CommandStatus.SUCCESS);
        } else {
            StringBuilder message = new StringBuilder();
            message.append("Removing geofences failed \n");
            message.append("Status code: " + status.getStatusCode() + "\n");
            message.append("Message: " + status.getStatusMessage() + "\n");
            logger.log(Log.ERROR, message.toString());
            CommandExecuted(new CommandStatus(false, message.toString()));
        }
    }

    @Override
    public void Execute() {
        if (pendingIntent != null) {
            LocationServices.GeofencingApi
                    .removeGeofences(googleApiClient, pendingIntent)
                    .setResultCallback(this);
        } else if (geofencesIds != null && geofencesIds.size() > 0) {
            LocationServices.GeofencingApi
                    .removeGeofences(googleApiClient, geofencesIds)
                    .setResultCallback(this);
        } else {
            String message = "Tried to remove Geofences when there were none";
            logger.log(Log.DEBUG, message);
            CommandExecuted(CommandStatus.SUCCESS);
        }
    }
}
