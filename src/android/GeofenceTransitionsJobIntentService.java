package com.cowbell.cordova.geofence;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import io.kiot.MainActivity;

/**
 * Listener for geofence transition changes.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class GeofenceTransitionsJobIntentService extends JobIntentService {

    private static final int JOB_ID = 573;
    protected GeoNotificationStore store;
    private static LocalStorage localStorage;

    private static final String TAG = "GeofenceTransitionsIS";

    private static final String CHANNEL_ID = "channel_01";
    public GeofenceTransitionsJobIntentService() {
        super();
        localStorage = new LocalStorage(this);
        store = new GeoNotificationStore(this);
        Logger.setLogger(new Logger(GeofencePlugin.TAG, this, false));
    }

    /**
     * Convenience method for enqueuing work in to this service.
     */
    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, GeofenceTransitionsJobIntentService.class, JOB_ID, intent);
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleWork(Intent intent) {
        Logger logger = Logger.getLogger();
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            logger.log(Log.DEBUG, "Geofence transition detected");
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();
            List<GeoNotification> geoNotifications = new ArrayList<GeoNotification>();
            for (Geofence fence : triggeringGeofences) {
                String fenceId = fence.getRequestId();
                GeoNotification geoNotification = store
                        .getGeoNotification(fenceId);

                if (geoNotification != null) {
                    if (geoNotification.notification != null) {
                        // notifier.notify(geoNotification.notification);
                    }
                    geoNotification.transitionType = geofenceTransition;
                    geoNotifications.add(geoNotification);
                }
            }

            if (geoNotifications.size() > 0) {
                //broadcastIntent.putExtra("transitionData", Gson.get().toJson(geoNotifications));
                try {

                    VolleyApi.tryRelogin(new VolleyCallback(){
                        @Override
                        public void onSuccess(JSONObject result) throws JSONException {
                            JSONObject userData = new JSONObject(localStorage.getItem("user"));
                            if(result.get("restype")=="success"){
                                VolleyApi.afterRelogin(true,result,userData);
                                onTransitionReceived(geoNotifications);
                            }
                        }
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } else {
            // Log the error.
            Log.e(TAG, "Geofence transition error: invalid transition type %1$d");
        }
    }

    public void onTransitionReceived(List<GeoNotification> notifications) throws JSONException {
        Log.d(TAG, "Transition Event Received!"+ notifications);
        for (int i=0; i<notifications.size();i++){
            String action = notifications.get(i).w_actions;
            GeoNotification notification = notifications.get(i);
            JSONArray obj = new JSONArray(action);
            final Boolean[] notifFlag = {false};
            for(int j=0; j<obj.length(); j++) {
                JSONObject act = obj.getJSONObject(j);
                if (act.get("type").equals("scene")) {
                    JSONObject scene_obj = new JSONObject();
                    scene_obj.put("sceneId", act.get("scene_id"));
                    VolleyApi.postTriggerScene(scene_obj, new VolleyCallback() {
                        @Override
                        public void onSuccess(JSONObject result) throws JSONException {
                            notifFlag[0] = true;
                           // sendNotification(notification);
                            //notifier.notify(notification);
                        }
                    });
                } else if (act.get("type").equals("switch")) {
                    JSONObject switch_obj = new JSONObject();
                    switch_obj.put("switch_no",act.get("switch_no"));
                    switch_obj.put("switch_state",act.get("switch_state"));
                    switch_obj.put("key",act.get("key"));
                    switch_obj.put("mobId",act.get("mobId"));
                    switch_obj.put("switch_appliance_id",act.get("switch_appliance_id"));
                    switch_obj.put("dimm_value",act.get("dimm_value"));
                    switch_obj.put("act_type",act.get("act_type"));
                    switch_obj.put("userdevice",act.get("userdevice"));
                    VolleyApi.postTriggerSwitch(switch_obj, new VolleyCallback() {
                        @Override
                        public void onSuccess(JSONObject result) throws JSONException {
                            notifFlag[0] = true;
                            //sendNotification(notification);
                           // notifier.notify(notification);
                        }
                    });
                }
            }
            if(notifFlag[0] == true){
                sendNotification(notification);
            }
        }

    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList<String> triggeringGeofencesIdsList = new ArrayList<>();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     * @param notificationDetails
     */
    private void sendNotification(GeoNotification notificationDetails) throws JSONException {
        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String description = "";
        if (notificationDetails.transitionType == Geofence.GEOFENCE_TRANSITION_ENTER){
             description = "Triggered "+notificationDetails.name ;
        }
        if (notificationDetails.transitionType == Geofence.GEOFENCE_TRANSITION_EXIT){
             description = "Triggered "+notificationDetails.name ;
        }

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Geofencing";
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }

        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder
                .setSmallIcon(_getResource("ic_launcher", "mipmap"))
//                 In a real app, you may want to use a library like Volley
//                 to decode the Bitmap.
                .setColor(Color.WHITE)
                .setContentTitle("Location Automation")
                .setContentText(description)
                .setContentIntent(notificationPendingIntent);

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Issue the notification
        mNotificationManager.notify((int)(Math.random()*(5000000-1000000+1)+1000000)  , builder.build());
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return "Entered";
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return "Exited";
            default:
                return "Unknown Transition";
        }
    }
    private int _getResource(String name, String type) {
        String package_name = getApplication().getPackageName();
        Resources resources = getApplication().getResources();
        return resources.getIdentifier(name, type, package_name);
    }
}
