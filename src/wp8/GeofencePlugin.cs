using GeofenceComponent;
using Newtonsoft.Json.Linq;
using System.Linq;
using WPCordovaClassLib.Cordova;
using WPCordovaClassLib.Cordova.Commands;

namespace Cordova.Extension.Commands
{
    public class GeofencePlugin : BaseCommand
    {
        public void initialize(string args)
        {
            GeofenceTrigger.Register();
            DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }

        public void addOrUpdate(string args)
        {
            var geofences = JArray.Parse(args);

            foreach (JValue geofenceJV in geofences.Take(geofences.Count - 1))
            {
                var geofence = JObject.Parse(geofenceJV.ToString());

                var geoNotification = new GeoNotification
                {
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
                GeoNotificationManager.AddOrUpdate(geoNotification);
            }
            DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }

        public void remove(string geofenceIds)
        {
            var ids = JArray.Parse(geofenceIds);

            foreach (JValue id in ids.Take(ids.Count - 1))
            {
                GeoNotificationManager.Remove(id.ToString());
            }
            DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }

        public void removeAll(string args)
        {
            GeoNotificationManager.RemoveAll();
            DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
        }
    }
}