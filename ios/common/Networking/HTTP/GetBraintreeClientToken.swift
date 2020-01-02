//
//  GetBraintreeClientToken.swift
//  driver
//
//  Created by Manly Man on 11/25/19.
//  Copyright © 2019 minimal. All rights reserved.
//

import Foundation

public class GetGatewayPublishableToken: HTTPRequest {
    public var path: String = "getway_public_token"
    
    public typealias ResponseType = String
    public var params: [String : Any]?
    
    public init(gatewayId: Int, jwtToken: String) {
        self.params = [
            "gatewayId": gatewayId,
            "token": jwtToken
        ]
    }
}
