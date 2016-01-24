package com.cowbell.cordova.geofence;

public class CommandStatus {
    public static CommandStatus SUCCESS = new CommandStatus(true);

    private boolean _isSuccess;
    private String message;

    public String getMessage() {
        return this.message;
    }
    public boolean isSuccess() {
        return this._isSuccess;
    }

    public CommandStatus(boolean isSuccess) {
        this._isSuccess = isSuccess;
    }

    public CommandStatus(boolean isSuccess, String message) {
        this._isSuccess = isSuccess;
        this.message = message;
    }
}
