//
//  TravelViewController.swift
//  Rider
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import UIKit

import MapKit

class TravelViewController: UIViewController, CouponsViewDelegate, MKMapViewDelegate {
    @IBOutlet weak var map: MKMapView!
    @IBOutlet weak var labelCost: UILabel!
    @IBOutlet weak var labelTime: UILabel!
    @IBOutlet weak var buttonCall: ColoredButton!
    @IBOutlet weak var buttonMessage: ColoredButton!
    @IBOutlet weak var buttonCancel: ColoredButton!
    
    var pickupMarker = MKPointAnnotation()
    var destinationMarkers: [MKPointAnnotation] = []
    var driverMarker = MKPointAnnotation()
    var timer: Timer!
    @IBOutlet weak var confirmationBarButton: UIBarButtonItem!
    @IBOutlet weak var tabBar: UISegmentedControl!
    @IBOutlet weak var backgroundView: UIView!
    @IBOutlet weak var viewStatistics: UIView!
    @IBOutlet weak var viewDriver: UIStackView!
    @IBOutlet weak var textDriverName: UILabel!
    @IBOutlet weak var textPlateNumber: UILabel!
    @IBOutlet weak var textCarModel: UILabel!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        NotificationCenter.default.addObserver(self, selector: #selector(self.onServiceStarted), name: .serviceStarted, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(self.onServiceCanceled), name: .serviceCanceled, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(self.onServiceFinished), name: .serviceFinished, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(self.onTravelInfoReceived), name: .travelInfoReceived, object: nil)
        NotificationCenter.default.addObserver(self, selector:#selector(self.requestRefresh), name: .connectedAfterForeground, object: nil)
        textDriverName.text = "\(Request.shared.driver?.firstName ?? "") \(Request.shared.driver?.lastName ?? "")"
        textCarModel.text = "\(Request.shared.driver?.car?.title ?? "") \(Request.shared.driver?.carColor ?? "")"
        textPlateNumber.text = Request.shared.driver?.carPlate
        tabBar.addTarget(self, action: #selector(selectedTabItem), for: .valueChanged)
        map.layoutMargins = UIEdgeInsets(top: 50, left: 0, bottom: 290, right: 0)
        let blurEffect = UIBlurEffect(style: UIBlurEffect.Style.regular)
        let blurEffectView = UIVisualEffectView(effect: blurEffect)
        blurEffectView.frame = backgroundView.bounds
        blurEffectView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        backgroundView.addSubview(blurEffectView)
        let maskLayer = CAShapeLayer()
        maskLayer.path = UIBezierPath(roundedRect: view.bounds, byRoundingCorners: [.topLeft, .topRight], cornerRadii: CGSize(width: 10, height: 10)).cgPath
        self.backgroundView.layer.mask = maskLayer
        driverMarker = MKPointAnnotation()
        map.delegate = self
        self.navigationItem.hidesBackButton = true
        if let canConfirm = Request.shared.service?.canEnableVerificationCode {
            if canConfirm == 1 {
                self.navigationItem.rightBarButtonItem = nil
            }
        } else {
            self.navigationItem.rightBarButtonItem = nil
        }
        if var cost = Request.shared.costBest {
            if let coupon = Request.shared.coupon {
                cost = cost * Double(100 - coupon.discountPercent!) / 100
                cost = cost - Double(coupon.discountFlat!)
            }
            if let service = Request.shared.service, (service.feeEstimationMode == .Dynamic || service.feeEstimationMode == .RangedStrict || service.feeEstimationMode == .Ranged) {
                self.labelCost.text = "~\(MyLocale.formattedCurrency(amount: cost, currency: Request.shared.currency!))"
            } else {
                self.labelCost.text = MyLocale.formattedCurrency(amount: cost, currency: Request.shared.currency!)
            }
        }
        timer = Timer.scheduledTimer(timeInterval: 1, target: self, selector: #selector(self.onEachSecond), userInfo: nil, repeats: true)
    }
    
    @objc func selectedTabItem(sender: UISegmentedControl) {
        if sender.selectedSegmentIndex == 1 {
            viewDriver.isHidden = true
            viewStatistics.isHidden = false
        } else {
            viewDriver.isHidden = false
            viewStatistics.isHidden = true
        }
    }
    
    @IBAction func onMessageTapped(_ sender: UIButton) {
        let vc = ChatViewController()
        vc.sender = Request.shared.driver!
        self.navigationController!.pushViewController(vc, animated: true)
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        /*if Request.shared.status == Request.Status.WaitingForPostPay || Request.shared.status == Request.Status.WaitingForPrePay {
            self.navigationController?.popViewController(animated: false)
            return
        }*/
        self.requestRefresh()
    }
    
    @objc func onEachSecond() {
        let now = Date()
        //let etaTime = status == .Started ? Request.shared.startTimestamp?.addingTimeInterval(Double(Request.shared.durationBest!)) : eta
        let etaInterval = Request.shared.startTimestamp != nil ? (Request.shared.startTimestamp! / 1000) + Double(Request.shared.durationBest!) : Request.shared.etaPickup! / 1000
        let etaTime = Date(timeIntervalSince1970: etaInterval)
        if etaTime <= now {
            labelTime.text = NSLocalizedString("travel.time.anyminute", value: "Soon!", comment: "When driver is coming later than expected.")
        } else {
            let formatter = DateComponentsFormatter()
            formatter.allowedUnits = [.minute, .second]
            formatter.unitsStyle = .short
            labelTime.text = formatter.string(from: now, to: etaTime)
        }
    }
    
    @objc private func requestRefresh() {
        GetCurrentRequestInfo().execute() { result in
            switch result {
            case .success(let response):
                Request.shared = response.request
                self.refreshScreen(driverLocation: response.driverLocation)
                
            case .failure(_):
                self.navigationController?.popViewController(animated: false)
            }
        }
    }
    
    private func refreshScreen(travel: Request = Request.shared, driverLocation: CLLocationCoordinate2D?) {
        switch travel.status! {
        case .RiderCanceled, .DriverCanceled:
            let alert = UIAlertController(title: "Success", message: "Service Has Been Canceled.", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "Allright!", style: .default) { action in
                _ = self.navigationController?.popViewController(animated: true)
            })
            present(alert, animated: true)
            break;
            
        case .DriverAccepted:
            pickupMarker.coordinate = travel.points[0]
            map.addAnnotation(pickupMarker)
            if let _location = driverLocation {
                driverMarker.coordinate = _location
                map.addAnnotation(driverMarker)
                map.showAnnotations([pickupMarker, driverMarker], animated: true)
            } else {
                let region = MKCoordinateRegion(center: travel.points[0], latitudinalMeters: 1000, longitudinalMeters: 1000)
                map.setRegion(region, animated: true)
            }
            break;
            
        case .Started:
            buttonCall.isHidden = true
            buttonMessage.isHidden = true
            buttonCancel.isHidden = true
            map.removeAnnotation(pickupMarker)
            for (index, point) in travel.points.enumerated() {
                if index == 0 {
                    continue;
                }
                let p = MKPointAnnotation()
                p.coordinate = point
                destinationMarkers.append(p)
                map.addAnnotation(p)
            }
            if driverLocation != nil || destinationMarkers.count > 1 {
                if(driverLocation != nil) {
                    driverMarker.coordinate = driverLocation!
                    map.addAnnotation(driverMarker)
                    destinationMarkers.append(driverMarker)
                    map.showAnnotations(destinationMarkers, animated: true)
                    destinationMarkers.removeLast()
                } else {
                    map.showAnnotations(destinationMarkers, animated: true)
                }
            } else {
                let region = MKCoordinateRegion(center: travel.points[1], latitudinalMeters: 1000, longitudinalMeters: 1000)
                map.setRegion(region, animated: true)
            }
            break;
            
        case .WaitingForPostPay:
            if let vc = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "Wallet") as? WalletViewController {
                vc.amount = Request.shared.costAfterCoupon
                vc.currency = Request.shared.currency
                self.navigationController?.pushViewController(vc, animated: true)
            }
            break
            
        case .WaitingForReview:
            let storyboard = UIStoryboard(name: "Main", bundle: nil)
            let controller = storyboard.instantiateViewController(withIdentifier: "finishedViewController")
            self.navigationController?.pushViewController(controller, animated: false)
            break
            
        case .Finished:
            DialogBuilder.alertOnSuccess(message: "Done!")
            self.navigationController?.popViewController(animated: true)
            
        default:
            let alert = UIAlertController(title: "Error", message: "Unknown status: \(travel.status!.rawValue)", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "Allright!", style: .default) { action in
                _ = self.navigationController?.popViewController(animated: true)
            })
            self.present(alert, animated: true)
        }
    }
    
    @IBAction func onCancelTapped(_ sender: UIButton) {
        Cancel().execute() { result in
            switch result {
            case .success(_):
                Request.shared.status = .RiderCanceled
                self.refreshScreen(driverLocation: nil)
                
            case .failure(let error):
                error.showAlert()
            }
        }
    }
    
    @IBAction func onWalletTapped(_ sender: UIButton) {
        if let vc = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "Wallet") as? WalletViewController {
            vc.amount = Request.shared.costAfterCoupon
            vc.currency = Request.shared.currency
            self.navigationController!.pushViewController(vc, animated: true)
        }
    }
    
    @objc func onServiceStarted(_ notification: Notification) {
        Request.shared = notification.object as! Request
        let location = driverMarker.coordinate.latitude != 0 ? driverMarker.coordinate : nil
        refreshScreen(driverLocation: location)
    }
    
    @objc func onServiceCanceled(_ notification: Notification) {
        Request.shared.status = .DriverCanceled
        refreshScreen(driverLocation: nil)
    }
    
    @objc func onServiceFinished(_ notification: Notification) {
        let obj = notification.object as! [Any]
        Request.shared.status = (obj[0] as! Bool) == true ? Request.Status.Finished : Request.Status.WaitingForPostPay
        Request.shared.costAfterCoupon = obj[1] as? Double
        refreshScreen(driverLocation: nil)
    }
    
    @objc func onTravelInfoReceived(_ notification: Notification) {
        refreshScreen(driverLocation: (notification.object as! CLLocationCoordinate2D))
    }
    
    @IBAction func onSelectCouponClicked(_ sender: UIButton) {
        if let vc = UIStoryboard(name: "Main", bundle: nil).instantiateViewController(withIdentifier: "CouponsCollectionViewController") as? CouponsCollectionViewController
        {
            vc.selectMode = true
            vc.delegate = self
            self.navigationController!.pushViewController(vc, animated: true)
        }
    }
    
    func didSelectedCoupon(_ coupon: Coupon) {
        ApplyCoupon(code: coupon.code!).execute() { result in
            switch result {
            case .success(let response):
                DialogBuilder.alertOnSuccess(message: "Done!")
                if let service = Request.shared.service, (service.feeEstimationMode == .Dynamic || service.feeEstimationMode == .RangedStrict || service.feeEstimationMode == .Ranged) {
                    self.labelCost.text = "~\(MyLocale.formattedCurrency(amount: response, currency: Request.shared.currency!))"
                } else {
                    self.labelCost.text = MyLocale.formattedCurrency(amount: response, currency: Request.shared.currency!)
                }
                
            case .failure(let error):
                error.showAlert()
            }
        }
    }
    
    enum MarkerType: String {
        case pickup = "pickup"
        case dropoff = "dropOff"
        case driver = "driver"
    }
    
    func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
        guard let annotation = annotation as? MKPointAnnotation else { return nil }
        let identifier = annotation == pickupMarker ? MarkerType.pickup : (annotation == driverMarker ? MarkerType.driver : MarkerType.dropoff)
        var view: MKMarkerAnnotationView
        if let dequeuedView = mapView.dequeueReusableAnnotationView(withIdentifier: identifier.rawValue) as? MKMarkerAnnotationView {
            dequeuedView.annotation = annotation
            view = dequeuedView
        } else {
            view = MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: identifier.rawValue)
            switch(identifier) {
            case .pickup:
                view.glyphImage = UIImage(named: "annotation_glyph_home")
                view.markerTintColor = UIColor(hex: 0x009688)
                break;
                
            case .dropoff:
                view.markerTintColor = UIColor(hex: 0xFFA000)
                break;
                
            default:
                view.glyphImage = UIImage(named: "annotation_glyph_car")
            }
        }
        return view
    }
    
    @IBAction func onCallTouched(_ sender: UIButton) {
        if let call = Request.shared.driver?.mobileNumber, let url = URL(string: "tel://\(call)"), UIApplication.shared.canOpenURL(url) {
            UIApplication.shared.open(url)
        }
    }
    
    @IBAction func enableConfirmationClicked(_ sender: UIBarButtonItem) {
        EnableVerification().execute() { result in
            switch result {
            case .success(let response):
                let message = UIAlertController(title: "Done", message: "Confirmation code service is enabled. Driver will need to enter \(response) to finish service.", preferredStyle: .alert)
                message.addAction(UIAlertAction(title: "OK", style: .default))
                self.present(message, animated: true)
                
            case .failure(let error):
                error.showAlert()
            }
        }
    }
}
