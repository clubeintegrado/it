//
//  ApplyCoupon.swift
//  rider
//
//  Created by Manly Man on 11/26/19.
//  Copyright © 2019 minimal. All rights reserved.
//

import UIKit


class ApplyCoupon: SocketRequest {
    typealias ResponseType = Double
    var params: [Any]?
    
    init(code: String) {
        self.params = [code]
    }
}
