//
//  Finish.swift
//  driver
//
//  Created by Manly Man on 11/23/19.
//  Copyright © 2019 minimal. All rights reserved.
//

import Foundation


class Finish: SocketRequest {
    typealias ResponseType = FinishResult
    
    var params: [Any]?
    
    required public init(confirmationCode: Int?, distance: Int, log: String) {
        self.params = [[
            "confirmationCode": confirmationCode ?? 0,
            "distance": distance,
            "log": log
        ]]
    }
}

struct FinishResult: Codable {
    public var status: Bool
}
