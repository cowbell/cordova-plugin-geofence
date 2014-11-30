//
//  GeofencePlugin.swift
//  ionic-geofence
//
//  Created by tomasz on 07/10/14.
//
//

import Foundation

let TAG = "GeofencePlugin"
func log(message: String){
    NSLog("%@ - %@", TAG, message)
}

@objc(HWPGeofencePlugin) class GeofencePlugin : CDVPlugin {
    let geoNotificationManager = GeoNotificationManager()

    func initialize(command: CDVInvokedUrlCommand) {
        log("Plugin initialization");
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }
    
    func addOrUpdate(command: CDVInvokedUrlCommand) {
        for geo in command.arguments {
            geoNotificationManager.addOrUpdateGeoNotification(JSON(geo))
        }
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }

    func getWatched(command: CDVInvokedUrlCommand) {
        var watched = geoNotificationManager.getWatchedGeoNotifications()!
        let watchedJsonString = watched.description
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsString: watchedJsonString)
        commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }
    
    func remove(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        for id in command.arguments {
            geoNotificationManager.removeGeoNotification(id as String)
        }
        commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }
    
    func removeAll(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        geoNotificationManager.removeAllGeoNotifications()
        commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
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
        let status = CLLocationManager.authorizationStatus()
        //log("LocationManager authorizationStatus: \(status)")
        if (status == CLAuthorizationStatus.NotDetermined) {
            
        }
        locationManager.requestAlwaysAuthorization()

        //locationManager.requestWhenInUseAuthorization()
        if (!CLLocationManager.isMonitoringAvailableForClass(CLRegion)) {
            log("Geofencing not available")
        }
        //locationManager.startUpdatingLocation()
    }

    func addOrUpdateGeoNotification(geoNotification: JSON) {
        log("GeoNotificationManager addOrUpdate")

        if (!CLLocationManager.locationServicesEnabled()) {
            log("Locationservices is not enabled")
        }

        var location = CLLocationCoordinate2DMake(
            geoNotification["latitude"].double!,
            geoNotification["longitude"].double!
        )
    
        var radius = geoNotification["radius"].double! as CLLocationDistance
        //let uuid = NSUUID().UUIDString
        let id = geoNotification["id"].string

        var region = CLCircularRegion(
            circularRegionWithCenter: location,
            radius: radius,
            identifier: id
        )
        region.notifyOnEntry = geoNotification["transitionType"].int! == 1 ? true: false
        region.notifyOnExit = geoNotification["transitionType"].int! == 2 ? true: false
        log("Starting monitoring region \(id)")
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
    }
    
    func locationManager(manager: CLLocationManager!, didExitRegion region: CLRegion!) {
        log("Exiting region \(region.identifier)")
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
        if (findById(geoNotification["id"].string!) != nil) {
            update(geoNotification)
        }
        else {
            add(geoNotification)
        }
    }

    func add(geoNotification: JSON) {
        let id = geoNotification["id"].string!
        let err = SD.executeChange("INSERT INTO GeoNotifications (Id, Data) VALUES(?, ?)",
            withArgs: [id, geoNotification.description])

        if err != nil {
            log("Error while adding \(id) GeoNotification: \(err)")
        }
    }

    func update(geoNotification: JSON) {
        let id = geoNotification["id"].string!
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
                return JSON(resultSet[0]["Data"]!.asString()!)
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
                    results.append(JSON(data))
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