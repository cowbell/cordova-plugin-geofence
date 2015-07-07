# ChangeLog

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
