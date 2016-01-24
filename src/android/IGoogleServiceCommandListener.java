package com.cowbell.cordova.geofence;

import com.google.android.gms.common.api.Status;

public interface IGoogleServiceCommandListener {
    public void onCommandExecuted(CommandStatus status);
}
