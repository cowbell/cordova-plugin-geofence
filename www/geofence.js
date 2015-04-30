var exec = require('cordova/exec'),
    Promise = require('com.vladstirbu.cordova.promise.Promise'),
    channel = require('cordova/channel'),
    geofence,
    Geofence = function () {

    };

/**
 * Adding new geofence to monitor.
 * Geofence could override the previously one with the same id.
 *
 * @name addOrUpdate
 * @param {Geofence|Array} geofences
 * @param {Function} success callback
 * @param {Function} error callback
 *
 * @return {Promise}
 */
Geofence.prototype.addOrUpdate = function (geofences, success, error) {
    if (!Array.isArray(geofences)) {
        geofences = [geofences];
    }
    return execPromise(success, error, 'GeofencePlugin', 'addOrUpdate', geofences);
};

/**
 * Initializing geofence plugin
 *
 * @name initialize
 * @param  {Function} success callback
 * @param  {Function} error callback
 *
 * @return {Promise}
 */
Geofence.prototype.initialize = function (success, error) {
    return execPromise(success, error, 'GeofencePlugin', 'initialize', []);
};

/**
 * Removing geofences with given ids
 *
 * @name  remove
 * @param  {Number|Array} ids
 * @param  {Function} success callback
 * @param  {Function} error callback
 * @return {Promise}
 */
Geofence.prototype.remove = function (ids, success, error) {
    if (!Array.isArray(ids)) {
        ids = [ids];
    }
    return execPromise(success, error, 'GeofencePlugin', 'remove', ids);
};

/**
 * removing all stored geofences on the device
 *
 * @name  removeAll
 * @param  {Function} success callback
 * @param  {Function} error callback
 * @return {Promise}
 */
Geofence.prototype.removeAll = function (success, error) {
    return execPromise(success, error, 'GeofencePlugin', 'removeAll', []);
};

/**
 * Getting all watched geofences from the device
 *
 * @name  getWatched
 * @param  {Function} success callback
 * @param  {Function} error callback
 * @return {Promise} if successful returns geofences array stringify to JSON
 */
Geofence.prototype.getWatched = function (success, error) {
    return execPromise(success, error, 'GeofencePlugin', 'getWatched', []);
};

/**
 * Called when app is opened via Notification bar
 *
 * @name onNotificationClicked
 * @param {JSON} notificationData user data from notification
 */
Geofence.prototype.onNotificationClicked = function(notificationData){

};

function execPromise(success, error, pluginName, method, args) {
    return new Promise(function (resolve, reject) {
        exec(function (result) {
                resolve(result);
                if (typeof success === 'function') {
                    success(result);
                }
            },
            function (reason) {
                reject(reason);
                if (typeof error === 'function') {
                    error(reason);
                }
            },
            pluginName,
            method,
            args);
    });
}


// Called after 'deviceready' event
channel.deviceready.subscribe(function () {
    // Device is ready now, the listeners are registered
    // and all queued events can be executed.
    exec(null, null, 'GeofencePlugin', 'deviceready', []);
});

geofence = new Geofence();
module.exports = geofence;
