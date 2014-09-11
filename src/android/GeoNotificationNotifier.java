package com.tsubik.cordova.geofence;

import org.json.JSONException;
import org.json.JSONObject;

import android.R;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.util.Log;
import android.support.v4.app.*;
import android.content.Context;
import android.content.Intent;

public class GeoNotificationNotifier {
	private NotificationManager notificationManager;
	private Context context;
	private BeepHelper beepHelper;
	
	public GeoNotificationNotifier(NotificationManager notificationManager, Context context){
		this.notificationManager = notificationManager;
		this.context = context;
		this.beepHelper = new BeepHelper();
	}
	
	public void notify(GeoNotification notification, boolean isEntered){
		NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
		    .setSmallIcon(R.drawable.ic_notification_overlay)
		    .setContentTitle(notification.getNotificationTitle())
		    .setContentText(notification.getNotificationText());
		// Creates an explicit intent for an Activity in your app
		//Intent resultIntent = new Intent(context, Donebytheway.class);
		
		if(notification.getOpenAppOnClick()){
			String packageName  = context.getPackageName();
			Intent resultIntent = context.getPackageManager().getLaunchIntentForPackage(packageName);
			
			resultIntent.putExtra("tsubik.geotask.loadTask", notification.getData().toString());
			// The stack builder object will contain an artificial back stack for the
			// started Activity.
			// This ensures that navigating backward from the Activity leads out of
			// your application to the Home screen.
			TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
			// Adds the back stack for the Intent (but not the Intent itself)
			//stackBuilder.addParentStack(Donebytheway.class);
			// Adds the Intent that starts the Activity to the top of the stack
			stackBuilder.addNextIntent(resultIntent);
			PendingIntent resultPendingIntent =
			        stackBuilder.getPendingIntent(
			            0,
			            PendingIntent.FLAG_UPDATE_CURRENT
			        );
			mBuilder.setContentIntent(resultPendingIntent);
		}
		beepHelper.startTone("beep_beep_beep");
		notificationManager.notify(100, mBuilder.build());
	}
}
