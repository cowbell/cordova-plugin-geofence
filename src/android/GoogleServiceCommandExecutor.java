package com.cowbell.cordova.geofence;

import android.content.Context;
import android.content.IntentSender;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;

import java.util.LinkedList;
import java.util.Queue;

public class GoogleServiceCommandExecutor implements
        IGoogleServiceCommandListener,
        ConnectionCallbacks,
        OnConnectionFailedListener {
    private Queue<AbstractGoogleServiceCommand> commandsToExecute;
    private boolean isExecuting = false;
    private Context context;
    private GoogleApiClient googleApiClient;
    private Logger logger;

    public GoogleServiceCommandExecutor(Context context) {
        commandsToExecute = new LinkedList<AbstractGoogleServiceCommand>();
        this.context = context;
        googleApiClient = buildGoogleApi(context);
        logger = Logger.getLogger();
        connectToGoogleServices();
    }

    public void QueueToExecute(AbstractGoogleServiceCommand command) {
        commandsToExecute.add(command);
        if (!isExecuting) {
            ExecuteNext();
        }
    }

    private void ExecuteNext() {
        if (commandsToExecute.isEmpty()) {
            return;
        }
        if (!googleApiClient.isConnected()) {
            connectToGoogleServices();
            return;
        }
        isExecuting = true;
        AbstractGoogleServiceCommand command = commandsToExecute.poll();
        command.addListener(this);
        command.setGoogleApiClient(googleApiClient);
        command.Execute();
    }

    @Override
    public void onCommandExecuted(CommandStatus status) {
        isExecuting = false;
        ExecuteNext();
    }

    private void connectToGoogleServices() {
        if (!googleApiClient.isConnected() || !googleApiClient.isConnecting()) {
            logger.log(Log.DEBUG, "Connecting google api client");
            googleApiClient.connect();
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        logger.log(Log.DEBUG, "Google api client connected");

        if (!commandsToExecute.isEmpty() && !isExecuting) {
            ExecuteNext();
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        logger.log(Log.DEBUG, String.format("Google api client connection suspended, cause: %d", cause));
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        StringBuilder errorMessage = new StringBuilder();
        errorMessage.append("Connection to Google api client failed \n");
        errorMessage.append("Error code: " + result.getErrorCode() + " \n");
        errorMessage.append("Error message: " + result.getErrorMessage() + "\n");
        logger.log(Log.ERROR, errorMessage.toString());

        // couldn't connect to api so if any commands to execute left
        for (AbstractGoogleServiceCommand command: commandsToExecute) {
            command.CommandExecuted(new CommandStatus(false, errorMessage.toString()));
        }
    }

    private GoogleApiClient buildGoogleApi(Context context) {
        return new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }
}
