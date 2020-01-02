//
//  AboutViewController.swift
//  Rider
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import UIKit
import Eureka

class AboutViewController:FormViewController {
    override func viewDidLoad() {
        super.viewDidLoad()
        form +++ Section(header: NSLocalizedString("about.section.info", value: "Info", comment: ""), footer: NSLocalizedString("about-footer", value: "© 2020 Minimalistic Apps All rights reserved.", comment: ""))
            <<< LabelRow(){
                $0.title = NSLocalizedString("about.field.application.name", value: "Application Name", comment: "")
                $0.value = Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String
            }
            <<< LabelRow(){
                $0.title = NSLocalizedString("about.field.version", value: "Version", comment: "")
                $0.value = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
            }
            <<< LabelRow(){
                $0.title = NSLocalizedString("about.field.website", value: "Website", comment: "")
                $0.value = "http://www.ridy.io"
            }
            <<< LabelRow(){
                $0.title = NSLocalizedString("about.field.phone", value: "Phone Number", comment: "")
                $0.value = "-"
        }
    }
}
