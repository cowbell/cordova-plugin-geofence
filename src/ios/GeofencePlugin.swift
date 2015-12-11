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
let monitoringLimit = 10
let dummyRegionIdentifier = "DUMMY"
var latitude = 0.0
var longitude = 0.0
var refreshing = false


func log(message: String){
    NSLog("%@ - %@", TAG, message)
}

@available(iOS 8.0, *)
@objc(HWPGeofencePlugin) class GeofencePlugin : CDVPlugin {
    var isDeviceReady: Bool = false
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
            var overLimit = false
            if self.geoNotificationManager.locationManager.monitoredRegions.count + command.arguments.count > monitoringLimit {
                overLimit = true
            }
            for geo in command.arguments {
                self.geoNotificationManager.addOrUpdateGeoNotification(JSON(geo),overLimit: overLimit)
            }
            log("Stored: \(self.geoNotificationManager.getWatchedGeoNotifications()?.count) Monitored: \(self.geoNotificationManager.locationManager.monitoredRegions.count)")
            // If over the limit, get user location and let monitorNearbyRegions() handle the monitoring
            if overLimit{
                refreshing = true
                self.geoNotificationManager.locationManager.startUpdatingLocation()
                log("Number of total fences is over the limit. Start updating location")
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
    
    func getMonitored(command: CDVInvokedUrlCommand) {
        dispatch_async(dispatch_get_global_queue(priority, 0)) {
            var monitored = self.geoNotificationManager.locationManager.monitoredRegions.generate()
            var jsonObject: [AnyObject] = Array(count: self.geoNotificationManager.locationManager.monitoredRegions.count, repeatedValue: [AnyObject]())
            var index = 0
            while let member = monitored.next(){
                let region = member as! CLCircularRegion
                jsonObject[index] = ["id": region.identifier, "latitude": region.center.latitude, "longitude": region.center.longitude, "radius": region.radius]
                index++
            }
            
            let monitoredJsonString = jsonObject.description
            dispatch_async(dispatch_get_main_queue()) {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsString: monitoredJsonString)
                self.commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
            }
        }
    }
    
    func locationEnabled(command: CDVInvokedUrlCommand) {
        dispatch_async(dispatch_get_global_queue(priority, 0)) {
            let enabled = CLLocationManager.locationServicesEnabled()
            dispatch_async(dispatch_get_main_queue()) {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsBool: enabled)
                self.commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
            }
        }
    }
    
    func locationAuthorized(command: CDVInvokedUrlCommand) {
        dispatch_async(dispatch_get_global_queue(priority, 0)) {
            let authorized = CLLocationManager.authorizationStatus() == CLAuthorizationStatus.AuthorizedAlways
            dispatch_async(dispatch_get_main_queue()) {
                let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsBool: authorized)
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
        //        if iOS8 {
        //            locationManager.requestAlwaysAuthorization()
        //        }
        
        if (!CLLocationManager.isMonitoringAvailableForClass(CLRegion)) {
            log("Geofencing not available")
        }
    }
    func registerPermissions() {
        if iOS8 {
            locationManager.requestAlwaysAuthorization()
        }
    }
    func addOrUpdateGeoNotification(geoNotification: JSON, overLimit: Bool) {
        log("GeoNotificationManager addOrUpdate")
        
        if (!CLLocationManager.locationServicesEnabled()) {
            log("Locationservices is not enabled")
        }
        
        if iOS8 {
            locationManager.requestAlwaysAuthorization()
        }
        
        //store
        store.addOrUpdate(geoNotification)
        
        //If over the limit, postpone the monitoring until monitorNearbyRegions() is fired
        if !overLimit{
            log("Number of fences is under the limit. Start monitoring normally.")
            let location = CLLocationCoordinate2DMake(
                geoNotification["latitude"].double!,
                geoNotification["longitude"].double!
            )
            log("AddOrUpdate geo: \(geoNotification)")
            let radius = geoNotification["radius"].double! as CLLocationDistance
            //let uuid = NSUUID().UUIDString
            let id = geoNotification["id"].string
            
            let region = CLCircularRegion(center: location, radius: radius, identifier: id!)
            
            var transitionType = 0
            if let i = geoNotification["transitionType"].int {
                transitionType = i
            }
            region.notifyOnEntry = 0 != transitionType & 1
            region.notifyOnExit = 0 != transitionType & 2
            
            locationManager.startMonitoringForRegion(region)
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
    
    func distanceBetween(geo: JSON,location: CLLocation) -> Double? {
        let origin = CLLocation(latitude: geo["latitude"].double!, longitude: geo["longitude"].double!)
        let radius = geo["radius"].double! as CLLocationDistance
        //        let name = geo["notification"]["title"]
        //        let id = geo["id"].asString!
        let distanceToLocation = location.distanceFromLocation(origin) - radius
        //log("distance to \(name) = \(location.distanceFromLocation(origin)) - \(radius) = \(distanceToLocation)")
        return distanceToLocation
    }
    func monitorNearbyRegions(lat: Double, lon: Double){
        log("Monitor nearby regions")
        log("[Before refresh] Stored: \(getWatchedGeoNotifications()?.count) Monitored: \(locationManager.monitoredRegions.count)") //Expect (32,0) or (32,20)
        //Stop all monitoring
        for object in locationManager.monitoredRegions {
            let region = object
            log("Stoping monitoring region \(region.identifier)")
            locationManager.stopMonitoringForRegion(region)
        }
        
        log("[Between refresh] Stored: \(getWatchedGeoNotifications()?.count) Monitored: \(locationManager.monitoredRegions.count)") // Expect (32, 0)
        let location = CLLocation(latitude: lat, longitude: lon)    //User location
        var geos = getWatchedGeoNotifications()!    //All geofences stored
        
        // Sort by distance ascendingly
        geos.sortInPlace{self.distanceBetween($0, location: location) < self.distanceBetween($1, location: location)}
        log("Sorted distances")
        
        for geo in geos {
            let id = geo["id"]
            let name = geo["notification"]["title"]
            log("ID \(id) \(name): \(distanceBetween(geo, location: location)!/1000) km")
        }
        
        // Create and monitor dummy geofence (special geofence which covers the nearest [monitoringLimit - 1] geofences)
        let farGeo = geos[monitoringLimit-1]
        let farGeoOrigin = CLLocation(latitude: farGeo["latitude"].double!,longitude: farGeo["longitude"].double!)
        var dummyRegionRadius = location.distanceFromLocation(farGeoOrigin)-(farGeo["radius"].double!)    // from user location to the limit region's (i.e. 20th region) rim
        // Prevent the radius from exceeding the limit
        if (dummyRegionRadius > locationManager.maximumRegionMonitoringDistance) {
            dummyRegionRadius = locationManager.maximumRegionMonitoringDistance;
        }
        let dummyRegion = CLCircularRegion(
            center: CLLocationCoordinate2DMake(lat,lon),  // user location
            radius: dummyRegionRadius,
            identifier: dummyRegionIdentifier
        )
        // Notify on exit only
        dummyRegion.notifyOnEntry = false
        dummyRegion.notifyOnExit = true
        log("Start monitoring dummy region")
        locationManager.startMonitoringForRegion(dummyRegion)
        
        // Monitor the nearest [monitoringLimit - 1] geofences
        var index : Int
        for index = 0; index < monitoringLimit - 1; index++ {
            let origin = CLLocationCoordinate2DMake(
                geos[index]["latitude"].double!,
                geos[index]["longitude"].double!
            )
            let region = CLCircularRegion(
                center: origin,
                radius: geos[index]["radius"].double!,
                identifier: geos[index]["id"].string!
            )
            var transitionType = 0
            if let i = geos[index]["transitionType"].int {
                transitionType = i
            }
            region.notifyOnEntry = 0 != transitionType & 1
            region.notifyOnExit = 0 != transitionType & 2
            //region.notifyOnEntry = geos[index]["transitionType"].asInt == 1 ? true: false
            //region.notifyOnExit = geos[index]["transitionType"].asInt == 2 ? true: false
            log("Start monitoring region \(region.identifier)")
            locationManager.startMonitoringForRegion(region)
        }
        
        log("[After refresh] Stored: \(getWatchedGeoNotifications()?.count) Monitored: \(locationManager.monitoredRegions.count)")  //Expect (32, 20)
    }
    
    func locationManager(manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        if refreshing {
            let locationArray = locations as NSArray
            let locationObj = locationArray.lastObject as! CLLocation
            let coord = locationObj.coordinate
            latitude = coord.latitude
            longitude = coord.longitude
            log("New location = \(latitude), \(longitude)")
            locationManager.stopUpdatingLocation()
            log("Stop updating location")
            refreshing = false
            monitorNearbyRegions(latitude, lon: longitude)
        }
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
        let circularRegion = (region as! CLCircularRegion)  // convert to CLCircularRegion
        // create circular region with doubled radius (in order not to make the filter too strict)
        let coverageRegion = CLCircularRegion(center: circularRegion.center, radius: circularRegion.radius * 2, identifier: circularRegion.identifier)
        
        if coverageRegion.containsCoordinate(location){
            log("This event is reliable")
            handleTransition(region, transitionType: 1, location: location)
        }
        else{
            log("This event is not reliable. Ignore it.")
        }
    }
    
    func locationManager(manager: CLLocationManager, didExitRegion region: CLRegion) {
        log("Exiting region \(region.identifier)")
        let location = manager.location!.coordinate // user location at the trigger time
        
        /*if !((region as CLCircularRegion).containsCoordinate(location)){
        log("This event is reliable")
        // If it's dummy geofence, restart the monitoring*/
        if region.identifier == dummyRegionIdentifier {
            refreshing = true
            //locationManager.startUpdatingLocation()
            log("Refreshing geofences due to dummy geofence exit")
            //BEGIN REFRESH
            var lat = location.latitude
            var lon = location.longitude
            log("New location = \(lat), \(lon)")
            monitorNearbyRegions(lat, lon: lon)
            //END REFRESH
            /*var notification = UILocalNotification()
            notification.timeZone = NSTimeZone.defaultTimeZone()
            var dateTime = NSDate()
            notification.fireDate = dateTime
            notification.alertBody = "Exit dummy geofence. Refreshing geofences..."
            UIApplication.sharedApplication().scheduleLocalNotification(notification)*/
        }
        else {
            handleTransition(region, transitionType: 2, location: location)
        }
        /*}
        else{
        log("This event is not reliable. Ignore it.")
        }*/    }
    
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
        
        if let customMaxHour = geo["notification"]["data"]["closeHour"].int {
            maxHour = customMaxHour
        }
        
        if let customMinHour = geo["notification"]["data"]["openHour"].int {
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
        
        if let customRateLimit = geo["notification"]["data"]["frequency"].int {
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
    
    func notifyServer(geo: JSON, location: CLLocationCoordinate2D!) {
        log("Creating notification")
        let notification = UILocalNotification()
        notification.timeZone = NSTimeZone.defaultTimeZone()
        let dateTime = NSDate()
        notification.fireDate = dateTime
        notification.soundName = UILocalNotificationDefaultSoundName
        let surpriseID = geo["notification"]["data"]["_id"].string!
        var serverURL = "https://api.discovermoment.com/api/surprises/" + surpriseID + "/ping"
        
        if let tempServerUrl = geo["notification"]["data"]["endpoint"].string {
            serverURL = tempServerUrl
        }
        
        log("Telling the server we've triggered a geofence for surprise: \(serverURL)")
        
        
        let request = NSMutableURLRequest(URL: NSURL(string: serverURL)!)
        
        let session = NSURLSession.sharedSession()
        request.HTTPMethod = "POST"
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.addValue("application/json", forHTTPHeaderField: "Accept")
        
        // TOOD: check here if basicAuth was sent in, and if so, add it to the HeaderField
        
        let params = [
            "transitionType": geo["transitionType"].int!,
            "location": [
                "coords":[
                    "latitude": location.latitude,
                    "longitude": location.longitude
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
