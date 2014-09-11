var exec = require("cordova/exec");

var Geofence = function() {
    
};

Geofence.prototype.addOrUpdate = function(geofences){
    if(!Array.isArray(geofences)){
        geofences = [geofences];
    }
    return new Promise(function(resolve, reject) {
        exec(function(result){
            resolve(result);
        },
        function(reason){
            reject(reason);
        },
        'GeofencePlugin',
        'addOrUpdate',
        geofences);
    });    
}

Geofence.prototype.remove = function(ids){
    if(!Array.isArray(ids)){
        ids = [ids];
    }
    return new Promise(function(resolve, reject) {
        exec(function(result){
            resolve(result);
        },
        function(reason){
            reject(reason);
        },
        'GeofencePlugin',
        'remove',
        ids);
    });    
}

Geofence.prototype.removeAll = function(){
    return new Promise(function(resolve, reject) {
        exec(function(result){
            resolve(result);
        },
        function(reason){
            reject(reason);
        },
        'GeofencePlugin',
        'removeAll',
        []);
    });
}

var geofence = new Geofence();
module.exports = geofence;