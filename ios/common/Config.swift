//
//  Config.swift
//  Shared
//
//  Created by Manly Man on 11/22/19.
//  Copyright © 2019 Innomalist. All rights reserved.
//

import Foundation

class Config {
    static var Backend: String {
        get {
            #if DEBUG
                return "http://localhost:8080/"
            #else
                return "http://localhost:3000/"
            #endif
        }
    }
    
    static var Version: String {
        get {
            return self.Info["CFBundleVersion"] as! String
        }
    }
    
    static var Info: [String:Any] {
        get {
            let path = Bundle.main.path(forResource: "Info", ofType: "plist")!
            return NSDictionary(contentsOfFile: path) as! [String: Any]
        }
    }
}
