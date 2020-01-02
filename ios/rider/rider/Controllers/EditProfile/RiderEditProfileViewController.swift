//
//  EditProfileViewController.swift
//  Rider
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import UIKit
import Eureka

import ImageRow
import Kingfisher

class RiderEditProfileViewController: FormViewController {
    var downloading = false
    var rider: Rider!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        rider = try! Rider(from: UserDefaultsConfig.user!)
        form +++ Section(NSLocalizedString("profile.section.image", value: "Images", comment: "Profile's image section header"))
            <<< ImageRow() {
                $0.tag = "profile_row"
                $0.title = NSLocalizedString("profile.field.image", value: "Profile Image", comment: "Profile's image field title")
                $0.allowEditor = true
                $0.useEditedImage = true
                if let address = rider.media?.address {
                    let url = URL(string: Config.Backend + address.replacingOccurrences(of: " ", with: "%20"))
                    ImageDownloader.default.downloadImage(with: url!){ result in
                        switch result {
                        case .success(let value):
                            self.downloading = true
                            (self.form.rowBy(tag: "profile_row")! as! ImageRow).value = value.image
                            (self.form.rowBy(tag: "profile_row")! as! ImageRow).reload()
                            self.downloading = false
                        case .failure(let error):
                            print(error)
                        }
                    }
                }
                $0.sourceTypes = .PhotoLibrary
                $0.clearAction = .no
                }.onChange {
                    if(!self.downloading) {
                        let data = $0.value?.jpegData(compressionQuality: 0.7)
                        $0.title = NSLocalizedString("profile.field.image.uploading", value: "Uploading, Please wait...", comment: "Uploading image state")
                        $0.disabled = true
                        $0.reload()
                        UpdateProfileImage(data: data!).execute() { result in
                            self.form.rowBy(tag: "profile_row")!.title = NSLocalizedString("profile.field.image", value: "Profile Image", comment: "Profile's image field title")
                            self.form.rowBy(tag: "profile_row")!.disabled = false
                            self.form.rowBy(tag: "profile_row")!.reload()
                            switch result {
                            case .success(let response):
                                DialogBuilder.alertOnSuccess(message: NSLocalizedString("alert.success.upload", value: "Uploading Profile Image was successful.", comment: "Uploading profile image successful alert message"))
                                self.rider.media = response
                                UserDefaultsConfig.user = try! self.rider.asDictionary()
                                
                            case .failure(let error):
                                error.showAlert()
                            }
                        }
                    }
                }
                .cellUpdate { cell, row in
                    cell.accessoryView?.layer.cornerRadius = 17
                    cell.accessoryView?.frame = CGRect(x: 0, y: 0, width: 34, height: 34)
            }
            +++ Section(NSLocalizedString("profile.section.info", value: "Basic Info", comment: "Profile Basic Info section header"))
            <<< PhoneRow(){
                $0.title = NSLocalizedString("profile.field.mobile.number", value: "Mobile Number", comment: "Profile Mobile Number field title")
                $0.disabled = true
                $0.value = String(rider.mobileNumber!)
            }
            <<< EmailRow(){
                $0.title = NSLocalizedString("profile.field.email", value: "E-Mail", comment: "Profile Email field title")
                $0.value = rider.email
            }
            <<< TextRow(){
                $0.title = NSLocalizedString("profile.field.name", value: "Name", comment: "Profile Name field")
                $0.value = rider.firstName
                $0.placeholder = NSLocalizedString("profile.field.name.first", value: "First Name", comment: "Profile First Name Field")
                }.onChange {
                    self.rider.firstName = $0.value
            }
            <<< TextRow(){
                $0.title = " "
                $0.placeholder = NSLocalizedString("profile.field.name.last", value: "Last Name", comment: "Profile Last Name field")
                $0.value = rider.lastName
                }.onChange {
                    self.rider.lastName = $0.value
            }
            +++ Section(NSLocalizedString("profile.section.additional", value: "Additional Info", comment: "Profile's additional Info section"))
            <<< PushRow<String>() {
                $0.title = NSLocalizedString("profile.field.gender", value: "Gender", comment: "Profile's gender field title")
                $0.selectorTitle = NSLocalizedString("profile.field.gender.selector.title", value: "Select Your Gender", comment: "Profile's gender field selector title")
                $0.options = ["Male","Female","Unspecified"]
                $0.value = rider.gender    // initially selected
                }.onChange {
                    self.rider.gender = $0.value! as String
            }
            <<< TextRow(){
                $0.title = NSLocalizedString("profile.field.address", value: "Address", comment: "Profile Address field title")
                $0.value = rider.address
                }.onChange { self.rider.address = $0.value }
    }
    @IBAction func onSaveProfileClicked(_ sender: Any) {
        UpdateProfile(user: self.rider).execute() { result in
            switch result {
            case .success(_):
                _ = self.navigationController?.popViewController(animated: true)
                UserDefaultsConfig.user = try! self.rider.asDictionary()
                
            case .failure(let error):
                error.showAlert()
            }
        }
    }
}
