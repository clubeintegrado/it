//
//  WalletTopUp.swift
//  Shared
//
//  Created by Manly Man on 11/23/19.
//  Copyright © 2019 Innomalist. All rights reserved.
//

import Foundation

public final class WalletTopUp: SocketRequest {
    public typealias ResponseType = EmptyClass
    public var params: [Any]?
    
    public init(gatewayId: Int, currency: String, token: String, amount: Double) {
        self.params = [gatewayId, currency, token, amount]
    }
    
    required public init() {}
    
}
