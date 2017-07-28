var exec = require("cordova/exec");
var channel = require("cordova/channel");

var isIOS = cordova.platformId === "ios"

function addOrUpdateIOS (geofences, success, error) {
    var promises = geofences.map(function (geofence) {
        return execPromise(null, null, "GeofencePlugin", "addOrUpdate", [geofence]);
    });

    return Promise
        .all(promises)
        .then(function (results) {
            if (typeof success === "function") {
                success(results);
            }
            return results;
        })
        .catch(function (reason) {
            if (typeof error === "function") {
                error(reason);
            }
            throw reason;
        });
}

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
        return execPromise(success, error, "GeofencePlugin", "initialize", []);
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

        geofences.forEach(coerceProperties);

        if (isIOS) {
            return addOrUpdateIOS(geofences, success, error);
        }

        return execPromise(success, error, "GeofencePlugin", "addOrUpdate", geofences);
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
        return execPromise(success, error, "GeofencePlugin", "remove", ids);
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
        return execPromise(success, error, "GeofencePlugin", "removeAll", []);
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
        return execPromise(success, error, "GeofencePlugin", "getWatched", []);
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
        return execPromise(success, error, "GeofencePlugin", "ping", []);
    }
};

function execPromise(success, error, pluginName, method, args) {
    return new Promise(function (resolve, reject) {
        exec(function (result) {
                resolve(result);
                if (typeof success === "function") {
                    success(result);
                }
            },
            function (reason) {
                reject(reason);
                if (typeof error === "function") {
                    error(reason);
                }
            },
            pluginName,
            method,
            args);
    });
}

function coerceProperties(geofence) {
    if (geofence.id) {
        geofence.id = geofence.id.toString();
    } else {
        throw new Error("Geofence id is not provided");
    }

    if (geofence.latitude) {
        geofence.latitude = coerceNumber("Geofence latitude", geofence.latitude);
    } else {
        throw new Error("Geofence latitude is not provided");
    }

    if (geofence.longitude) {
        geofence.longitude = coerceNumber("Geofence longitude", geofence.longitude);
    } else {
        throw new Error("Geofence longitude is not provided");
    }

    if (geofence.radius) {
        geofence.radius = coerceNumber("Geofence radius", geofence.radius);
    } else {
        throw new Error("Geofence radius is not provided");
    }

    if (geofence.transitionType) {
        geofence.transitionType = coerceNumber("Geofence transitionType", geofence.transitionType);
    } else {
        throw new Error("Geofence transitionType is not provided");
    }

    if (geofence.notification) {
        if (geofence.notification.id) {
            geofence.notification.id = coerceNumber("Geofence notification.id", geofence.notification.id);
        }

        if (geofence.notification.title) {
            geofence.notification.title = geofence.notification.title.toString();
        }

        if (geofence.notification.text) {
            geofence.notification.text = geofence.notification.text.toString();
        }

        if (geofence.notification.smallIcon) {
            geofence.notification.smallIcon = geofence.notification.smallIcon.toString();
        }

        if (geofence.notification.openAppOnClick) {
            geofence.notification.openAppOnClick = coerceBoolean("Geofence notification.openAppOnClick", geofence.notification.openAppOnClick);
        }

        if (geofence.notification.vibration) {
            if (Array.isArray(geofence.notification.vibration)) {
                for (var i=0; i<geofence.notification.vibration.length; i++) {
                    geofence.notification.vibration[i] = coerceInteger("Geofence notification.vibration["+ i +"]", geofence.notification.vibration[i]);
                }
            } else {
                throw new Error("Geofence notification.vibration is not an Array");
            }
        }
    }
}

function coerceNumber(name, value) {
    if (typeof(value) !== "number") {
        console.warn(name + " is not a number, trying to convert to number");
        value = Number(value);

        if (isNaN(value)) {
            throw new Error("Cannot convert " + name + " to number");
        }
    }

    return value;
}

function coerceInteger(name, value) {
    if (!isInt(value)) {
        console.warn(name + " is not an integer, trying to convert to integer");
        value = parseInt(value);

        if (isNaN(value)) {
            throw new Error("Cannot convert " + name + " to integer");
        }
    }

    return value;
}

function coerceBoolean(name, value) {
    if (typeof(value) !== "boolean") {
        console.warn(name + " is not a boolean value, converting to boolean");
        value = Boolean(value);
    }

    return value;
}

function isInt(n){
    return Number(n) === n && n % 1 === 0;
}

// Called after "deviceready" event
channel.deviceready.subscribe(function () {
    // Device is ready now, the listeners are registered
    // and all queued events can be executed.
    exec(null, null, "GeofencePlugin", "deviceReady", []);
});
