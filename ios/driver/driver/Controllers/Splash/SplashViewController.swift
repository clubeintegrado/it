//
//  SplashViewController.swift
//  Driver
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import UIKit
import Firebase
import FirebaseUI


class SplashViewController: UIViewController {
    @IBOutlet weak var indicatorLoading: UIActivityIndicatorView!
    @IBOutlet weak var textLoading: UILabel!
    @IBOutlet weak var buttonLogin: UIButton!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        NotificationCenter.default.addObserver(self, selector:#selector(self.showMainPage), name: .connectedAfterForeground, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(self.onConError), name: .connectionError, object: nil)
        if let token = UserDefaultsConfig.jwtToken {
            connectSocket(token: token)
        } else {
            self.loginState()
        }
    }
    
    func loginState() {
        indicatorLoading.isHidden = true
        textLoading.isHidden = true
        buttonLogin.isHidden = false
        buttonLogin.isEnabled = true
    }
    
    @objc func onConError(_ notification: Notification) {
        let err = notification.object as! ConnectionError
        connectionError(error: err)
    }
    
    func connectionError(error: ConnectionError) {
        switch error {
        case .NotFound:
            let title = NSLocalizedString("message.title.default",value: "Message", comment: "Message Default Title")
            let dialog = UIAlertController(title: title, message: "User Info not found. Do you want to register again?", preferredStyle: .alert)
            dialog.addAction(UIAlertAction(title: "Register", style: .default) { action in
                self.onLoginClicked(self.buttonLogin)
            })
            dialog.addAction(UIAlertAction(title: "Cancel", style: .cancel, handler: nil))
            self.present(dialog, animated: true, completion: nil)
            
        case .RegistrationIncomplete:
            self.showRegisterForm(jwtToken: UserDefaultsConfig.jwtToken!)
            
        default:
            let dlg = DialogBuilder.getDialogForMessage(message: error.rawValue, completion: nil)
            self.present(dlg, animated: true, completion: nil)
            self.loginState()
        }
    }
    
    func connectSocket(token:String) {
        InstanceID.instanceID().instanceID { (result, error) in
            SocketNetworkDispatcher.instance.connect(namespace: .Driver, token: token, notificationId: result?.token ?? "") { result in
                switch result {
                case .success(_):
                    self.performSegue(withIdentifier: "segueShowHost", sender: nil)
                    
                case .failure(let error):
                    self.connectionError(error: error)
                    
                }
            }
        }
    }
    
    @IBAction func onLoginClicked(_ sender: UIButton) {
        let auth = FUIAuth.defaultAuthUI()
        auth?.delegate = self
        let phoneAuth = FUIPhoneAuth(authUI: auth!)
        auth?.providers = [phoneAuth]
        phoneAuth.signIn(withPresenting: self, phoneNumber: nil)
    }
    
    @objc func showMainPage() {
        self.performSegue(withIdentifier: "segueShowHost", sender: nil)
    }
    
    func tryLogin(firebaseToken: String) {
        buttonLogin.isEnabled = false
        Login(firebaseToken: firebaseToken).execute() { result in
            switch result {
            case .success(let response):
                UserDefaultsConfig.jwtToken = response.token
                UserDefaultsConfig.user = try! response.user.asDictionary()
                if(response.user.status == Driver.Status.Offline || response.user.status == Driver.Status.Online) {
                    self.connectSocket(token: response.token)
                } else {
                    self.showRegisterForm(jwtToken: response.token)
                }
                break
                
            case .failure(let error):
                let dlg = DialogBuilder.getDialogForMessage(message: error.localizedDescription, completion: nil)
                self.present(dlg, animated: true, completion: nil)
            }
        }
    }
    
    func showRegisterForm(jwtToken: String) {
        GetRegisterInfo(jwtToken: jwtToken).execute() { result in
            switch result {
            case .success(let response):
                do {
                    UserDefaultsConfig.user = try response.driver.asDictionary()
                    UserDefaultsConfig.services = try response.services.map() { return try $0.asDictionary() }
                    self.buttonLogin.isHidden = true
                    self.textLoading.isHidden = true
                    self.indicatorLoading.isHidden = true
                    self.performSegue(withIdentifier: "showRegister", sender: nil)
                } catch {
                    let dlg = DialogBuilder.getDialogForMessage(message: "Couldn't decode well", completion: nil)
                    self.present(dlg, animated: true, completion: nil)
                }
                
            case .failure(let error):
                let dlg = DialogBuilder.getDialogForMessage(message: error.localizedDescription, completion: nil)
                self.present(dlg, animated: true, completion: nil)
                break
            }
        }
    }
}

extension SplashViewController: FUIAuthDelegate {
    func authUI(_ authUI: FUIAuth, didSignInWith user: User?, error: Error?) {
        if user == nil {
            return
        }
        user?.getIDTokenForcingRefresh(true) { idToken, error in
            if let error = error {
                print(error)
                return;
            }
            
            self.tryLogin(firebaseToken: idToken!)
        }
    }
}
