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
    lazy var geoNotificationManager = GeoNotificationManager()
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

        geoNotificationManager = GeoNotificationManager()
        geoNotificationManager.registerPermissions()

        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
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
                        let id = geo["id"].stringValue
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

        if (!CLLocationManager.isMonitoringAvailableForClass(CLRegion)) {
            log("Geofencing not available")
        }
    }

    func registerPermissions() {
        if iOS8 {
            locationManager.requestAlwaysAuthorization()
        }
    }

    func addOrUpdateGeoNotification(geoNotification: JSON) {
        log("GeoNotificationManager addOrUpdate")

        checkRequirements()

        let location = CLLocationCoordinate2DMake(
            geoNotification["latitude"].doubleValue,
            geoNotification["longitude"].doubleValue
        )
        log("AddOrUpdate geo: \(geoNotification)")
        let radius = geoNotification["radius"].doubleValue as CLLocationDistance
        //let uuid = NSUUID().UUIDString
        let id = geoNotification["id"].stringValue

        let region = CLCircularRegion(center: location, radius: radius, identifier: id)

        var transitionType = 0
        if let i = geoNotification["transitionType"].int {
            transitionType = i
        }
        region.notifyOnEntry = 0 != transitionType & 1
        region.notifyOnExit = 0 != transitionType & 2

        //store
        store.addOrUpdate(geoNotification)
        locationManager.startMonitoringForRegion(region)
    }

    func checkRequirements() {
        if (!CLLocationManager.locationServicesEnabled()) {
            log("Warning: Locationservices is not enabled")
        }

        let authStatus = CLLocationManager.authorizationStatus()

        if (authStatus != CLAuthorizationStatus.AuthorizedAlways) {
            log("Warning: Location always permissions not granted, have you initialized geofence plugin?")
        }

        if let notificationSettings = UIApplication.sharedApplication().currentUserNotificationSettings() {
            if !notificationSettings.types.contains(.Sound) {
                log("Warning: notification settings - sound permission missing")
            }

            if !notificationSettings.types.contains(.Alert) {
                log("Warning: notification settings - alert permission missing")
            }

            if !notificationSettings.types.contains(.Badge) {
                log("Warning: notification settings - badge permission missing")
            }
        } else {
            log("Warning: notification permission missing")
        }
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
        let location = manager.location!.coordinate // user location at the trigger time
        handleTransition(region, transitionType: 1, location: location)
    }

    func locationManager(manager: CLLocationManager, didExitRegion region: CLRegion) {
        log("Exiting region \(region.identifier)")
        let location = manager.location!.coordinate // user location at the trigger time
        handleTransition(region, transitionType: 2, location: location)
    }

    func locationManager(manager: CLLocationManager, didStartMonitoringForRegion region: CLRegion) {
        let lat = (region as! CLCircularRegion).center.latitude
        let lng = (region as! CLCircularRegion).center.longitude
        let radius = (region as! CLCircularRegion).radius

        log("Starting monitoring for region \(region) lat \(lat) lng \(lng) of radius \(radius)")
    }

    func locationManager(manager: CLLocationManager, didDetermineState state: CLRegionState, forRegion region: CLRegion) {
        log("State for region " + region.identifier)
    }

    func locationManager(manager: CLLocationManager, monitoringDidFailForRegion region: CLRegion?, withError error: NSError) {
        log("Monitoring region " + region!.identifier + " failed " + error.description)
    }

    func handleTransition(region: CLRegion!, transitionType: Int, location: CLLocationCoordinate2D!) {
        if var geoNotification = store.findById(region.identifier) {
            geoNotification["transitionType"].int = transitionType

            if geoNotification["notification"].isExists() {
                if validTime(geoNotification) == true {
                    if checkRateLimit(geoNotification) == true {
                        notifyAbout(geoNotification)
                        notifyServer(geoNotification, location: location)
                        setNotificationTimestamp()
                    }
                }
            }

            NSNotificationCenter.defaultCenter().postNotificationName("handleTransition", object: geoNotification.rawString(NSUTF8StringEncoding, options: []))
        }
    }
    
    func validTime(geo: JSON) -> Bool {
        var maxHour = 18; // 6pm
        var minHour = 10; // 10am
        
        let calendar = NSCalendar.currentCalendar()
        let hour = calendar.component(.Hour,fromDate: NSDate())
        
        if let customMaxHour = geo["notification"]["data"]["maxHour"].int {
            maxHour = customMaxHour
        }
        
        if let customMinHour = geo["notification"]["data"]["minHour"].int {
            minHour = customMinHour
        }
        
        if (hour >= maxHour || hour < minHour) { // notify at 10:01am, but not at 6:01pm
            return false
        } else {
            return true
        }
    }
    
    
    
    func convertDateToString(date: NSDate) -> String? {
        let dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd H:mm:ss Z"
        return dateFormatter.stringFromDate(date)
    }
    
    func getNotificationTimestamp() -> String?{
        let defaults = NSUserDefaults.standardUserDefaults()
        let variable = defaults.objectForKey("moment_push_time") as? String
        
        if variable != nil{
            return variable
        } else{
            let returnValue : String = "empty"
            return returnValue
        }
    }
    
    func setNotificationTimestamp(){
        let defaults = NSUserDefaults.standardUserDefaults()
        let date = convertDateToString(NSDate())
        let labelTime = "moment_push_time"
        defaults.setObject(date, forKey: labelTime)
        NSUserDefaults.standardUserDefaults().synchronize()
    }
    
    func checkRateLimit(geo: JSON) -> Bool {
        let previousDate = getNotificationTimestamp()
        
        var rateLimit = 1440; // minutes -- 24hrs = 60 * 24 =
        
        if let customRateLimit = geo["notification"]["data"]["rateLimit"].int {
            rateLimit = customRateLimit
        }
        
        if previousDate == "empty" {
            return true
        } else {
            let dateFormatter = NSDateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd H:mm:ss Z"
            let convertedPreviousDate = dateFormatter.dateFromString(previousDate!)
            var elapsedTime = NSDate().timeIntervalSinceDate(convertedPreviousDate!)
            elapsedTime = elapsedTime / 60 // convert seconds to minutes
            
            if (elapsedTime > Double(rateLimit)) {
                return true
            } else {
                return false
            }
        }
        
        
    }

    func notifyAbout(geo: JSON) {
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
    
    func notifyServer(geo: JSON, location: CLLocationCoordinate2D!) {
        let surpriseID = geo["notification"]["data"]["_id"].string!
        var serverURL = ""
        
        if let apiEndpoint = geo["notification"]["data"]["env"].string {
            serverURL = apiEndpoint + "/surprises/" + surpriseID + "/ping"
        } else {
            let apiEndpoint = "https://api.discovermoment.com/api" //assume if no env, in production
            serverURL = apiEndpoint + "/surprises/" + surpriseID + "/ping"
        }

        
        log("Telling the server we've triggered a geofence for surprise: \(surpriseID)")
        
        
        let request = NSMutableURLRequest(URL: NSURL(string: serverURL)!)
        
        let session = NSURLSession.sharedSession()
        request.HTTPMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        
        let params = [
            "transitionType": geo["transitionType"].int!,
            "location": [
                "coords":[
                    "latitude": location.latitude,
                    "longitude":location.longitude
                ]
            ]
        ] as Dictionary<String,AnyObject>
        
        do {
            request.HTTPBody = try NSJSONSerialization.dataWithJSONObject(params, options: [])
            print(params)
        } catch {
            log("error on constructing HTTP body = \(error)")
            request.HTTPBody = nil
        }
        
        let task = session.dataTaskWithRequest(request) { data, response, error in
            guard data != nil else {
                log("no data found: \(error)")
                return
            }
            
            do {
                if let json = try NSJSONSerialization.JSONObjectWithData(data!, options: []) as? NSDictionary {
                    let success = json["success"] as? Int                                  // Okay, the `json` is here, let's get the value for 'success' out of it
                    log("Success: \(success)")
                    log("JSON response: \(json)")
                } else {
                    let jsonStr = NSString(data: data!, encoding: NSUTF8StringEncoding)    // No error thrown, but not NSDictionary
                    log("Error could not parse JSON: \(jsonStr)")
                }
            } catch let parseError {
                log("error on parsing json = \(parseError)")    // Log the error thrown by `JSONObjectWithData`
                let jsonStr = NSString(data: data!, encoding: NSUTF8StringEncoding)
                log("Error could not parse JSON: '\(jsonStr)'")
            }
        }
        
        task.resume()
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
