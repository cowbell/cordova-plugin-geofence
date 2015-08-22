var exec = require('cordova/exec'),
    Promise = require('es6-promise-plugin'),
    channel = require('cordova/channel');

module.exports = {
    /**
     * Initializing geofence plugin
     *
     * @name initialize
     * @param  {Function} success callback
     * @param  {Function} error callback
     *
     * @return {Promise}
     */
    initialize: function (success, error) {
        return execPromise(success, error, 'GeofencePlugin', 'initialize', []);
    },
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
    addOrUpdate: function (geofences, success, error) {
        if (!Array.isArray(geofences)) {
            geofences = [geofences];
        }
        return execPromise(success, error, 'GeofencePlugin', 'addOrUpdate', geofences);
    },
    /**
     * Removing geofences with given ids
     *
     * @name  remove
     * @param  {Number|Array} ids
     * @param  {Function} success callback
     * @param  {Function} error callback
     * @return {Promise}
     */
    remove: function (ids, success, error) {
        if (!Array.isArray(ids)) {
            ids = [ids];
        }
        return execPromise(success, error, 'GeofencePlugin', 'remove', ids);
    },
    /**
     * removing all stored geofences on the device
     *
     * @name  removeAll
     * @param  {Function} success callback
     * @param  {Function} error callback
     * @return {Promise}
     */
    removeAll: function (success, error) {
        return execPromise(success, error, 'GeofencePlugin', 'removeAll', []);
    },
    /**
     * Getting all watched geofences from the device
     *
     * @name  getWatched
     * @param  {Function} success callback
     * @param  {Function} error callback
     * @return {Promise} if successful returns geofences array stringify to JSON
     */
    getWatched: function (success, error) {
        return execPromise(success, error, 'GeofencePlugin', 'getWatched', []);
    },
    /**
     * Called when app is opened via Notification bar
     *
     * @name onNotificationClicked
     * @param {JSON} notificationData user data from notification
     */
    onNotificationClicked: function (notificationData) {},
    /**
     * Called when app received geofence transition event
     * @param  {Array} geofences
     */
    onTransitionReceived: function (geofences) {
        this.receiveTransition(geofences);
    },
    /**
     * Called when app received geofence transition event
     * @deprecated since version 0.4.0, see onTransitionReceived
     * @param  {Array} geofences
     */
    receiveTransition: function (geofences) {},
    /**
     * Simple ping function for testing
     * @param  {Function} success callback
     * @param  {Function} error callback
     *
     * @return {Promise}
     */
    ping: function (success, error) {
        return execPromise(success, error, 'GeofencePlugin', 'ping', []);
    }
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
    exec(null, null, 'GeofencePlugin', 'deviceReady', []);
});
