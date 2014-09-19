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

namespace Cordova.Extension.Commands
{
    public class GeofencePlugin : BaseCommand
    {
        public void addOrUpdate(string args)
        {
            var geofences = JArray.Parse(args);
            foreach (dynamic geofence in geofences)
            {
                CreateGeofence(geofence.id, geofence.latitude, geofence.longitude, geofence.radius, geofence.transitionType);
            }
        }

        public void remove(string geofenceIds)
        {

        }

        public void removeAll()
        {

        }
        private void CreateGeofence(string id, double lat, double lng, double radius, int transitionType)
        {
            Geofence geofence = null;

            BasicGeoposition position;
            position.Latitude = lat;
            position.Longitude = lng;
            position.Altitude = 0.0;
            
            // the geofence is a circular region
            Geocircle geocircle = new Geocircle(position, radius);

            // want to listen for enter geofence, exit geofence and remove geofence events
            // you can select a subset of these event states
            MonitoredGeofenceStates mask = 0;

            if (transitionType == 1)
            {
                mask |= MonitoredGeofenceStates.Entered;
            }
            else
            {
                mask |= MonitoredGeofenceStates.Exited;
            }
            mask |= MonitoredGeofenceStates.Removed;

            // setting up how long you need to be in geofence for enter event to fire
            TimeSpan dwellTime = TimeSpan.FromSeconds(10);
            TimeSpan duration = TimeSpan.FromDays(365);

            // setting up the start time of the geofence
            DateTimeOffset startTime = DateTimeOffset.Now;

            geofence = new Geofence(id, geocircle, mask, false, dwellTime, startTime, duration);
            GeofenceMonitor.Current.Geofences.Add(geofence);
        }


    }
}
