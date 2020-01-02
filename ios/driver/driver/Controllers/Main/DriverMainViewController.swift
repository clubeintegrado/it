//
//  DriverMainViewController.swift
//  Driver
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import UIKit
import MapKit

import iCarousel

class DriverMainViewController: UIViewController, CLLocationManagerDelegate {
    @IBOutlet weak var buttonStatus: UISwitch!
    @IBOutlet weak var requestsList: iCarousel!
    @IBOutlet weak var map: MKMapView!
    var shouldUpdateRequests = true
    var initialRefresh = true
    
    var requests : [Request] = []
    var locationManager = CLLocationManager()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        NotificationCenter.default.addObserver(self, selector: #selector(self.onRequestReceived), name: .requestReceived, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(self.onRequestCanceled), name: .requestCanceled, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(self.refreshRequests), name: .connectedAfterForeground, object: nil)
        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBestForNavigation
        locationManager.distanceFilter = 1
        locationManager.activityType = .automotiveNavigation
        locationManager.requestAlwaysAuthorization()
        locationManager.startUpdatingLocation()
        requestsList.dataSource = self
        requestsList.delegate = self
        requestsList.type = .rotary
        GetCurrentRequestInfo().execute() { result in
            switch result {
            case .success(let response):
                if response.request.status! == .WaitingForReview {
                    return
                }
                let title = NSLocalizedString("message.title.default",value: "Message", comment: "Message Default Title")
                let message = NSLocalizedString("message.content.unfinished.travel", value: "There is an unfinished travel found. After tapping OK you will be redirected to Travel screen.", comment: "")
                let dialog = UIAlertController(title: title, message: message, preferredStyle: .alert)
                dialog.addAction(UIAlertAction(title: NSLocalizedString("message.button.ok",value: "OK", comment: "Message OK button"), style: .default) { action in
                    Request.shared = response.request
                    self.performSegue(withIdentifier: "startTravel", sender: nil)
                })
                self.present(dialog, animated: true)
                
            // Failure here means there is no waiting travel to be done
            case .failure(_):
                break
            }
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        shouldUpdateRequests = true
    }
    
    @objc func refreshRequests() {
        GetAvailableRequests().execute() { result in
            switch result {
            case .success(let response):
                self.buttonStatus.isOn = true
                self.requests = response
                self.requestsList.reloadData()
                
            case .failure(_):
                self.buttonStatus.isOn = false
                //error.showAlert()
            }
        }
    }
    
    @IBAction func onDriverStatusClicked(_ sender: UISwitch) {
        buttonStatus.isEnabled = false
        UpdateStatus(turnOnline: sender.isOn).execute() { result in
            self.buttonStatus.isEnabled = true
            switch result {
            case .success(_):
                if self.buttonStatus.isOn {
                    LocationUpdate(jwtToken: UserDefaultsConfig.jwtToken!, location: self.map.userLocation.coordinate).execute() {_ in
                        self.refreshRequests()
                    }
                }
                
            case .failure(let error):
                self.buttonStatus.isOn = !self.buttonStatus.isOn
                error.showAlert()
            }
        }
    }
    
    @IBAction func onMenuClicked(_ sender: Any) {
        NotificationCenter.default.post(name: .menuClicked, object: nil)
    }
    
    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let userLocation:CLLocation = locations[0] as CLLocation
        if (buttonStatus.isOn || initialRefresh) {
            LocationUpdate(jwtToken: UserDefaultsConfig.jwtToken!, location: userLocation.coordinate).execute() { _ in
                if self.shouldUpdateRequests {
                    self.refreshRequests()
                    self.shouldUpdateRequests = false
                }
            }
        }
        if initialRefresh {
            initialRefresh = false
        }
        let region = MKCoordinateRegion(center: userLocation.coordinate, latitudinalMeters: 1000, longitudinalMeters: 1000)
        map.setRegion(region, animated: true)
    }
    
    @objc func onRequestReceived(_ notification: Notification) {
        if let request = notification.object as? Request {
            requests.append(request)
            requestsList.reloadData()
        }
    }
    
    @objc func onRequestCanceled(_ notification: Notification) {
        refreshRequests()
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}

extension DriverMainViewController: iCarouselDataSource, iCarouselDelegate {
    func numberOfItems(in carousel: iCarousel) -> Int {
        return requests.count
    }
    
    func carousel(_ carousel: iCarousel, viewForItemAt index: Int, reusing view: UIView?) -> UIView {
        let vc = Bundle.main.loadNibNamed("RequestCard", owner: self, options: nil)?[0] as! RequestCard
        
        let travel = requests[requests.index(requests.startIndex, offsetBy: index)]
        vc.request = travel
        vc.labelPickupLocation.text = travel.addresses[0]
        //vc.labelDestinationLocation.text = travel.destinationAddress
        let distanceDriver = CLLocation.distance(from: map.userLocation.coordinate, to: travel.points[0])
        vc.labelFromYou.text = String(format: NSLocalizedString("base.distance.km", value: "%.1f km", comment: "Default format for distances in km"), Double(distanceDriver) / 1000.0)
        vc.labelDistance.text = String(format: NSLocalizedString("base.distance.km", value: "%.1f km", comment: "Default format for distances in km"), Double(travel.distanceBest!) / 1000.0)
        vc.labelCost.text = MyLocale.formattedCurrency(amount: travel.costBest!, currency: travel.currency!)
        vc.delegate = self
        vc.layer.cornerRadius = 8
        vc.layer.shadowOpacity = 0.2
        vc.layer.shadowOffset = CGSize(width: 0, height: 0)
        vc.layer.shadowRadius = 4.0
        let shadowRect: CGRect = vc.bounds
        // let _ = vc.constraintUser.setMultiplier(multiplier: CGFloat(Double(distanceDriver) / Double(travel.distanceBest!)))
        let _ = vc.constraintUser.setConstant(constant: CGFloat((distanceDriver.binade - Double(travel.distanceBest!)) / (distanceDriver + Double(travel.distanceBest!)) * 100))
        vc.layer.shadowPath = UIBezierPath(rect: shadowRect).cgPath
        return vc
    }
    
    func carousel(_ carousel: iCarousel, valueFor option: iCarouselOption, withDefault value: CGFloat) -> CGFloat {
        if (option == .spacing) {
            return value * 1.1
        }
        return value
    }
}
extension DriverMainViewController: DriverRequestCardDelegate {
    func accept(request: Request) {
        LoadingOverlay.shared.showOverlay(view: self.navigationController?.view)
        AcceptOrder(requestId: request.id!).execute() { result in
            LoadingOverlay.shared.hideOverlayView()
            switch result {
            case .success(let response):
                self.requests.removeAll()
                self.requestsList.reloadData()
                Request.shared = response
                self.performSegue(withIdentifier: "startTravel", sender: nil)
                
            case .failure(let error):
                if error.status == .OrderAlreadyTaken {
                    self.refreshRequests()
                }
                error.showAlert()
            }
        }
    }
    
    func reject(request: Request) {
        requests.removeAll() {req in return req.id == request.id }
        requestsList.reloadData()
    }
}

extension NSLayoutConstraint {
    /**
     Change multiplier constraint
     
     - parameter multiplier: CGFloat
     - returns: NSLayoutConstraint
     */
    func setMultiplier(multiplier:CGFloat) -> NSLayoutConstraint {
        
        NSLayoutConstraint.deactivate([self])
        
        let newConstraint = NSLayoutConstraint(
            item: firstItem!,
            attribute: firstAttribute,
            relatedBy: relation,
            toItem: secondItem,
            attribute: secondAttribute,
            multiplier: multiplier,
            constant: constant)
        
        newConstraint.priority = priority
        newConstraint.shouldBeArchived = self.shouldBeArchived
        newConstraint.identifier = self.identifier
        
        NSLayoutConstraint.activate([newConstraint])
        return newConstraint
    }
    
    func setConstant(constant:CGFloat) -> NSLayoutConstraint {
        
        NSLayoutConstraint.deactivate([self])
        
        let newConstraint = NSLayoutConstraint(
            item: firstItem!,
            attribute: firstAttribute,
            relatedBy: relation,
            toItem: secondItem,
            attribute: secondAttribute,
            multiplier: multiplier,
            constant: constant)
        
        newConstraint.priority = priority
        newConstraint.shouldBeArchived = self.shouldBeArchived
        newConstraint.identifier = self.identifier
        
        NSLayoutConstraint.activate([newConstraint])
        return newConstraint
    }
}
