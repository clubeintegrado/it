//
//  ServerError.swift
//  Shared
//
//  Created by Manly Man on 11/22/19.
//  Copyright © 2019 Innomalist. All rights reserved.
//

import Foundation
import StatusAlert

public struct ServerError: Error, Codable {
    public var status: ErrorStatus
    public var message: String?
    
    public func showAlert() {
        // Creating StatusAlert instance
        let statusAlert = StatusAlert()
        statusAlert.title = NSLocalizedString("message.title.error", value: "Error Happened", comment: "Default title for any error occured")
        if self.status != .Unknown {
            statusAlert.message = self.status.rawValue
        } else if message != nil {
            statusAlert.message = message
        } else {
            statusAlert.message = "An unknown error happend"
        }
        statusAlert.canBePickedOrDismissed = true
        statusAlert.image = UIImage(named: "alert_error")
        statusAlert.showInKeyWindow()
    }
}

public enum ErrorStatus: String, Codable {
    case DistanceCalculationFailed = "DistanceCalculationFailed"
    case DriversUnavailable = "DriversUnavailable"
    case ConfirmationCodeRequired = "ConfirmationCodeRequired"
    case ConfirmationCodeInvalid = "ConfirmationCodeInvalid"
    case OrderAlreadyTaken = "OrderAlreadyTaken"
    case Unknown = "Unknown"
    case Networking = "Networking"
    case FailedEncoding = "FailedEncoding"
    case FailedToVerify = "FailedToVerify"
    case RegionUnsupported = "RegionUnsupported"
    case NoServiceInRegion = "NoServiceInRegion"
}
