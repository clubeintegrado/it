//
//  Error+Extensions.swift
//  Shared
//
//  Created by Manly Man on 11/23/19.
//  Copyright © 2019 Innomalist. All rights reserved.
//

import Foundation
import StatusAlert

public extension HTTPStatusCode {
    func showAlert() {
        // Creating StatusAlert instance
        let statusAlert = StatusAlert()
        statusAlert.title = NSLocalizedString("message.title.error", value: "Error Happened", comment: "Default title for any error occured")
        statusAlert.message = self.localizedDescription
        statusAlert.canBePickedOrDismissed = true
        statusAlert.image = UIImage(named: "alert_error")
        statusAlert.showInKeyWindow()
    }
}
