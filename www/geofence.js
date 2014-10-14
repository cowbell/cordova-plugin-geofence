var exec = require("cordova/exec");
var Geofence = function () {

};

Geofence.prototype.addOrUpdate = function (geofences, success, error) {
    if (!Array.isArray(geofences)) {
        geofences = [geofences];
    }
    return execPromise(success, error, 'GeofencePlugin', 'addOrUpdate', geofences);
};

Geofence.prototype.initialize = function (success, error) {
    return execPromise(success, error, 'GeofencePlugin', 'initialize', []);
};

Geofence.prototype.remove = function (ids, success, error) {
    if (!Array.isArray(ids)) {
        ids = [ids];
    }
    return execPromise(success, error, 'GeofencePlugin', 'remove', ids);
};

Geofence.prototype.removeAll = function (success, error) {
    return execPromise(success, error, 'GeofencePlugin', 'removeAll', []);
};

Geofence.prototype.getWatched = function (success, error) {
    return execPromise(success, error, 'GeofencePlugin', 'getWatched', []);
};

Geofence.prototype.recieveTransition = function (geofences) {

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
};

var geofence = new Geofence();
module.exports = geofence;