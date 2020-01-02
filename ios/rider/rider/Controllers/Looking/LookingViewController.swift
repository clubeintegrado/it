//
//  LookingViewController.swift
//  rider
//
//  Copyright © 2019 minimalistic apps. All rights reserved.
//

import UIKit

import Lottie

class LookingViewController: UIViewController {
    @IBOutlet weak var ViewLoading: UIView!
    var animationView: AnimationView!
    weak var delegate: LookingDelegate?
    @IBOutlet weak var textStatus: UILabel!
    @IBOutlet weak var blurView: UIView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        NotificationCenter.default.addObserver(self, selector: #selector(self.onDriverAccepted), name: .newDriverAccepted, object: nil)
        NotificationCenter.default.addObserver(self, selector:#selector(self.refreshPage), name: .connectedAfterForeground, object: nil)
        let blurEffect = UIBlurEffect(style: UIBlurEffect.Style.regular)
        let blurEffectView = UIVisualEffectView(effect: blurEffect)
        blurEffectView.frame = self.view.bounds
        blurEffectView.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        self.blurView.addSubview(blurEffectView)
        animationView = AnimationView(name: "car")
        animationView.contentMode = .scaleAspectFit
        animationView.translatesAutoresizingMaskIntoConstraints = false
        animationView.loopMode = .loop
        animationView.backgroundColor = UIColor.clear
        ViewLoading.addSubview(animationView)
        // Do any additional setup after loading the view.
    }
    
    override func updateViewConstraints() {
        super.updateViewConstraints()
        let horizontalConstraint = NSLayoutConstraint(item: animationView!, attribute: NSLayoutConstraint.Attribute.centerX, relatedBy: NSLayoutConstraint.Relation.equal, toItem: ViewLoading, attribute: NSLayoutConstraint.Attribute.centerX, multiplier: 1, constant: 0)
        let verticalConstraint = NSLayoutConstraint(item: animationView!, attribute: NSLayoutConstraint.Attribute.centerY, relatedBy: NSLayoutConstraint.Relation.equal, toItem: ViewLoading, attribute: NSLayoutConstraint.Attribute.centerY, multiplier: 1, constant: 0)
        let widthConstraint = NSLayoutConstraint(item: animationView!, attribute: NSLayoutConstraint.Attribute.width, relatedBy: NSLayoutConstraint.Relation.equal, toItem: ViewLoading, attribute: NSLayoutConstraint.Attribute.width, multiplier: 1, constant: 0)
        let heightConstraint = NSLayoutConstraint(item: animationView!, attribute: NSLayoutConstraint.Attribute.height, relatedBy: NSLayoutConstraint.Relation.equal, toItem: ViewLoading, attribute: NSLayoutConstraint.Attribute.height, multiplier: 1, constant: 0)
        ViewLoading.addConstraints([horizontalConstraint,verticalConstraint,widthConstraint,heightConstraint])
    }
    
    @objc func refreshPage() {
        GetCurrentRequestInfo().execute() { result in
            switch result {
            case .success(let response):
                Request.shared = response.request
                self.refreshUI()
                
            case .failure(_):
                break
            }
        }
    }
    
    func refreshUI() {
        let travel = Request.shared
        if travel.status == nil {
            self.animationView.play()
            self.textStatus.text = "Looking for drivers..."
            return
        }
        switch(travel.status!) {
        case .DriverAccepted:
            self.dismiss(animated: true, completion: nil)
            self.delegate?.found()
            break
            
        case .DriverCanceled, .RiderCanceled:
            self.dismiss(animated: true, completion: nil)
            self.delegate?.cancel()
            break
            
        case .Booked:
            self.animationView.play()
            let dateFormatter = DateFormatter()
            dateFormatter.dateStyle = .none
            dateFormatter.timeStyle = .short
            if let expected = travel.expectedTimestamp {
                self.textStatus.text = "Travel Booked for " + dateFormatter.string(from: Date(timeIntervalSince1970: TimeInterval(expected)))
            } else {
                self.textStatus.text = "Travel Booked"
            }
            
        case .Expired:
            let alert = UIAlertController(title: "Message", message: "Sadly your request wasn't accepted in appropriate time and it is expired now.", preferredStyle: .alert)
            alert.addAction(UIAlertAction(title: "OK", style: .default) { action in
                self.dismiss(animated: true, completion: nil)
                self.delegate?.cancel()
            })
            self.present(alert, animated: true)
            
        default:
            self.animationView.play()
            self.textStatus.text = "Looking for drivers..."
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        self.animationView.play()
        refreshPage()
    }
    
    @IBAction func onCancelRequestClicked(_ sender: UIButton) {
        CancelRequest().execute() { result in
            switch result {
            case .success(_):
                self.dismiss(animated: true, completion: nil)
                
            case .failure(let error):
                error.showAlert()
            }
        }
        
    }
    
    @objc func onDriverAccepted(_ notification: Notification) {
        if let travel = notification.object as? Request {
            Request.shared = travel
            dismiss(animated: true, completion: nil)
            self.delegate?.found()
        }
    }
    

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */
    
}

protocol LookingDelegate: class{
    func found()
    func cancel()
}
