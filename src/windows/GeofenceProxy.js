module.exports = {
    initialize: function (success, fail, args, env) {
        GeofenceComponent.GeofenceTrigger.register();
        success && success();
    },

    addOrUpdate: function (success, fail, args, env) {
        args.forEach(function (geo) {
            var geoN = new GeofenceComponent.GeoNotification();
            geoN.id = geo.id;
            geoN.latitude = geo.latitude;
            geoN.longitude = geo.longitude;
            geoN.transitionType = geo.transitionType;
            geoN.radius = geo.radius;
            geoN.notificationText = geo.notification.text;
            geoN.notificationTitle = geo.notification.title;
            geoN.openAppOnClick = geo.notification.openAppOnClick;
            geoN.data = JSON.stringify(geo.notification.data);
            GeofenceComponent.GeoNotificationManager.addOrUpdate(geoN);
        });
        success && success();
    },

    remove: function (success, fail, args, env) {
        args.forEach(function (geoId) {
            GeofenceComponent.GeoNotificationManager.remove(geoId);
        });
        success && success();
    },

    removeAll: function (success, fail, args, env) {
        GeofenceComponent.GeoNotificationManager.removeAll();
        success && success();
    }
};

require("cordova/exec/proxy").add("GeofencePlugin", module.exports);
