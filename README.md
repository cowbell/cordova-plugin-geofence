# Cordova Geofence Plugin

[![Code Climate](https://codeclimate.com/github/cowbell/cordova-plugin-geofence/badges/gpa.svg)](https://codeclimate.com/github/cowbell/cordova-plugin-geofence)

Plugin to monitor circular geofences using mobile devices. The purpose is to notify user if crossing the boundary of the monitored geofence.

*Geofences persists after device reboot. You do not have to open your app first to monitor added geofences*

## Installation

From master
```
cordova plugin add https://github.com/cowbell/cordova-plugin-geofence
```

## Removing the Plugin from project

Using cordova CLI

```
cordova plugin rm com.cowbell.cordova.geofence
```

## Supported Platforms

- Android
- iOS (coming soon)
- Windows Phone 8.1 
    - using Universal App (cordova windows platform)
    - using Silverlight App (cordova wp8 platform retargeted to WP 8.1)

# Using the plugin

Cordova initialize plugin to `window.geofence` object.

## Methods

- window.geofence.initialize(onSuccess, onError)
- window.geofence.addOrUpdate(geofences, onSuccess, onError)
- window.geofence.remove(onSuccess, onError)
- window.geofence.removeAll(onSuccess, onError)
- window.geofence.getWatched(onSuccess, onError)

All methods returning promises, but you can also use standard callback functions.

For listening of geofence transistion you can override receiveTransition method
- window.geofence.receiveTransition(geofences)

## Constants

- `TransitionType.ENTER` = 1
- `TransitionType.EXIT` = 2

## Plugin initialization

The plugin is not available until `deviceready` event is fired.

```javascript
document.addEventListener('deviceready', function () {
    // window.geofence is now available
    window.geofence.initialize();
}, false);
```

## Adding new geofence to monitor

```javascript
window.geofence.addOrUpdate({
    id:             String, //A unique identifier of geofence
    latitude:       Number, //Geo latitude of geofence
    longitude:      Number, //Geo longitude of geofence
    radius:         Number, //Radius of geofence in meters
    transitionType: Number, //Type of transition 1 - Enter, 2 - Exit
    notification: {         //Notification object
        id:             Number, //optional should be integer, id of notidication
        title:          String, //Title of notification
        text:           String, //Text of notification
        openAppOnClick: Boolean,//is main app activity should be opened after clicking on notification
        data:           Object  //Custom object associated with notification
    }
}).then(function () {
    console.log('Geofence successfully added');
}, function (reason) {
    console.log('Adding geofence failed', reason);
});
```
Adding more geofences at once
```javascript
window.geofence.addOrUpdate([geofence1, geofence2, geofence3]);
```

Geofence overrides the previously one with the same `id`. 

*All geofences are stored on the device and restored to monitor after device reboot.*

Notification overrides the previously one with the same `notification.id`.

## Removing 

Removing single geofence
```javascript
window.geofence.remove(geofenceId)
    .then(function () {
        console.log('Geofence sucessfully removed');
    }
    , function (reason){
        console.log('Removing geofence failed', reason);
    });
```
Removing more than one geofence at once.
```javascript
window.geofence.remove([geofenceId1, geofenceId2, geofenceId3]);
```

## Removing all geofences

```javascript
window.geofence.removeAll()
    .then(function () { 
        console.log('All geofences successfully removed.');
    }
    , function (reason) {
        console.log('Removing geofences failed', reason);
    });
```

## Getting watched geofences from device

```javascript
window.geofence.getWatched().then(function (geofencesJson) {
    var geofences = JSON.parse(geofencesJson);
});
```

## Listening for geofence transitions

```javascript
window.geofence.receiveTransition = function (geofences) {
    geofences.forEach(function (geo) {
        console.log('Geofence transition detected', geo);
    });
};
```

#Example usage

Adding geofence to monitor entering Gliwice city center area of radius 3km

```javascript
window.geofence.addOrUpdate({
    id:             "69ca1b88-6fbe-4e80-a4d4-ff4d3748acdb",
    latitude:       50.2980049, 
    longitude:      18.6593152, 
    radius:         3000, 
    transitionType: TransitionType.ENTER, 
    notification: {    
        id:             1,     
        title:          "Welcome in Gliwice", 
        text:           "You just arrived to Gliwice city center.",
        openAppOnClick: true
    }
}).then(function () {
    console.log('Geofence successfully added');
}, function (reason) {
    console.log('Adding geofence failed', reason);
})
```

#Example application

Check out our [example application](https://github.com/cowbell/ionic-geofence) built with ionic framework.

# Platform specifics

##Windows phone 8.1

Plugin can be used with both windows phone 8.1 type projects Univeral App, Silverlight App.

In order to use toast notifications you have to enable this feature in appxmanifest file either using UI in Visual Studio or edit file setting attribute **ToastCapable="true"** in **m3:VisualElements** node under Package/Applications/Application.

If you are retargeting WP 8.0 to WP 8.1 you need to register background task to perform geofence notifications. Register it via UI in Visual Studio or add following code under Package/Applications/Application/Extensions

```xml
<Extension Category="windows.backgroundTasks" EntryPoint="GeofenceComponent.GeofenceTrigger">
    <BackgroundTasks>
        <m2:Task Type="location" />
    </BackgroundTasks>
</Extension>
``` 

##License

This software is released under the [Apache 2.0 License](http://opensource.org/licenses/Apache-2.0).

© 2014 Cowbell-labs. All rights reserved
