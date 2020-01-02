//
//  DriverNavigationMenuViewController.swift
//  Driver
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import UIKit

import Kingfisher

class DriverNavigationMenuViewController : MenuViewController {
    let kCellReuseIdentifier = "MenuCell"
    let menuItems = ["Main"]
    
    @IBOutlet weak var imageUser: UIImageView!
    @IBOutlet weak var labelName: UILabel!
    @IBOutlet weak var labelBalance: UILabel!
    
    
    override var prefersStatusBarHidden: Bool {
        return false
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        imageUser.layer.cornerRadius = imageUser.frame.size.width / 2
        imageUser.clipsToBounds = true
        imageUser.layer.borderColor = UIColor.white.cgColor
        imageUser.layer.borderWidth = 3.0
    }
    
    override func viewDidAppear(_ animated: Bool) {
        let driver = try! Driver(from: UserDefaultsConfig.user!)
        if let driverImage = driver.media?.address {
            let processor = DownsamplingImageProcessor(size: imageUser.intrinsicContentSize) |> RoundCornerImageProcessor(cornerRadius: imageUser.intrinsicContentSize.width / 2)
            let url = URL(string: Config.Backend + driverImage.replacingOccurrences(of: " ", with: "%20"))
            imageUser.kf.setImage(with: url, placeholder: UIImage(named: "Nobody"), options: [
                .processor(processor),
                .scaleFactor(UIScreen.main.scale),
                .transition(.fade(0.5)),
                .cacheOriginalImage
            ]) { result in
                switch result {
                case .success(let value):
                    print("Task done for: \(value.source.url?.absoluteString ?? "")")
                case .failure(let error):
                    print("Job failed: \(error.localizedDescription)")
                }
            }
        }
        labelName.text = "\(driver.firstName == nil ? "" : driver.firstName!) \(driver.lastName == nil ? "" : driver.lastName!)"
        labelBalance.text = "\(driver.mobileNumber!)"
    }
    
    @IBAction func onWithdrawClicked(_ sender: UIButton) {
        let title = NSLocalizedString("question.request.payment.title", value: "Request Payment", comment: "")
        let message = NSLocalizedString("question.request.payment.description", value: "This action will move all your earnings to you bank account. Are you sure?", comment: "Asking user if is sure to request a payment")
        let dialog = UIAlertController(title: title, message: message, preferredStyle: .alert)
        dialog.addAction(UIAlertAction(title: NSLocalizedString("message.button.ok",value: "OK", comment: "Message OK button"), style: .default) { action in
            RequestPayment().execute() { result in
                switch result {
                case .success(_):
                    let popup = DialogBuilder.getDialogForMessage(message: NSLocalizedString("message.payment.requested", value: "Withdraw request was successfully applied. Your payment will be done soon.", comment: ""), completion: nil)
                    self.present(popup, animated: true, completion: nil)
                    
                case .failure(let error):
                    error.showAlert()
                }
            }
        })
        dialog.addAction(UIAlertAction(title: "Cancel", style: .cancel))
        self.present(dialog, animated: true)
    }
    
    @IBAction func onTravelsClicked(_ sender: UIButton) {
        guard let menuContainerViewController = self.menuContainerViewController else {
            return
        }
        if let vc = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "TripHistory") as? TripHistoryCollectionViewController {
            (menuContainerViewController.contentViewControllers[0] as! UINavigationController).pushViewController(vc, animated: true)
            menuContainerViewController.hideSideMenu()
        }
    }
        
    @IBAction func onTransactionsClicked(_ sender: UIButton) {
        guard let menuContainerViewController = self.menuContainerViewController else {
            return
        }
        if let vc = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "Transactions") as? TransactionsCollectionViewController {
            (menuContainerViewController.contentViewControllers[0] as! UINavigationController).pushViewController(vc, animated: true)
            menuContainerViewController.hideSideMenu()
        }
    }
    
    @IBAction func onWalletClicked(_ sender: UIButton) {
        guard let menuContainerViewController = self.menuContainerViewController else {
            return
        }
        if let vc = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "Wallet") as? WalletViewController {
            (menuContainerViewController.contentViewControllers[0] as! UINavigationController).pushViewController(vc, animated: true)
            menuContainerViewController.hideSideMenu()
        }
    }
    
    @IBAction func onAboutClicked(_ sender: UIButton) {
        guard let menuContainerViewController = self.menuContainerViewController else {
            return
        }
        
        menuContainerViewController.contentViewControllers[0].performSegue(withIdentifier: "showAbout", sender: nil)
        menuContainerViewController.hideSideMenu()
    }
    
    @IBAction func onExitClicked(_ sender: UIButton) {
        guard let menuContainerViewController = self.menuContainerViewController else {
            return
        }
        if let bundle = Bundle.main.bundleIdentifier {
            UserDefaults.standard.removePersistentDomain(forName: bundle)
            self.dismiss(animated: true, completion: nil)
            menuContainerViewController.hideSideMenu()
        }
    }
}
