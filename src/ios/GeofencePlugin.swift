//
//  Geofence.swift
//  ionic-geofence
//
//  Created by tomasz on 07/10/14.
//
//

import Foundation

@objc(HWPGeofencePlugin) class GeofencePlugin : CDVPlugin {
    let geoNotificationManager = GeoNotificationManager()

    func initialize(command: CDVInvokedUrlCommand) {
        println("Plugin initialization");
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId:command.callbackId)
    }
    
    func addOrUpdate(command: CDVInvokedUrlCommand) {
        for geo in command.arguments {
            geoNotificationManager.addOrUpdateGeoNotification(geo as NSDictionary)
        }
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId:command.callbackId)
    }
    
    func remove(command: CDVInvokedUrlCommand) {
        println("Helloł bitches");
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId:command.callbackId)
    }
    
    func removeAll(command: CDVInvokedUrlCommand) {
        println("Helloł bitches");
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK)
        commandDelegate.sendPluginResult(pluginResult, callbackId:command.callbackId)
    }
}


class GeoNotificationManager : NSObject, CLLocationManagerDelegate {
    let locationManager = CLLocationManager()
    
    override init() {
        println("Manager init")
        super.init()
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        if (!CLLocationManager.locationServicesEnabled()) {
            println("Location services is not enabled")
        }
        let status = CLLocationManager.authorizationStatus()
        if(status == CLAuthorizationStatus.NotDetermined) {
            
        }
        locationManager.requestAlwaysAuthorization()
        //locationManager.requestWhenInUseAuthorization()
        if(!CLLocationManager.isMonitoringAvailableForClass(CLRegion)) {
            println("Geofencing not available")
        }
        //locationManager.startUpdatingLocation()
    }

    func addOrUpdateGeoNotification(geoNotification: NSDictionary) {
        println("addOrUpdate")
        
        if (!CLLocationManager.locationServicesEnabled()) {
            println("locationservices is not enabled")
        }
        
        if (CLLocationManager.regionMonitoringAvailable()) {
            
        }
        
        var location = CLLocationCoordinate2DMake(
            geoNotification["latitude"] as Double,
            geoNotification["longitude"] as Double
        )
        var radius = geoNotification["radius"] as CLLocationDistance
        let uuid = NSUUID().UUIDString
        
        var region = CLCircularRegion(
            circularRegionWithCenter: location,
            radius: radius,
            identifier: uuid
        )
        region.notifyOnEntry = true
        region.notifyOnExit = true
        locationManager.startMonitoringForRegion(region)
    }
    
    func locationManager(manager: CLLocationManager!, didUpdateLocations locations: [AnyObject]!) {
        println("update location")
    }
    
    func locationManager(manager: CLLocationManager!, didFailWithError error: NSError!) {
        println("fail with error " + error.description)
    }
    
    func locationManager(manager: CLLocationManager!, didFinishDeferredUpdatesWithError error: NSError!) {
        println("deferred fail error " + error.description)
    }
    
    func locationManager(manager: CLLocationManager!, didEnterRegion region: CLRegion!) {
        println("Entering region " + region.identifier)
    }
    
    func locationManager(manager: CLLocationManager!, didExitRegion region: CLRegion!) {
        println("Exiting region " + region.identifier)
    }
    
    func locationManager(manager: CLLocationManager!, didStartMonitoringForRegion region: CLRegion!) {
        let lat = (region as CLCircularRegion).center.latitude
        let lng = (region as CLCircularRegion).center.longitude
        let radius = (region as CLCircularRegion).radius
        
        println("Starting monitoring for region \(region) lat \(lat) lng \(lng)")
    }
    
    func locationManager(manager: CLLocationManager, didDetermineState state: CLRegionState, forRegion region: CLRegion) {
        println("State for region " + region.identifier)
    }
    
    func locationManager(manager: CLLocationManager, monitoringDidFailForRegion region: CLRegion!, withError error: NSError!) {
        println("Monitoring region " + region.identifier + " failed " + error.description)
    }
    
    
}