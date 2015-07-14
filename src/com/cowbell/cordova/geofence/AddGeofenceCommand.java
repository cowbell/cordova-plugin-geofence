package com.cowbell.cordova.geofence;

import java.util.List;

import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;
import com.google.android.gms.location.LocationStatusCodes;

public class AddGeofenceCommand extends AbstractGoogleServiceCommand implements
        OnAddGeofencesResultListener {
    private List<Geofence> geofencesToAdd;
    private PendingIntent pendingIntent;

    public AddGeofenceCommand(Context context, PendingIntent pendingIntent,
            List<Geofence> geofencesToAdd) {
        super(context);
        this.geofencesToAdd = geofencesToAdd;
        this.pendingIntent = pendingIntent;
    }

    @Override
    public void onAddGeofencesResult(int statusCode, String[] arg1) {
        // If adding the geofences was successful
        if (LocationStatusCodes.SUCCESS == statusCode) {
            logger.log(Log.DEBUG, "Geofences successfully added");
            /*
             * Handle successful addition of geofences here. You can send out a
             * broadcast intent or update the UI. geofences into the Intent's
             * extended data.
             */
        } else {
            logger.log(Log.DEBUG, "Adding geofences failed");
            // If adding the geofences failed
            /*
             * Report errors here. You can log the error using Log.e() or update
             * the UI.
             */
        }
        CommandExecuted();
    }

    @Override
    public void ExecuteCustomCode() {
        // TODO Auto-generated method stub
        logger.log(Log.DEBUG, "Adding new geofences");
        locationClient.addGeofences(geofencesToAdd, pendingIntent, this);
    }
}
