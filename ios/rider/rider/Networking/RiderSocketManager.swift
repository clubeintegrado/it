//
//  RiderSocketManager.swift
//  Rider
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import Foundation
import CoreLocation
import SocketIO

/*class RiderSocketManager: CommonSocketPaths {
    var delegateMessage: MessageReceivedDelegate?
    
    var socket : SocketIOClient
    var manager: SocketManager
    static let shared = RiderSocketManager()
    init() {
        manager = SocketManager(socketURL: URL(string: AppDelegate.info["ServerAddress"] as! String)!)
        socket = manager.socket(forNamespace: "/riders")
    }
    
    func connect(token:String, notificationId: String, completionHandler:@escaping ()->Void) {
        socket.on("driverInLocation") { data, ack in
            NotificationCenter.default.post(name: .driverInLocation, object: nil)
        }
        socket.on("startTravel") { data, ack in
            let travel = try! Travel(from: data[0])
            NotificationCenter.default.post(name: .serviceStarted, object: travel)
        }
        socket.on("messageReceived") { data, ack in
            if let delegate = self.delegateMessage {
                let message = try! ChatMessage(from: data[0])
                delegate.messageReceived(message: message)
            }
        }
        socket.on("cancelTravel") { data, ack in
            NotificationCenter.default.post(name: .serviceCanceled, object: nil)
        }
        socket.on("riderInfoChanged") { data, ack in
            let rider = try! Rider(from: data[0])
            NotificationCenter.default.post(name: .riderInfoChanged, object: rider)
            AppConfig.shared.user = rider
            let enc = try! PropertyListEncoder().encode(AppConfig.shared)
            let encodedData = NSKeyedArchiver.archivedData(withRootObject: enc)
            UserDefaults.standard.set(encodedData, forKey:"settings")
        }
        socket.on("travelInfoReceived") { data, ack in
            let location = try! CLLocationCoordinate2D(from: data[0])
            NotificationCenter.default.post(name: .travelInfoReceived, object: location)
        }
        socket.on("finishedTaxi") { data, ack in
            NotificationCenter.default.post(name: .serviceFinished, object: data)
        }
        socket.on("driverAccepted") {data, ack in
            let travel = try! Travel(from: data[0] as Any)
            // let myDict: [String: Any] = ["driver": driver]
            NotificationCenter.default.post(name: .newDriverAccepted, object: travel)
        }
        socket.connect()
    }
}
*/
