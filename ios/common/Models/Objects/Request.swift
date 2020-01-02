//
//  Travel.swift
//
//  Copyright (c) Minimalistic Apps. All rights reserved.
//

import Foundation
import CoreLocation

public final class Request: Codable, Hashable {
    public static func == (lhs: Request, rhs: Request) -> Bool {
        return lhs.id == rhs.id
    }
    
    public static var shared = Request()
    
    /*enum CodingKeys: String, CodingKey {
        case addresses = "addresses"
        case points = "points"
        case durationReal = "duration_real"
        case distanceReal = "distance_real"
        case requestTime = "request_time"
        case etaPickup = "eta_pickup"
        case expectedTimestamp = "expected_timestamp"
        case startTimestamp = "start_timestamp"
        case finishTimestamp = "finish_timestamp"
        case driver = "driver"
        case coupon = "coupon"
        case rating = "rating"
        case costBest = "cost_best"
        case rider = "rider"
        case distanceBest = "distance_best"
        case status = "status"
        case durationBest = "duration_best"
        case cost = "cost"
        case costAfterCoupon = "costAfterCoupon"
        case id = "id"
        case isHidden = "is_hidden"
        case log = "log"
        case service = "service"
        case confirmationCode = "confirmation_code"
    }*/
    
    // MARK: Properties
    public var addresses: [String] = []
    public var points: [CLLocationCoordinate2D] = []
    public var durationReal: Int?
    public var distanceReal: Int?
    public var requestTimestamp: Double?
    public var expectedTimestamp: Double?
    public var currency: String?
    public var startTimestamp: Double?
    public var etaPickup: Double?
    public var finishTimestamp: Double?
    public var driver: Driver?
    public var coupon: Coupon?
    public var rating: Int?
    public var costBest: Double?
    public var costAfterCoupon: Double?
    public var rider: Rider?
    public var distanceBest: Int?
    public var status: Status?
    public var durationBest: Int?
    public var cost: Double?
    public var id: Int?
    public var isHidden: Int?
    public var log: String?
    public var service: Service?
    public var confirmationCode: Int?
    
    public enum Status: String, Codable {
        case Requested = "Requested"
        case NotFound = "NotFound"
        case NoCloseFound = "NoCloseFound"
        case Found = "Found"
        case DriverAccepted = "DriverAccepted"
        case WaitingForPrePay = "WaitingForPrePay"
        case DriverCanceled = "DriverCanceled"
        case RiderCanceled = "RiderCanceled"
        case Started = "Started"
        case WaitingForPostPay = "WaitingForPostPay"
        case WaitingForReview = "WaitingForReview"
        case Finished = "Finished"
        case Booked = "Booked"
        case Expired = "Expired"
    }
    
    public func hash(into hasher: inout Hasher) {
        hasher.combine(id)
    }
}


