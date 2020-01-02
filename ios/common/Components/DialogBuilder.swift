//
//  DialogBuilder.swift
//  common
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import StatusAlert

public class DialogBuilder {
    public enum ButtonOptions {
        case OK_CANCEL, RETRY_CANCEL, OK
    }
    public enum DialogResult {
        case OK, CANCEL, RETRY
    }
    
    public static func getDialogForMessage(message:String, completion:((DialogResult)->Void)?) -> UIAlertController {
        // Prepare the popup assets
        let title = NSLocalizedString("message.title.default",value: "Message", comment: "Message Default Title")
        let dialog = UIAlertController(title: title, message: message, preferredStyle: .alert)
        dialog.addAction(UIAlertAction(title: NSLocalizedString("message.button.ok",value: "OK", comment: "Message OK button"), style: .default) { action in
            if let c = completion {
                c(.OK)
            }
        })
        return dialog
    }
    
    public static func alertOnError(message:String) {
        // Creating StatusAlert instance
        let statusAlert = StatusAlert()
        statusAlert.title = NSLocalizedString("message.title.error", value: "Error Happened", comment: "Default title for any error occured")
        statusAlert.message = message
        statusAlert.canBePickedOrDismissed = true
        statusAlert.image = UIImage(named: "alert_error")
        // Presenting created instance
        statusAlert.showInKeyWindow()
    }
    
    public static func alertOnSuccess(message:String) {
        // Creating StatusAlert instance
        let statusAlert = StatusAlert()
        //statusAlert.title = "Info"
        statusAlert.image = UIImage(named: "alert_success")
        statusAlert.message = message
        statusAlert.canBePickedOrDismissed = true
        
        // Presenting created instance
        statusAlert.showInKeyWindow()
    }
}
