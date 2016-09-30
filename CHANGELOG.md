# ChangeLog

## Version 0.6.0 - 15.04.2016

- Support for Android 6 new permission acquiring model
- Support for Cordova 6.0
- Fix for notification permissions on iOS 8
- Removed unnecessary WRITE_STORAGE permissions on Android
- initialize method fails when required permissions are not granted

## Version 0.5.0 - 09.11.2015

- Support for new Google API
- Support for Xcode 7.0, swift 2.0
- Android native code broadcast intent. [Details](https://github.com/cowbell/cordova-plugin-geofence#listening-for-geofence-transitions-in-native-code)
- iOS - using SwiftyJson instead of json.swift library
- Fixing received transition type for transitionType=BOTH. [Details](https://github.com/cowbell/cordova-plugin-geofence/issues/91)
- Parameters coercion. [Details](https://github.com/cowbell/cordova-plugin-geofence/issues/84)
- Fixed displaying location permission dialog only when `initialize` function is called. [Details](https://github.com/cowbell/cordova-plugin-geofence/issues/85)

## Version 0.4.2 - 02.08.2015

- fixed Promise bug

## Version 0.4.1 - 07.07.2015

- dependant plugins ids updated

## Version 0.4.0 - 06.05.2015

- Support for Xcode 6.3 and swift 1.2, swift < 1.2 is not supported
- Support for Cordova 5.0
- Add missing namespace decleration for M2 Windows Phone
- Notification for monitored region can be optional
- Vibrations on/off for iOS
- Vibration patterns for android

    ```
    //Vibrate for 1 sec
    //Wait for 0.5 sec
    //Vibrate for 2 sec
    notification: {
        vibrate: [1000, 500, 2000]
    }
    ```
- Custom notification icons for android

    ```
    notification: {
        smallIcon: 'res://my_location_icon',
        icon: 'file://img/geofence.png'
    }
    ```
- `onNotificationClicked` event
- `receiveTransition` event is deprecated see `onTransitionReceived`
- Google Support and Play Services load externally
