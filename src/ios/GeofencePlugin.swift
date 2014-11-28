//
//  Geofence.swift
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
            geoNotificationManager.addOrUpdateGeoNotification(geo as NSDictionary)
        }
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }

    func getWatched(command: CDVInvokedUrlCommand) {
        var watched = geoNotificationManager.getWatchedGeoNotifications()
        let watchedJsonString = JSON(watched).description
        //println("watched \(JSON(watched).description)")
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsString: watchedJsonString)
        commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }
    
    func remove(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }
    
    func removeAll(command: CDVInvokedUrlCommand) {
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }
}

class GeoNotificationManager : NSObject, CLLocationManagerDelegate {
    let locationManager = CLLocationManager()
    
    override init() {
        log("GeoNotificationManager init")
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        if (!CLLocationManager.locationServicesEnabled()) {
            log("Location services is not enabled")
        }
        let status = CLLocationManager.authorizationStatus()
        if (status == CLAuthorizationStatus.NotDetermined) {
            
        }
        locationManager.requestAlwaysAuthorization()

        //locationManager.requestWhenInUseAuthorization()
        if (!CLLocationManager.isMonitoringAvailableForClass(CLRegion)) {
            log("Geofencing not available")
        }
        //locationManager.startUpdatingLocation()
    }

    func addOrUpdateGeoNotification(geoNotification: NSDictionary) {
        log("GeoNotificationManager addOrUpdate")

        if (!CLLocationManager.locationServicesEnabled()) {
            log("Locationservices is not enabled")
        }

        var location = CLLocationCoordinate2DMake(
            geoNotification["latitude"] as Double,
            geoNotification["longitude"] as Double
        )
    
        var radius = geoNotification["radius"] as CLLocationDistance
        //let uuid = NSUUID().UUIDString
        
        var region = CLCircularRegion(
            circularRegionWithCenter: location,
            radius: radius,
            identifier: geoNotification["id"] as String
        )
        region.notifyOnEntry = true
        region.notifyOnExit = true
        log("Starting monitoring region")
        locationManager.startMonitoringForRegion(region)
    }

    func getWatchedGeoNotifications() -> [NSDictionary] {
        var result = [NSDictionary]()

        for object in locationManager.monitoredRegions {
            let region = object as CLCircularRegion

            var geoNot = [
                "id": region.identifier,
                "latitude": region.center.latitude,
                "longitude": region.center.longitude,
                "radius": region.radius
            ]
            result.append(geoNot)
        }
        return result
    }
    
    func locationManager(manager: CLLocationManager!, didUpdateLocations locations: [AnyObject]!) {
        log("update location")
    }
    
    func locationManager(manager: CLLocationManager!, didFailWithError error: NSError!) {
        log("fail with error " + error.description)
    }
    
    func locationManager(manager: CLLocationManager!, didFinishDeferredUpdatesWithError error: NSError!) {
        log("deferred fail error " + error.description)
    }
    
    func locationManager(manager: CLLocationManager!, didEnterRegion region: CLRegion!) {
        log("Entering region " + region.identifier)
    }
    
    func locationManager(manager: CLLocationManager!, didExitRegion region: CLRegion!) {
        log("Exiting region " + region.identifier)
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