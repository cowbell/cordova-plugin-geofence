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
        log(message: message);
    }
}

func log(errors: [[String:String]]) {
    for error in errors {
        //log("\(error["code"]) - \(error["message"])");
        log(message: "\(error["code"]) - \(error["message"])");
    }
}

func checkRequirements() -> (Bool, [String], [[String:String]]) {
    var errors = [[String:String]]()
    var warnings = [String]()
    
    if (!CLLocationManager.isMonitoringAvailable(for: CLRegion.self)) {
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
    
    if (authStatus != CLAuthorizationStatus.authorizedAlways) {
        errors.append([
            "code": GeofencePlugin.ERROR_PERMISSION_DENIED,
            "message": "Location always permissions not granted"
        ])
    }
    
    if (iOS8) {
        if let notificationSettings = UIApplication.shared.currentUserNotificationSettings {
            if notificationSettings.types == .none {
                errors.append([
                    "code": GeofencePlugin.ERROR_PERMISSION_DENIED,
                    "message": "Notification permission missing"
                ])
            } else {
                if !notificationSettings.types.contains(.sound) {
                    warnings.append("Warning: notification settings - sound permission missing")
                }
                
                if !notificationSettings.types.contains(.alert) {
                    warnings.append("Warning: notification settings - alert permission missing")
                }
                
                if !notificationSettings.types.contains(.badge) {
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
    let priority = DispatchQueue.GlobalQueuePriority.default

    override func pluginInitialize () {
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(GeofencePlugin.didReceiveLocalNotification(notification:)),
            name: NSNotification.Name(rawValue: "CDVLocalNotification"),
            object: nil
        )

        NotificationCenter.default.addObserver(
            self,
            selector: #selector(GeofencePlugin.didReceiveTransition(notification:)),
            name: NSNotification.Name(rawValue: "handleTransition"),
            object: nil
        )
    }

    @objc(initialize:)
    func initialize(command: CDVInvokedUrlCommand) {
        log(message: "Plugin initialization")

        if iOS8 {
            promptForNotificationPermission()
        }

        geoNotificationManager = GeoNotificationManager()
        geoNotificationManager.registerPermissions()

        let (ok, warnings, errors) = checkRequirements()

        log(messages: warnings)
        log(errors: errors)
        

        let result: CDVPluginResult

        if ok {
            result = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: warnings.joined(separator: "\n"))
        } else {
            result = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: errors.first
            )
        }

        commandDelegate!.send(result, callbackId: command.callbackId)
    }

    @objc(deviceReady:)
    func deviceReady(command: CDVInvokedUrlCommand) {
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate!.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(ping:)
    func ping(command: CDVInvokedUrlCommand) {
        log(message: "Ping")
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate!.send(pluginResult, callbackId: command.callbackId)
    }

    @objc(promptForNotificationPermission)
    func promptForNotificationPermission() {
        UIApplication.shared.registerUserNotificationSettings(UIUserNotificationSettings(
            types: [UIUserNotificationType.sound, UIUserNotificationType.alert, UIUserNotificationType.badge],
            categories: nil
            )
        )
    }

    @objc(addOrUpdate:)
    func addOrUpdate(command: CDVInvokedUrlCommand) {
        //dispatch_async(dispatch_get_global_queue(priority, 0)) {
        DispatchQueue.global(qos: .default).async {
            let geo = command.arguments[0]
            self.geoNotificationManager.addOrUpdateGeoNotification(geoNotification: JSON(geo), completion: {
                (errors: [[String:String]]?) -> Void in
                
                //dispatch_async(dispatch_get_main_queue()) {
                DispatchQueue.main.async {
                    var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                    if (errors != nil) {
                        pluginResult = CDVPluginResult(
                            status: CDVCommandStatus_ERROR,
                            messageAs: errors!.first
                        )
                    }
                    self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
                }
            })
        }
    }

    @objc(getWatched:)
    func getWatched(command: CDVInvokedUrlCommand) {
        //dispatch_async(dispatch_get_global_queue(priority, 0)) {
        DispatchQueue.global(qos: .background).async {
            let watched = self.geoNotificationManager.getWatchedGeoNotifications()!
            let watchedJsonString = watched.description
            //dispatch_async(dispatch_get_main_queue()) {
            DispatchQueue.main.async {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAs: watchedJsonString)
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    @objc(remove:)
    func remove(command: CDVInvokedUrlCommand) {
        //dispatch_async(dispatch_get_global_queue(priority, 0)) {
        DispatchQueue.global(qos: .background).async {
            for id in command.arguments {
                self.geoNotificationManager.removeGeoNotification(id: id as! String)
            }
            //dispatch_async(dispatch_get_main_queue()) {
            DispatchQueue.main.async {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    @objc(removeAll:)
    func removeAll(command: CDVInvokedUrlCommand) {
        //dispatch_async(dispatch_get_global_queue(priority, 0)) {
        DispatchQueue.global(qos: .background).async {
            self.geoNotificationManager.removeAllGeoNotifications()
            //dispatch_async(dispatch_get_main_queue()) {
            DispatchQueue.main.async {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate!.send(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    @objc(didReceiveTransition:)
    func didReceiveTransition (notification: NSNotification) {
        log(message: "didReceiveTransition")
        if let geoNotificationString = notification.object as? String {

            let js = "setTimeout('geofence.onTransitionReceived([" + geoNotificationString + "])',0)"

            evaluateJs(script: js)
        }
    }

    @objc(didReceiveLocalNotification:)
    func didReceiveLocalNotification (notification: NSNotification) {
        log(message: "didReceiveLocalNotification")
        if UIApplication.shared.applicationState != UIApplicationState.active {
            var data = "undefined"
            if let uiNotification = notification.object as? UILocalNotification {
                if let notificationData = uiNotification.userInfo?["geofence.notification.data"] as? String {
                    data = notificationData
                }
                let js = "setTimeout('geofence.onNotificationClicked(" + data + ")',0)"

                evaluateJs(script: js)
            }
        }
    }

    func evaluateJs (script: String) {
        if let webView = webView {
            if let uiWebView = webView as? UIWebView {
                uiWebView.stringByEvaluatingJavaScript(from: script)
            } else if let wkWebView = webView as? WKWebView {
                wkWebView.evaluateJavaScript(script, completionHandler: nil)
            }
        } else {
            log(message: "webView is nil")
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
        log(message: "GeoNotificationManager init")
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
    }

    func registerPermissions() {
        if iOS8 {
            locationManager.requestAlwaysAuthorization()
        }
    }

    func addOrUpdateGeoNotification(geoNotification: JSON, completion: @escaping Callback) {
        log(message: "GeoNotificationManager addOrUpdate")

        let (ok, warnings, errors) = checkRequirements()

        log(messages: warnings)
        log(errors: errors)
        
        if (!ok) {
            return completion(errors)
        }

        let location = CLLocationCoordinate2DMake(
            geoNotification["latitude"].doubleValue,
            geoNotification["longitude"].doubleValue
        )
        log(message: "AddOrUpdate geo: \(geoNotification)")
        let radius = geoNotification["radius"].doubleValue as CLLocationDistance
        let id = geoNotification["id"].stringValue

        log(message: "id : \(id)")
        
        
        let region = CLCircularRegion(center: location, radius: radius, identifier: id)
        
        log(message: "region : \(region)")

        var transitionType = 0
        if let i = geoNotification["transitionType"].int {
            transitionType = i
        }
        region.notifyOnEntry = 0 != transitionType & 1
        region.notifyOnExit = 0 != transitionType & 2

        let command = Command(geoNotification: geoNotification, callback: completion)
        addOrUpdateCallbacks[region] = command
        locationManager.startMonitoring(for: region)
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
        store.remove(id: id)
        let region = getMonitoredRegion(id: id)
        if (region != nil) {
            log(message: "Stop monitoring region \(id)")
            locationManager.stopMonitoring(for: region!)
        }
    }

    func removeAllGeoNotifications() {
        store.clear()
        for object in locationManager.monitoredRegions {
            let region = object
            log(message: "Stop monitoring region \(region.identifier)")
            locationManager.stopMonitoring(for: region)
        }
    }

    
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        log(message: "Entering region \(region.identifier)")
        handleTransition(region: region, transitionType: 1)
    }
    
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        log(message: "Exiting region \(region.identifier)")
        handleTransition(region: region, transitionType: 2)
    }

    
    func locationManager(_ manager: CLLocationManager, didStartMonitoringFor region: CLRegion) {
        if let clRegion = region as? CLCircularRegion {
            if let command = self.addOrUpdateCallbacks[clRegion] {
                store.addOrUpdate(geoNotification: command.geoNotification)
                log(message: "Starting monitoring for region \(region.identifier)")
                command.callback(nil)
                self.addOrUpdateCallbacks.removeValue(forKey: clRegion)
            }
        }
    }
    
    func locationManager(_ manager: CLLocationManager, monitoringDidFailFor region: CLRegion?, withError error: Error) {
        log(message: "Monitoring region \(region!.identifier) failed. Reson: \(error.localizedDescription)")
        if let clRegion = region as? CLCircularRegion {
            if let command = self.addOrUpdateCallbacks[clRegion] {
                var errors = [[String:String]]()
                if locationManager.monitoredRegions.count >= 20 {
                    errors.append([
                        "code": GeofencePlugin.ERROR_GEOFENCE_LIMIT_EXCEEDED,
                        "message": error.localizedDescription
                        ])
                } else {
                    errors.append([
                        "code": GeofencePlugin.ERROR_UNKNOWN,
                        "message": error.localizedDescription
                        ])
                }
                
                command.callback(errors)
                self.addOrUpdateCallbacks.removeValue(forKey: clRegion)
            }
        }
    }

    func handleTransition(region: CLRegion!, transitionType: Int) {
        if var geoNotification = store.findById(id: region.identifier) {
            geoNotification["transitionType"].int = transitionType

            if geoNotification["notification"].exists() {
                notifyAbout(geo: geoNotification)
            }

            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "handleTransition"), object: geoNotification.rawString(String.Encoding.utf8, options: []))
        }
    }

    func notifyAbout(geo: JSON) {
        log(message: "Creating notification")
        let notification = UILocalNotification()
        notification.timeZone = NSTimeZone.default
        let dateTime = NSDate()
        notification.fireDate = dateTime as Date
        notification.soundName = UILocalNotificationDefaultSoundName
        notification.alertBody = geo["notification"]["text"].stringValue
        if let json = geo["notification"]["data"] as JSON? {
            notification.userInfo = ["geofence.notification.data": json.rawString(String.Encoding.utf8, options: [])!]
        }
        UIApplication.shared.scheduleLocalNotification(notification)

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
            log(message: "Cannot fetch sqlite tables: \(err)")
            return
        }

        if (tables.filter { $0 == "GeoNotifications" }.count == 0) {
            if let err = SD.executeChange(sqlStr: "CREATE TABLE GeoNotifications (ID TEXT PRIMARY KEY, Data TEXT)") {
                //there was an error during this function, handle it here
                log(message: "Error while creating GeoNotifications table: \(err)")
            } else {
                //no error, the table was created successfully
                log(message: "GeoNotifications table was created successfully")
            }
        }
    }

    func addOrUpdate(geoNotification: JSON) {
        if (findById(id: geoNotification["id"].stringValue) != nil) {
            update(geoNotification: geoNotification)
        }
        else {
            add(geoNotification: geoNotification)
        }
    }

    func add(geoNotification: JSON) {
        let id = geoNotification["id"].stringValue
        let err = SD.executeChange(sqlStr: "INSERT INTO GeoNotifications (Id, Data) VALUES(?, ?)",
            withArgs: [id as AnyObject, geoNotification.description as AnyObject])

        if err != nil {
            log(message: "Error while adding \(id) GeoNotification: \(err)")
        }
    }

    func update(geoNotification: JSON) {
        let id = geoNotification["id"].stringValue
        let err = SD.executeChange(sqlStr: "UPDATE GeoNotifications SET Data = ? WHERE Id = ?",
            withArgs: [geoNotification.description as AnyObject, id as AnyObject])

        if err != nil {
            log(message: "Error while adding \(id) GeoNotification: \(err)")
        }
    }

    func findById(id: String) -> JSON? {
        let (resultSet, err) = SD.executeQuery(sqlStr: "SELECT * FROM GeoNotifications WHERE Id = ?", withArgs: [id as AnyObject])

        if err != nil {
            //there was an error during the query, handle it here
            log(message: "Error while fetching \(id) GeoNotification table: \(err)")
            return nil
        } else {
            if (resultSet.count > 0) {
                let jsonString = resultSet[0]["Data"]!.asString()!
                do {
                    let jsonDt = try JSON(data: jsonString.data(using: String.Encoding.utf8)!)
                    return jsonDt
                } catch let error {
                    print("error occured \(error)")
                }
                
                return nil
            }
            else {
                return nil
            }
        }
    }

    func getAll() -> [JSON]? {
        let (resultSet, err) = SD.executeQuery(sqlStr: "SELECT * FROM GeoNotifications")

        if err != nil {
            //there was an error during the query, handle it here
            log(message: "Error while fetching from GeoNotifications table: \(err)")
            return nil
        } else {
            var results = [JSON]()
            for row in resultSet {
                if let data = row["Data"]?.asString() {
                    
                    do {
                        try results.append(JSON(data: data.data(using: String.Encoding.utf8)!))
                    } catch let error {
                        print("error occured \(error)")
                    }
                }
            }
            return results
        }
    }

    func remove(id: String) {
        let err = SD.executeChange(sqlStr: "DELETE FROM GeoNotifications WHERE Id = ?", withArgs: [id as AnyObject])

        if err != nil {
            log(message: "Error while removing \(id) GeoNotification: \(err)")
        }
    }

    func clear() {
        let err = SD.executeChange(sqlStr: "DELETE FROM GeoNotifications")

        if err != nil {
            log(message: "Error while deleting all from GeoNotifications: \(err)")
        }
    }
}
