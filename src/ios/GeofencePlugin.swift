//
//  GeofencePlugin.swift
//  ionic-geofence
//
//  Created by tomasz on 07/10/14.
//
//

import Foundation
import AudioToolbox
import WebKit

let TAG = "GeofencePlugin"
let iOS8 = floor(NSFoundationVersionNumber) > floor(NSFoundationVersionNumber_iOS_7_1)
let iOS7 = floor(NSFoundationVersionNumber) <= floor(NSFoundationVersionNumber_iOS_7_1)

typealias Callback = ([[String:String]]?) -> Void

func log(message: String){
    NSLog("%@ - %@", TAG, message)
}

func log(messages: [String]) {
    for message in messages {
        log(message);
    }
}

func log(errors: [[String:String]]) {
    for error in errors {
        log("\(error["code"]) - \(error["message"])");
    }
}

func checkRequirements() -> (Bool, [String], [[String:String]]) {
    var errors = [[String:String]]()
    var warnings = [String]()
    
    if (!CLLocationManager.isMonitoringAvailableForClass(CLRegion)) {
        errors.append([
            "code": GeofencePlugin.ERROR_GEOFENCE_NOT_AVAILABLE,
            "message": "Geofencing not available"
        ])
    }
    
    if (!CLLocationManager.locationServicesEnabled()) {
        errors.append([
            "code": GeofencePlugin.ERROR_LOCATION_SERVICES_DISABLED,
            "message": "Locationservices disabled"
        ])
    }
    
    let authStatus = CLLocationManager.authorizationStatus()
    
    if (authStatus != CLAuthorizationStatus.AuthorizedAlways) {
        errors.append([
            "code": GeofencePlugin.ERROR_PERMISSION_DENIED,
            "message": "Location always permissions not granted"
        ])
    }
    
    if (iOS8) {
        if let notificationSettings = UIApplication.sharedApplication().currentUserNotificationSettings() {
            if notificationSettings.types == .None {
                errors.append([
                    "code": GeofencePlugin.ERROR_PERMISSION_DENIED,
                    "message": "Notification permission missing"
                ])
            } else {
                if !notificationSettings.types.contains(.Sound) {
                    warnings.append("Warning: notification settings - sound permission missing")
                }
                
                if !notificationSettings.types.contains(.Alert) {
                    warnings.append("Warning: notification settings - alert permission missing")
                }
                
                if !notificationSettings.types.contains(.Badge) {
                    warnings.append("Warning: notification settings - badge permission missing")
                }
            }
        } else {
            errors.append([
                "code": GeofencePlugin.ERROR_PERMISSION_DENIED,
                "message": "Notification permission missing"
            ])
        }
    }
    
    let ok = (errors.count == 0)
    
    return (ok, warnings, errors)
}

@available(iOS 8.0, *)
@objc(HWPGeofencePlugin) class GeofencePlugin : CDVPlugin {
    static let ERROR_GEOFENCE_LIMIT_EXCEEDED = "GEOFENCE_LIMIT_EXCEEDED"
    static let ERROR_GEOFENCE_NOT_AVAILABLE = "GEOFENCE_NOT_AVAILABLE"
    static let ERROR_LOCATION_SERVICES_DISABLED = "LOCATION_SERVICES_DISABLED"
    static let ERROR_PERMISSION_DENIED = "PERMISSION_DENIED"
    static let ERROR_UNKNOWN = "UNKNOWN"
    
    lazy var geoNotificationManager = GeoNotificationManager()
    let priority = DISPATCH_QUEUE_PRIORITY_DEFAULT

    override func pluginInitialize () {
        NSNotificationCenter.defaultCenter().addObserver(
            self,
            selector: #selector(GeofencePlugin.didReceiveLocalNotification(_:)),
            name: "CDVLocalNotification",
            object: nil
        )

        NSNotificationCenter.defaultCenter().addObserver(
            self,
            selector: #selector(GeofencePlugin.didReceiveTransition(_:)),
            name: "handleTransition",
            object: nil
        )
    }

    func initialize(command: CDVInvokedUrlCommand) {
        log("Plugin initialization")

        if iOS8 {
            promptForNotificationPermission()
        }

        geoNotificationManager = GeoNotificationManager()
        geoNotificationManager.registerPermissions()

        let (ok, warnings, errors) = checkRequirements()

        log(warnings)
        log(errors)

        let result: CDVPluginResult

        if ok {
            result = CDVPluginResult(status: CDVCommandStatus_OK, messageAsString: warnings.joinWithSeparator("\n"))
        } else {
            result = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAsDictionary: errors.first
            )
        }

        commandDelegate!.sendPluginResult(result, callbackId: command.callbackId)
    }

    func deviceReady(command: CDVInvokedUrlCommand) {
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
            let geo = command.arguments[0]
            self.geoNotificationManager.addOrUpdateGeoNotification(JSON(geo), completion: {
                (errors: [[String:String]]?) -> Void in
                
                dispatch_async(dispatch_get_main_queue()) {
                    var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                    if (errors != nil) {
                        pluginResult = CDVPluginResult(
                            status: CDVCommandStatus_ERROR,
                            messageAsDictionary: errors!.first
                        )
                    }
                    self.commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
                }
            })
        }
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

            let js = "setTimeout('geofence.onTransitionReceived([" + geoNotificationString + "])',0)"

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
        if let webView = webView {
            if let uiWebView = webView as? UIWebView {
                uiWebView.stringByEvaluatingJavaScriptFromString(script)
            } else if let wkWebView = webView as? WKWebView {
                wkWebView.evaluateJavaScript(script, completionHandler: nil)
            }
        } else {
            log("webView is nil")
        }
    }
}

struct Command {
    var geoNotification: JSON
    var callback: Callback
}

@available(iOS 8.0, *)
class GeoNotificationManager : NSObject, CLLocationManagerDelegate {
    let locationManager = CLLocationManager()
    let store = GeoNotificationStore()
    var addOrUpdateCallbacks = [CLCircularRegion:Command]()

    override init() {
        log("GeoNotificationManager init")
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func registerPermissions() {
        if iOS8 {
            locationManager.requestAlwaysAuthorization()
        }
    }

    func addOrUpdateGeoNotification(geoNotification: JSON, completion: Callback) {
        log("GeoNotificationManager addOrUpdate")

        let (ok, warnings, errors) = checkRequirements()

        log(warnings)
        log(errors)
        
        if (!ok) {
            return completion(errors)
        }

        let location = CLLocationCoordinate2DMake(
            geoNotification["latitude"].doubleValue,
            geoNotification["longitude"].doubleValue
        )
        log("AddOrUpdate geo: \(geoNotification)")
        let radius = geoNotification["radius"].doubleValue as CLLocationDistance
        let id = geoNotification["id"].stringValue

        let region = CLCircularRegion(center: location, radius: radius, identifier: id)

        var transitionType = 0
        if let i = geoNotification["transitionType"].int {
            transitionType = i
        }
        region.notifyOnEntry = 0 != transitionType & 1
        region.notifyOnExit = 0 != transitionType & 2

        let command = Command(geoNotification: geoNotification, callback: completion)
        addOrUpdateCallbacks[region] = command
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
        let region = getMonitoredRegion(id)
        if (region != nil) {
            log("Stop monitoring region \(id)")
            locationManager.stopMonitoringForRegion(region!)
        }
    }

    func removeAllGeoNotifications() {
        store.clear()
        for object in locationManager.monitoredRegions {
            let region = object
            log("Stop monitoring region \(region.identifier)")
            locationManager.stopMonitoringForRegion(region)
        }
    }

    func locationManager(manager: CLLocationManager, didEnterRegion region: CLRegion) {
        log("Entering region \(region.identifier)")
        handleTransition(region, transitionType: 1)
    }

    func locationManager(manager: CLLocationManager, didExitRegion region: CLRegion) {
        log("Exiting region \(region.identifier)")
        handleTransition(region, transitionType: 2)
    }

    func locationManager(manager: CLLocationManager, didStartMonitoringForRegion region: CLRegion) {
        if let clRegion = region as? CLCircularRegion {
            if let command = self.addOrUpdateCallbacks[clRegion] {
                store.addOrUpdate(command.geoNotification)
                log("Starting monitoring for region \(region.identifier)")
                command.callback(nil)
                self.addOrUpdateCallbacks.removeValueForKey(clRegion)
            }
        }
    }

    func locationManager(manager: CLLocationManager, monitoringDidFailForRegion region: CLRegion?, withError error: NSError) {
        log("Monitoring region \(region!.identifier) failed. Reson: \(error.description)")
        if let clRegion = region as? CLCircularRegion {
            if let command = self.addOrUpdateCallbacks[clRegion] {
                var errors = [[String:String]]()
                if locationManager.monitoredRegions.count >= 20 {
                    errors.append([
                        "code": GeofencePlugin.ERROR_GEOFENCE_LIMIT_EXCEEDED,
                        "message": error.description
                    ])
                } else {
                    errors.append([
                        "code": GeofencePlugin.ERROR_UNKNOWN,
                        "message": error.description
                    ])
                }
                
                command.callback(errors)
                self.addOrUpdateCallbacks.removeValueForKey(clRegion)
            }
        }
    }

    func handleTransition(region: CLRegion!, transitionType: Int) {
        if var geoNotification = store.findById(region.identifier) {
            geoNotification["transitionType"].int = transitionType

            if geoNotification["notification"].isExists() {
                notifyAbout(geoNotification)
            }

            NSNotificationCenter.defaultCenter().postNotificationName("handleTransition", object: geoNotification.rawString(NSUTF8StringEncoding, options: []))
        }
    }

    func notifyAbout(geo: JSON) {
        log("Creating notification")
        let notification = UILocalNotification()
        notification.timeZone = NSTimeZone.defaultTimeZone()
        let dateTime = NSDate()
        notification.fireDate = dateTime
        notification.soundName = UILocalNotificationDefaultSoundName
        notification.alertBody = geo["notification"]["text"].stringValue
        if let json = geo["notification"]["data"] as JSON? {
            notification.userInfo = ["geofence.notification.data": json.rawString(NSUTF8StringEncoding, options: [])!]
        }
        UIApplication.sharedApplication().scheduleLocalNotification(notification)

        if let vibrate = geo["notification"]["vibrate"].array {
            if (!vibrate.isEmpty && vibrate[0].intValue > 0) {
                AudioServicesPlayAlertSound(SystemSoundID(kSystemSoundID_Vibrate))
            }
        }
    }
}

// TODO: pass errors to cordova application
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
        if (findById(geoNotification["id"].stringValue) != nil) {
            update(geoNotification)
        }
        else {
            add(geoNotification)
        }
    }

    func add(geoNotification: JSON) {
        let id = geoNotification["id"].stringValue
        let err = SD.executeChange("INSERT INTO GeoNotifications (Id, Data) VALUES(?, ?)",
            withArgs: [id, geoNotification.description])

        if err != nil {
            log("Error while adding \(id) GeoNotification: \(err)")
        }
    }

    func update(geoNotification: JSON) {
        let id = geoNotification["id"].stringValue
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
                let jsonString = resultSet[0]["Data"]!.asString()!
                return JSON(data: jsonString.dataUsingEncoding(NSUTF8StringEncoding)!)
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
                    results.append(JSON(data: data.dataUsingEncoding(NSUTF8StringEncoding)!))
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
