//
//  GeofencePlugin.swift
//  ionic-geofence
//
//  Created by tomasz on 07/10/14.
//
//

import Foundation
import UIKit

let TAG = "GeofencePlugin"
let iOS8 = floor(NSFoundationVersionNumber) > floor(NSFoundationVersionNumber_iOS_7_1)
let iOS7 = floor(NSFoundationVersionNumber) <= floor(NSFoundationVersionNumber_iOS_7_1)

func log(message: String){
    NSLog("%@ - %@", TAG, message)
}

var GeofencePluginWebView: UIWebView?

@objc(HWPGeofencePlugin) class GeofencePlugin : CDVPlugin {
    let geoNotificationManager = GeoNotificationManager()
    let priority = DISPATCH_QUEUE_PRIORITY_DEFAULT

    func initialize(command: CDVInvokedUrlCommand) {
        log("Plugin initialization");
        //let faker = GeofenceFaker(manager: geoNotificationManager)
        //faker.start()
        GeofencePluginWebView = self.webView

        if iOS8 {
            promptForNotificationPermission()
        }
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }

    func promptForNotificationPermission() {
        UIApplication.sharedApplication().registerUserNotificationSettings(UIUserNotificationSettings(
            forTypes: UIUserNotificationType.Sound | UIUserNotificationType.Alert | UIUserNotificationType.Badge,
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
                var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    func getWatched(command: CDVInvokedUrlCommand) {
        dispatch_async(dispatch_get_global_queue(priority, 0)) {
            var watched = self.geoNotificationManager.getWatchedGeoNotifications()!
            let watchedJsonString = watched.description
            dispatch_async(dispatch_get_main_queue()) {
                var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsString: watchedJsonString)
                self.commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    func remove(command: CDVInvokedUrlCommand) {
        dispatch_async(dispatch_get_global_queue(priority, 0)) {
            for id in command.arguments {
                self.geoNotificationManager.removeGeoNotification(id as String)
            }
            dispatch_async(dispatch_get_main_queue()) {
                var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    func removeAll(command: CDVInvokedUrlCommand) {
        dispatch_async(dispatch_get_global_queue(priority, 0)) {
            self.geoNotificationManager.removeAllGeoNotifications()
            dispatch_async(dispatch_get_main_queue()) {
                var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
                self.commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
            }
        }
    }

    class func fireReceiveTransition(geoNotification: JSON) {
        var mustBeArray = [JSON]()
        mustBeArray.append(geoNotification)
        let js = "setTimeout('geofence.receiveTransition(" + mustBeArray.description + ")',0)";
        if (GeofencePluginWebView != nil) {
            GeofencePluginWebView!.stringByEvaluatingJavaScriptFromString(js);
        }
    }
}

// class for faking crossing geofences
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
                        var geo = geos[Int(index)]
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
                NSThread.sleepForTimeInterval(3);
            }
         }
    }

    func stop() {

    }
}

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

        var location = CLLocationCoordinate2DMake(
            geoNotification["latitude"].asDouble!,
            geoNotification["longitude"].asDouble!
        )
        log("AddOrUpdate geo: \(geoNotification)")
        var radius = geoNotification["radius"].asDouble! as CLLocationDistance
        //let uuid = NSUUID().UUIDString
        let id = geoNotification["id"].asString

        var region = CLCircularRegion(
            circularRegionWithCenter: location,
            radius: radius,
            identifier: id
        )
        region.notifyOnEntry = geoNotification["transitionType"].asInt == 1 ? true: false
        region.notifyOnExit = geoNotification["transitionType"].asInt == 2 ? true: false
        //store
        store.addOrUpdate(geoNotification)
        locationManager.startMonitoringForRegion(region)
    }

    func getWatchedGeoNotifications() -> [JSON]? {
        return store.getAll()
    }

    func getMonitoredRegion(id: String) -> CLRegion? {
        for object in locationManager.monitoredRegions {
            let region = object as CLRegion

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
            locationManager.stopMonitoringForRegion(region)
        }
    }

    func removeAllGeoNotifications() {
        store.clear()
        for object in locationManager.monitoredRegions {
            let region = object as CLRegion
            log("Stoping monitoring region \(region.identifier)")
            locationManager.stopMonitoringForRegion(region)
        }
    }

    func locationManager(manager: CLLocationManager!, didUpdateLocations locations: [AnyObject]!) {
        log("update location")
    }

    func locationManager(manager: CLLocationManager!, didFailWithError error: NSError!) {
        log("fail with error: \(error)")
    }

    func locationManager(manager: CLLocationManager!, didFinishDeferredUpdatesWithError error: NSError!) {
        log("deferred fail error: \(error)")
    }

    func locationManager(manager: CLLocationManager!, didEnterRegion region: CLRegion!) {
        log("Entering region \(region.identifier)")
        handleTransition(region)
    }

    func locationManager(manager: CLLocationManager!, didExitRegion region: CLRegion!) {
        log("Exiting region \(region.identifier)")
        handleTransition(region)
    }

    func locationManager(manager: CLLocationManager!, didStartMonitoringForRegion region: CLRegion!) {
        let lat = (region as CLCircularRegion).center.latitude
        let lng = (region as CLCircularRegion).center.longitude
        let radius = (region as CLCircularRegion).radius

        log("Starting monitoring for region \(region) lat \(lat) lng \(lng)")
    }

    func locationManager(manager: CLLocationManager, didDetermineState state: CLRegionState, forRegion region: CLRegion) {
        log("State for region " + region.identifier)
    }

    func locationManager(manager: CLLocationManager, monitoringDidFailForRegion region: CLRegion!, withError error: NSError!) {
        log("Monitoring region " + region.identifier + " failed " + error.description)
    }

    func handleTransition(region: CLRegion!) {
        if let geo = store.findById(region.identifier) {
            notifyAbout(geo)
            GeofencePlugin.fireReceiveTransition(geo)
        }
    }

    func notifyAbout(geo: JSON) {
        log("Creating notification")
        
        var tryVar : Bool = checkRateLimit(geo)
        
        if tryVar == true {
            if validateTime(geo) == true{
                var notification = UILocalNotification()
                notification.timeZone = NSTimeZone.defaultTimeZone()
                var dateTime = NSDate()
                notification.fireDate = dateTime
                notification.alertBody = geo["notification"]["text"].asString!
                UIApplication.sharedApplication().scheduleLocalNotification(notification)
            }
        }
    }
    
    func validateTime(geo: JSON) -> Bool{
        let currentDate = NSDate()
        var startDate = NSDate(timeIntervalSince1970: geo["notification"]["start_date"].asNumber!)
        var endDate = NSDate(timeIntervalSince1970: geo["notification"]["end_date"].asNumber!)
        var startTime = geo["notification"]["start_time"].asString!
        var startTimeArr = split(startTime) {$0 == ":"}
        var endTime = geo["notification"]["end_time"].asString!
        var endTimeArr = split(endTime) {$0 == ":"}
        
        let calendar = NSCalendar.currentCalendar()
        let components = calendar.components(.CalendarUnitDay | .CalendarUnitMonth | .CalendarUnitYear, fromDate: currentDate)
        
        var componentsTwo = NSDateComponents()
            componentsTwo.day = components.day
            componentsTwo.month = components.month
            componentsTwo.year = components.year
            componentsTwo.hour = startTimeArr[0].toInt()!
            componentsTwo.minute = startTimeArr[1].toInt()!
        
        var startTimeDate = calendar.dateFromComponents(componentsTwo)
        
            componentsTwo.hour = endTimeArr[0].toInt()!
            componentsTwo.minute = endTimeArr[1].toInt()!
        
        
        var endTimeDate = calendar.dateFromComponents(componentsTwo)
        
        var diffStart = startDate.compare(currentDate)
        var diffEnd = endDate.compare(currentDate)
        var diffStartTime = currentDate.compare(startTimeDate!)
        var diffEndTime = currentDate.compare(endTimeDate!)
        
        if diffStart == NSComparisonResult.OrderedAscending && diffEnd == NSComparisonResult.OrderedDescending{
            if diffStartTime == NSComparisonResult.OrderedDescending && diffEndTime == NSComparisonResult.OrderedAscending{
                var currentDateFromString = convertDateToString(currentDate)
                var tryText = geo["notification"]["text"].asString!
                setNotificationTimestamp(geo["id"].asString!,date: currentDateFromString!,msg: geo["notification"]["text"].asString!)
                return true
            }
            else{
                return false
            }
        }
        else{
            return false
        }
    }
    
    func checkRateLimit(geo: JSON) -> Bool{
        var currentDate : String? = convertDateToString(NSDate())
        var currentText : String? = geo["notification"]["text"].asString!
        
        var previousDate = getNotificationTimestamp(geo["id"].asString!)
        var previousText = getNotificationSavedText(geo["id"].asString!)
        
        let rateLimit = geo["notification"]["period_milliseconds"].asDouble
        
        if previousDate == "empty" {
            //no previous timestamp and no previous text message
            return true
        } else {
            let dateFormatter = NSDateFormatter()
            dateFormatter.dateFormat = "yyyy-MM-dd hh:mm:ss"
            let convertedPreviousDate = dateFormatter.dateFromString(previousDate!)
        
            var elapsedTime = NSDate().timeIntervalSinceDate(convertedPreviousDate!)
            elapsedTime = elapsedTime * 1000 //convert seconds to milliseconds
            
            if currentText == previousText {
                if (elapsedTime > rateLimit) {
                    return true
                } else {
                    return false
                }
            } else {
                return true
            }
        }
    }
    
    func convertDateToString(date: NSDate) -> String? {
        let dateFormatter = NSDateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd hh:mm:ss"
        return dateFormatter.stringFromDate(date)
    }
    
    func setNotificationTimestamp(regionId: String,date: String,msg: String){
        let defaults = NSUserDefaults.standardUserDefaults()
        var labelTime = regionId + "_time"
        var labelMessage = regionId + "_message"
        defaults.setObject(date, forKey: labelTime)
        defaults.setObject(msg, forKey: labelMessage)
        NSUserDefaults.standardUserDefaults().synchronize()
    }
    
    func getNotificationTimestamp(regionId: String) -> String?{
        let defaults = NSUserDefaults.standardUserDefaults()
        var variable = defaults.objectForKey(regionId + "_time") as? String
        
        if variable != nil{
            return variable
        }
        else{
            let returnValue : String = "empty"
            return returnValue
        }
    }
    
    func getNotificationSavedText(regionId: String) -> String?{
        let defaults = NSUserDefaults.standardUserDefaults()
        var textMessage = defaults.objectForKey(regionId + "_message") as? String
        
        if textMessage != nil {
            return textMessage
        } else {
            let returnMessage : String = "empty"
            return returnMessage
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