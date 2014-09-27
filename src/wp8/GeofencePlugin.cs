using Newtonsoft.Json.Linq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using WPCordovaClassLib;
using WPCordovaClassLib.Cordova.Commands;
using WPCordovaClassLib.Cordova.JSON;
using Windows.Devices.Geolocation.Geofencing;
using Windows.Devices.Geolocation;
using System.Windows.Threading;
using Windows.ApplicationModel.Background;
using Windows.UI.Notifications;
using Windows.Data.Xml.Dom;
using System.Diagnostics;
using com.cowbell.cordova.geofence.core;
using WPCordovaClassLib.Cordova;

namespace Cordova.Extension.Commands
{
    public class GeofencePlugin : BaseCommand
    {
        public async void initialize(string args)
        {
            Debug.WriteLine("Geofence plugin - initalize");
            Debug.WriteLine(GeofenceMonitor.Current.Geofences.Count);
            
            // Get permission for a background task from the user. If the user has already answered once,
            // this does nothing and the user must manually update their preference via PC Settings.
            BackgroundAccessStatus backgroundAccessStatus = await BackgroundExecutionManager.RequestAccessAsync();

            // Regardless of the answer, register the background task. If the user later adds this application
            // to the lock screen, the background task will be ready to run.
            // Create a new background task builder
            BackgroundTaskBuilder geofenceTaskBuilder = new BackgroundTaskBuilder();

            geofenceTaskBuilder.Name = "GeofenceTrigger";
            geofenceTaskBuilder.TaskEntryPoint = typeof(BackgroundTasks.GeofenceTrigger).ToString();

            // Create a new location trigger
            var trigger = new LocationTrigger(LocationTriggerType.Geofence);

            // Associate the locationi trigger with the background task builder
            geofenceTaskBuilder.SetTrigger(trigger);

            // If it is important that there is user presence and/or
            // internet connection when OnCompleted is called
            // the following could be called before calling Register()
            // SystemCondition condition = new SystemCondition(SystemConditionType.UserPresent | SystemConditionType.InternetAvailable);
            // geofenceTaskBuilder.AddCondition(condition);

            // Register the background task
            var geofenceTask = geofenceTaskBuilder.Register();

            switch (backgroundAccessStatus)
            {
                case BackgroundAccessStatus.Unspecified:
                case BackgroundAccessStatus.Denied:
                    //rootPage.NotifyUser("This application must be added to the lock screen before the background task will run.", NotifyType.ErrorMessage);
                    break;

            }
            DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }

        public void addOrUpdate(string args)
        {
            var geofences = JArray.Parse(args);

            foreach (JValue geofenceJV in geofences.Take(geofences.Count - 1))
            {
                var geofence = JObject.Parse(geofenceJV.ToString());
                
                var geoNotification = new GeoNotification{
                    Id = geofence["id"].Value<string>(),
                    Latitude = geofence["latitude"].Value<double>(),
                    Longitude = geofence["longitude"].Value<double>(),
                    Radius = geofence["radius"].Value<int>(),
                    TransitionType = geofence["transitionType"].Value<int>(),
                    NotificationText = geofence["notification"]["text"].Value<string>(),
                    NotificationTitle = geofence["notification"]["title"].Value<string>(),
                    OpenAppOnClick = geofence["notification"]["openAppOnClick"].Value<bool>(),
                    Data = geofence["notification"]["data"].ToString()
                };
                GeoNotificationManager.addOrUpdate(geoNotification);
            }
            DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }

        public void remove(string geofenceIds)
        {
            var ids = JArray.Parse(geofenceIds);

            foreach (JValue id in ids.Take(ids.Count - 1))
            {
                GeoNotificationManager.remove(id.ToString());
            }
            DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }

        public void removeAll(string args)
        {
            GeoNotificationManager.removeAll();
            DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }
    }
}
