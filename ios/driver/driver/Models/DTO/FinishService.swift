//
//  FinishService.swift
//  driver
//
//  Copyright © 2019 minimal. All rights reserved.
//

import Foundation


class FinishService: Codable {
    public var log: String?
    public var cost: Double?
    public var distance: Int?
    public var confirmationCode: Int?
    
    enum CodingKeys: String, CodingKey {
        case log = "log"
        case cost = "cost"
        case distance = "distance"
        case confirmationCode = "confirmation_code"
        
    }
    
    public init(cost: Double, log: String? = "", distance: Int, confirmationCode: Int) {
        self.log = log
        self.cost = cost
        self.confirmationCode = confirmationCode
        self.distance = distance
    }
    
    public init(cost: Double, log: String? = "", distance: Int) {
        self.log = log
        self.cost = cost
        self.distance = distance
    }
}
