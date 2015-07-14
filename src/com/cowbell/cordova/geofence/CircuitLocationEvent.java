package com.cowbell.cordova.geofence;

import com.google.android.gms.location.Geofence;
import com.google.gson.annotations.Expose;

public class CircuitLocationEvent {
    @Expose private String location;
    @Expose private String type;
    @Expose private long timestamp;

    public CircuitLocationEvent() {}

    public CircuitLocationEvent(String location, int transitionType, long timestamp) {
        this.location = location;
        this.timestamp = timestamp;

        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                this.type = "enter";
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                this.type = "exit";
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                this.type = "dwelling";
                break;
        }
    }

    public String toJson() {
        return Gson.get().toJson(this);
    }
}
