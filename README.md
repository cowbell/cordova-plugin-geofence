# Cordova Geofence Plugin

Plugin to monitor circular geofences using mobile devices. The purpose is to notify user if crossing the boundary of the monitored geofence.

## Installation

From master
```
cordova plugin add  
```

## Removing the Plugin from project

Using cordova CLI

```
cordova plugin rm com.tsubik.cordova.geofence
```

## Supported Platforms

- Android
- iOS (comming soon)
- Windows Phone (comming soon)

# Using the plugin

Cordova initialize plugin to `window.geofence` object.

## Methods

- window.geofence.addOrUpdate
- window.geofence.remove
- window.geofence.removeAll

All methods returing promises.

## Plugin initialization

The plugin is not available until `deviceready` event is fired.

```javascript
document.addEventListener('deviceready', function () {
    // window.geofence is now available
}, false);
```

## Adding new geofence to monitor

```javascript
window.geofence.addOrUpdate({
    id:             String, //A unique identifier of geofence
    latitude:       Number, //Geo latitude of geofence
    longitude:      Number, //Geo longitude of geofence
    radius:         Number, //Radius of geofence in meters
    transitionType: Number, //Type of transition 1 - Enter, 0 - Exit
    notification: {         //Notification object
        title:          String, //Title of notification
        text:           String, //Text of notification
        openAppOnClick: Boolean,//is main app activity should be opened after clicking on notification
        data:           Object  //Custom object associated with notification
    }
}).done(function(){
    console.log('Geofence successfully added');
}).fail(function(reason){
    console.log('Adding geofence failed', reason);
})
```
Adding more geofences at once
```javascript
window.geofence.addOrUpdate([geofence1, geofence2, geofence3]);
```

Geofence could override the previously one with the same `id`. 

*All geofences are stored on the device and restored to monitor after device reboot.*

## Removing 

Removing single geofence
```javascript
window.geofence.remove(geofenceId)
    .done(function(){
        console.log('Geofence sucessfully removed')
    })
    .fail(function(reason){
        console.log('Removing geofence failed', reason)
    })
```
Removing more than one geofence at once.
```javascript
window.geofence.remove([geofenceId1, geofenceId2, geofenceId3]);
```

## Removing all geofences

```javascript
window.geofence.removeAll()
    .done(function(){ 
        console.log('All geofences successfully removed.');
    })
    .fail(function(reason){
        console.log('Removing geofences failed', reason);
    })
```

#Example

Adding geofence to monitor entering Gliwice city center area of radius 3km

```javascript
window.geofence.addOrUpdate({
    id:             "69ca1b88-6fbe-4e80-a4d4-ff4d3748acdb",
    latitude:       50.2980049, 
    longitude:      18.6593152, 
    radius:         3000, 
    transitionType: 1, 
    notification: {         
        title:          "Welcome in Gliwice", 
        text:           "You just arrived to Gliwice city center.",
        openAppOnClick: true
    }
}).done(function(){
    console.log('Geofence successfully added');
}).fail(function(reason){
    console.log('Adding geofence failed', reason);
})
```

##License

This software is released under the [Apache 2.0 License](http://opensource.org/licenses/Apache-2.0).

© 2014 Tomasz Subik. All rights reserved