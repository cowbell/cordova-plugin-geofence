package com.cowbell.cordova.geofence;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;

public abstract class AbstractGoogleServiceCommand implements
        ConnectionCallbacks, OnConnectionFailedListener {
    protected LocationClient locationClient;
    protected Logger logger;
    protected boolean connectionInProgress = false;
    protected List<IGoogleServiceCommandListener> listeners;
    protected Context context;

    public AbstractGoogleServiceCommand(Context context) {
        this.context = context;
        locationClient = new LocationClient(context, this, this);
        logger = Logger.getLogger();
        listeners = new ArrayList<IGoogleServiceCommandListener>();
    }

    private void connectToGoogleServices() {
        if (!locationClient.isConnected() || !locationClient.isConnecting()
                && !connectionInProgress) {
            connectionInProgress = true;
            logger.log(Log.DEBUG, "Connecting location client");
            locationClient.connect();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        connectionInProgress = false;
        logger.log(Log.DEBUG, "Connecting to google services fail - "
                + connectionResult.toString());
        /*
         * Google Play services can resolve some errors it detects. If the error
         * has a resolution, try sending an Intent to start a Google Play
         * services activity that can resolve error.
         */
        if (connectionResult.hasResolution()) {

            // If no resolution is available, display an error dialog
        } else {

        }
    }

    @Override
    public void onConnected(Bundle arg0) {
        // TODO Auto-generated method stub
        logger.log(Log.DEBUG, "Google play services connected");
        // Get the PendingIntent for the request
        ExecuteCustomCode();
    }

    @Override
    public void onDisconnected() {
        // Turn off the request flag
        connectionInProgress = false;
        // Destroy the current location client
        locationClient = null;
        // Display the connection status
        // Toast.makeText(this, DateFormat.getDateTimeInstance().format(new
        // Date()) + ": Disconnected. Please re-connect.",
        // Toast.LENGTH_SHORT).show();
        logger.log(Log.DEBUG, "Google play services Disconnected");
    }

    public void addListener(IGoogleServiceCommandListener listener) {
        listeners.add(listener);
    }

    public void Execute() {
        connectToGoogleServices();
    }

    protected void CommandExecuted() {
        // Turn off the in progress flag and disconnect the client
        connectionInProgress = false;
        locationClient.disconnect();
        for (IGoogleServiceCommandListener listener : listeners) {
            listener.onCommandExecuted();
        }
    }

    protected abstract void ExecuteCustomCode();

}
