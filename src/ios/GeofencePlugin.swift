//
//  GeofencePlugin.swift
//  ionic-geofence
//
//  Created by tomasz on 07/10/14.
//
//

import Foundation
import AudioToolbox

let TAG = "GeofencePlugin"
let iOS8 = floor(NSFoundationVersionNumber) > floor(NSFoundationVersionNumber_iOS_7_1)
let iOS7 = floor(NSFoundationVersionNumber) <= floor(NSFoundationVersionNumber_iOS_7_1)

func log(message: String){
    NSLog("%@ - %@", TAG, message)
}

@available(iOS 8.0, *)
@objc(HWPGeofencePlugin) class GeofencePlugin : CDVPlugin {
    var isDeviceReady: Bool = false
    let geoNotificationManager = GeoNotificationManager()
    let priority = DISPATCH_QUEUE_PRIORITY_DEFAULT

    override func pluginInitialize () {
        NSNotificationCenter.defaultCenter().addObserver(
            self,
            selector: "didReceiveLocalNotification:",
            name: "CDVLocalNotification",
            object: nil
        )

        NSNotificationCenter.defaultCenter().addObserver(
            self,
            selector: "didReceiveTransition:",
            name: "handleTransition",
            object: nil
        )
    }

    func initialize(command: CDVInvokedUrlCommand) {
        log("Plugin initialization")
        //let faker = GeofenceFaker(manager: geoNotificationManager)
        //faker.start()

        if iOS8 {
            promptForNotificationPermission()
        }
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }

    func ping(command: CDVInvokedUrlCommand) {
        log("Ping")
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }

    func promptForNotificationPermission() {
        UIApplication.sharedApplication().registerUserNotificationSettings(UIUserNotificationSettings(
            forTypes: [UIUserNotificationType.Sound, UIUserNotificationType.Alert, UIUserNotificationType.Badge],
            categories: nil
            )
        )
    }

    func addOrUpdate(command: CDVInvokedUrlCommand) {
        dispatch_async(dispatch_get_global_queue(priority, 0)) {
            // do some task
            for geo in command.arguments {
                self.geoNotificationManager.addOrUpdateGeoNotification(JSON(geo))
            }
            dispatch_async(dispatch_get_main_queue()) {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    func deviceReady(command: CDVInvokedUrlCommand) {
        isDeviceReady = true

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }

    func getWatched(command: CDVInvokedUrlCommand) {
        dispatch_async(dispatch_get_global_queue(priority, 0)) {
            let watched = self.geoNotificationManager.getWatchedGeoNotifications()!
            let watchedJsonString = watched.description
            dispatch_async(dispatch_get_main_queue()) {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsString: watchedJsonString)
                self.commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    func remove(command: CDVInvokedUrlCommand) {
        dispatch_async(dispatch_get_global_queue(priority, 0)) {
            for id in command.arguments {
                self.geoNotificationManager.removeGeoNotification(id as! String)
            }
            dispatch_async(dispatch_get_main_queue()) {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    func removeAll(command: CDVInvokedUrlCommand) {
        dispatch_async(dispatch_get_global_queue(priority, 0)) {
            self.geoNotificationManager.removeAllGeoNotifications()
            dispatch_async(dispatch_get_main_queue()) {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    func didReceiveTransition (notification: NSNotification) {
        log("didReceiveTransition")
        if let geoNotificationString = notification.object as? String {
            let geoNotification = JSON(geoNotificationString)
            var mustBeArray = [JSON]()
            mustBeArray.append(geoNotification)
            let js = "setTimeout('geofence.onTransitionReceived(" + mustBeArray.description + ")',0)"

            evaluateJs(js)
        }
    }

    func didReceiveLocalNotification (notification: NSNotification) {
        log("didReceiveLocalNotification")
        if UIApplication.sharedApplication().applicationState != UIApplicationState.Active {
            var data = "undefined"
            if let uiNotification = notification.object as? UILocalNotification {
                if let notificationData = uiNotification.userInfo?["geofence.notification.data"] as? String {
                    data = notificationData
                }
                let js = "setTimeout('geofence.onNotificationClicked(" + data + ")',0)"

                evaluateJs(js)
            }
        }
    }

    func evaluateJs (script: String) {
        if webView != nil {
            webView!.stringByEvaluatingJavaScriptFromString(script)
        } else {
            log("webView is null")
        }
    }
}

// class for faking crossing geofences
@available(iOS 8.0, *)
class GeofenceFaker {
    let priority = DISPATCH_QUEUE_PRIORITY_DEFAULT
    let geoNotificationManager: GeoNotificationManager

    init(manager: GeoNotificationManager) {
        geoNotificationManager = manager
    }

    func start() {
         dispatch_async(dispatch_get_global_queue(priority, 0)) {
            while (true) {
                log("FAKER")
                let notify = arc4random_uniform(4)
                if notify == 0 {
                    log("FAKER notify chosen, need to pick up some region")
                    var geos = self.geoNotificationManager.getWatchedGeoNotifications()!
                    if geos.count > 0 {
                        //WTF Swift??
                        let index = arc4random_uniform(UInt32(geos.count))
                        let geo = geos[Int(index)]
                        let id = geo["id"].asString!
                        dispatch_async(dispatch_get_main_queue()) {
                            if let region = self.geoNotificationManager.getMonitoredRegion(id) {
                                log("FAKER Trigger didEnterRegion")
                                self.geoNotificationManager.locationManager(
                                    self.geoNotificationManager.locationManager,
                                    didEnterRegion: region
                                )
                            }
                        }
                    }
                }
                NSThread.sleepForTimeInterval(3)
            }
         }
    }

    func stop() {

    }
}

@available(iOS 8.0, *)
class GeoNotificationManager : NSObject, CLLocationManagerDelegate {
    let locationManager = CLLocationManager()
    let store = GeoNotificationStore()

    override init() {
        log("GeoNotificationManager init")
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        if (!CLLocationManager.locationServicesEnabled()) {
            log("Location services is not enabled")
        } else {
            log("Location services enabled")
        }
        if iOS8 {
            locationManager.requestAlwaysAuthorization()
        }

        if (!CLLocationManager.isMonitoringAvailableForClass(CLRegion)) {
            log("Geofencing not available")
        }
    }

    func addOrUpdateGeoNotification(geoNotification: JSON) {
        log("GeoNotificationManager addOrUpdate")

        if (!CLLocationManager.locationServicesEnabled()) {
            log("Locationservices is not enabled")
        }

        let location = CLLocationCoordinate2DMake(
            geoNotification["latitude"].asDouble!,
            geoNotification["longitude"].asDouble!
        )
        log("AddOrUpdate geo: \(geoNotification)")
        let radius = geoNotification["radius"].asDouble! as CLLocationDistance
        //let uuid = NSUUID().UUIDString
        let id = geoNotification["id"].asString

        let region = CLCircularRegion(center: location, radius: radius, identifier: id!)
        
        var transitionType = 0
        if let i = geoNotification["transitionType"].asInt {
            transitionType = i
        }
        region.notifyOnEntry = 0 != transitionType & 1
        region.notifyOnExit = 0 != transitionType & 2

        //store
        store.addOrUpdate(geoNotification)
        locationManager.startMonitoringForRegion(region)
    }

    func getWatchedGeoNotifications() -> [JSON]? {
        return store.getAll()
    }

    func getMonitoredRegion(id: String) -> CLRegion? {
        for object in locationManager.monitoredRegions {
            let region = object 

            if (region.identifier == id) {
                return region
            }
        }
        return nil
    }

    func removeGeoNotification(id: String) {
        store.remove(id)
        var region = getMonitoredRegion(id)
        if (region != nil) {
            log("Stoping monitoring region \(id)")
            locationManager.stopMonitoringForRegion(region!)
        }
    }

    func removeAllGeoNotifications() {
        store.clear()
        for object in locationManager.monitoredRegions {
            let region = object 
            log("Stoping monitoring region \(region.identifier)")
            locationManager.stopMonitoringForRegion(region)
        }
    }

    func locationManager(manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        log("update location")
    }

    func locationManager(manager: CLLocationManager, didFailWithError error: NSError) {
        log("fail with error: \(error)")
    }

    func locationManager(manager: CLLocationManager, didFinishDeferredUpdatesWithError error: NSError?) {
        log("deferred fail error: \(error)")
    }

    func locationManager(manager: CLLocationManager, didEnterRegion region: CLRegion) {
        log("Entering region \(region.identifier)")
        handleTransition(region)
    }

    func locationManager(manager: CLLocationManager, didExitRegion region: CLRegion) {
        log("Exiting region \(region.identifier)")
        handleTransition(region)
    }

    func locationManager(manager: CLLocationManager, didStartMonitoringForRegion region: CLRegion) {
        let lat = (region as! CLCircularRegion).center.latitude
        let lng = (region as! CLCircularRegion).center.longitude
        let radius = (region as! CLCircularRegion).radius

        log("Starting monitoring for region \(region) lat \(lat) lng \(lng)")
    }

    func locationManager(manager: CLLocationManager, didDetermineState state: CLRegionState, forRegion region: CLRegion) {
        log("State for region " + region.identifier)
    }

    func locationManager(manager: CLLocationManager, monitoringDidFailForRegion region: CLRegion?, withError error: NSError) {
        log("Monitoring region " + region!.identifier + " failed " + error.description)
    }

    func handleTransition(region: CLRegion!) {
        if let geo = store.findById(region.identifier) {
            if let notification = geo["notification"].asDictionary {
                notifyAbout(geo)
            }

            NSNotificationCenter.defaultCenter().postNotificationName("handleTransition", object: geo.description)
        }
    }

    func notifyAbout(geo: JSON) {
        log("Creating notification")
        let notification = UILocalNotification()
        notification.timeZone = NSTimeZone.defaultTimeZone()
        let dateTime = NSDate()
        notification.fireDate = dateTime
        notification.soundName = UILocalNotificationDefaultSoundName
        notification.alertBody = geo["notification"]["text"].asString!
        if let json = geo["notification"]["data"] as? JSON {
            notification.userInfo = ["geofence.notification.data": json.description]
        }
        UIApplication.sharedApplication().scheduleLocalNotification(notification)

        if let vibrate = geo["notification"]["vibrate"].asArray {
            if (!vibrate.isEmpty && vibrate[0].asInt > 0) {
                AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
            }
        }
    }
}

class GeoNotificationStore {
    init() {
        createDBStructure()
    }

    func createDBStructure() {
        let (tables, err) = SD.existingTables()

        if (err != nil) {
            log("Cannot fetch sqlite tables: \(err)")
            return
        }

        if (tables.filter { $0 == "GeoNotifications" }.count == 0) {
            if let err = SD.executeChange("CREATE TABLE GeoNotifications (ID TEXT PRIMARY KEY, Data TEXT)") {
                //there was an error during this function, handle it here
                log("Error while creating GeoNotifications table: \(err)")
            } else {
                //no error, the table was created successfully
                log("GeoNotifications table was created successfully")
            }
        }
    }

    func addOrUpdate(geoNotification: JSON) {
        if (findById(geoNotification["id"].asString!) != nil) {
            update(geoNotification)
        }
        else {
            add(geoNotification)
        }
    }

    func add(geoNotification: JSON) {
        let id = geoNotification["id"].asString!
        let err = SD.executeChange("INSERT INTO GeoNotifications (Id, Data) VALUES(?, ?)",
            withArgs: [id, geoNotification.description])

        if err != nil {
            log("Error while adding \(id) GeoNotification: \(err)")
        }
    }

    func update(geoNotification: JSON) {
        let id = geoNotification["id"].asString!
        let err = SD.executeChange("UPDATE GeoNotifications SET Data = ? WHERE Id = ?",
            withArgs: [geoNotification.description, id])

        if err != nil {
            log("Error while adding \(id) GeoNotification: \(err)")
        }
    }

    func findById(id: String) -> JSON? {
        let (resultSet, err) = SD.executeQuery("SELECT * FROM GeoNotifications WHERE Id = ?", withArgs: [id])

        if err != nil {
            //there was an error during the query, handle it here
            log("Error while fetching \(id) GeoNotification table: \(err)")
            return nil
        } else {
            if (resultSet.count > 0) {
                return JSON(string: resultSet[0]["Data"]!.asString()!)
            }
            else {
                return nil
            }
        }
    }

    func getAll() -> [JSON]? {
        let (resultSet, err) = SD.executeQuery("SELECT * FROM GeoNotifications")

        if err != nil {
            //there was an error during the query, handle it here
            log("Error while fetching from GeoNotifications table: \(err)")
            return nil
        } else {
            var results = [JSON]()
            for row in resultSet {
                if let data = row["Data"]?.asString() {
                    results.append(JSON(string: data))
                }
            }
            return results
        }
    }

    func remove(id: String) {
        let err = SD.executeChange("DELETE FROM GeoNotifications WHERE Id = ?", withArgs: [id])

        if err != nil {
            log("Error while removing \(id) GeoNotification: \(err)")
        }
    }

    func clear() {
        let err = SD.executeChange("DELETE FROM GeoNotifications")

        if err != nil {
            log("Error while deleting all from GeoNotifications: \(err)")
        }
    }
}
