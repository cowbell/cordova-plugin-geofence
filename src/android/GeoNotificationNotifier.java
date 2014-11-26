package com.cowbell.cordova.geofence;

import android.R;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

public class GeoNotificationNotifier {
    private NotificationManager notificationManager;
    private Context context;
    private BeepHelper beepHelper;
    private Logger logger;

    public GeoNotificationNotifier(NotificationManager notificationManager,
            Context context) {
        this.notificationManager = notificationManager;
        this.context = context;
        this.beepHelper = new BeepHelper();
        this.logger = Logger.getLogger();
    }

    public void notify(Notification notification, boolean isEntered) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(
                context).setSmallIcon(R.drawable.ic_menu_mylocation)
                .setVibrate(new long[] { 1000, 1000, 1000 })
                .setAutoCancel(true).setContentTitle(notification.title)
                .setContentText(notification.text);

        if (notification.openAppOnClick) {
            String packageName = context.getPackageName();
            Intent resultIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(packageName);

            Object extraData = notification.data;
            if (extraData != null) {
                resultIntent.putExtra("geofence.notification.data",
                        extraData.toString());
            }
            // The stack builder object will contain an artificial back stack
            // for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out
            // of
            // your application to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
            // Adds the back stack for the Intent (but not the Intent itself)
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(resultIntent);
            PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                    0, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
        }
        beepHelper.startTone("beep_beep_beep");
        notificationManager.notify(notification.id, mBuilder.build());
        logger.log(Log.DEBUG, "GeoNotification title: " + notification.title
                + " text: " + notification.text);
    }
}
