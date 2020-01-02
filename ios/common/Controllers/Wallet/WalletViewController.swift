//
//  DriverWalletViewController.swift
//  Rider
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import UIKit
import Eureka
import Stripe
import Braintree
import BraintreeDropIn

class WalletViewController: FormViewController {
    var paymentField: STPPaymentCardTextField?
    var amount: Double?
    var currency: String?
    var wallet: [Wallet] = []
    let amounts: [Double] = [5,20,50]
    var methods: [PaymentGateway] = []
    var selectedMethod: PaymentGateway!
    var currentCreditSection: Section {
        get {
            return (self.form.sectionBy(tag: "sec_currrent_credit"))!
        }
    }
    var methodsRow: SegmentedRow<String> {
        get {
            return (self.form.rowBy(tag: "methods") as? SegmentedRow<String>)!
        }
    }
    var amountsRow: SegmentedRow<String> {
        get {
            return (self.form.rowBy(tag: "amounts") as? SegmentedRow<String>)!
        }
    }
    var cardRow: StripeRow {
        get {
            return (self.form.rowBy(tag: "card") as? StripeRow)!
        }
    }
    var amountRow: StepperRow {
        get {
            return (self.form.rowBy(tag: "amount") as? StepperRow)!
        }
    }
    var creditsRow: PushRow<String> {
        get {
            return (self.form.rowBy(tag: "credits") as? PushRow<String>)!
        }
    }
    var currencyRow: PushRow<String> {
        get {
            return (self.form.rowBy(tag: "currency") as? PushRow<String>)!
        }
    }
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
        LoadingOverlay.shared.showOverlay(view: self.view)
        WalletInfo().execute() { result in
            LoadingOverlay.shared.hideOverlayView()
            switch result {
            case .success(let methods):
                self.methods = methods.gateways
                self.wallet = methods.wallet
                if methods.wallet.count < 1 {
                    self.currentCreditSection.hidden = true
                    self.currentCreditSection.evaluateHidden()
                } else {
                    self.creditsRow.options = self.wallet.map() {
                        let formatter = NumberFormatter()
                        formatter.locale = Locale.current
                        formatter.numberStyle = .currency
                        formatter.currencyCode = $0.currency
                        return formatter.string(from: NSNumber(value: $0.amount!))!
                    }
                    self.creditsRow.value = self.creditsRow.options![0]
                    self.creditsRow.updateCell()
                }
                if self.methods.count == 0 {
                    DialogBuilder.alertOnError(message: "No Payment Method is enabled.")
                    self.navigationController?.popViewController(animated: true)
                    return
                } else if self.methods.count == 1 {
                    self.methodsRow.hidden = true
                    self.methodsRow.evaluateHidden()
                } else {
                    self.methodsRow.options = self.methods.map(){ return $0.title }
                    self.methodsRow.value = self.methodsRow.options![0]
                    self.methodsRow.updateCell()
                }
                self.selectedMethod = self.methods[0]
                if self.selectedMethod.type != .Braintree {
                    self.cardRow.hidden = false
                    self.cardRow.evaluateHidden()
                }
                if self.amount != nil {
                    self.amountRow.value = self.amount
                    self.currencyRow.value = self.currency
                    if self.methods.count == 1 && self.methods[0].type == .Braintree {
                        self.selectedMethod = self.methods[0]
                        self.startBraintreePayment()
                        return
                    }
                    self.amountRow.disabled = true
                    self.amountRow.evaluateDisabled()
                    self.amountsRow.hidden = true
                    self.amountsRow.evaluateHidden()
                }
                
            case .failure(let error):
                error.showAlert()
            }
        }
        form +++ Section("Credit") {
            $0.tag = "sec_currrent_credit"
        }
        <<< PushRow<String>() {
            $0.tag = "credits"
            $0.title = "Credits"
            $0.selectorTitle = "Current Credit"
        }
        form +++ Section(NSLocalizedString("wallet.section.method",value: "Add credit", comment: "")) {
            $0.tag = "sec_add_credit"
            }
            <<< SegmentedRow<String>() {
                $0.tag = "methods"
                $0.options = []
            }.onChange { row in
                if row.cell.segmentedControl.selectedSegmentIndex < 0 {
                    return
                }
                self.selectedMethod = self.methods[row.cell.segmentedControl.selectedSegmentIndex]
                self.cardRow.hidden = Condition(booleanLiteral:self.selectedMethod.type == .Braintree)
                self.cardRow.evaluateHidden()
            }
            <<< StripeRow() { row in
                row.tag = "card"
                row.hidden = true
                row.cellUpdate { cell, _row in
                    self.paymentField = cell.paymentField
                }
            }
            <<< StepperRow() {
                $0.tag = "amount"
                $0.value = self.amount ?? 0
                $0.title = NSLocalizedString("wallet.field.amount.description", value: "Topup amount", comment: "Wallet's amount field description")
            }
            <<< PushRow<String>() {
                $0.tag = "currency"
                $0.title = "Currency"
                $0.selectorTitle = "Select Currency"
                $0.options = Locale.commonISOCurrencyCodes
                $0.value = "USD"
            }
            <<< SegmentedRow<String>() {
                $0.tag = "amounts"
                $0.cell.segmentedControl.isMomentary = true
                $0.cell.segmentedControl.addTarget(self, action: #selector(self.selectedPresetValue), for: .valueChanged)
                $0.options = self.amounts.map({return String($0)})
        }
    }
    
    @objc func selectedPresetValue(r: UISegmentedControl) {
        self.amountsRow.value = String((self.amountRow.value ?? 0) + Double(self.amounts[(r.selectedSegmentIndex)]))
        self.amountsRow.updateCell()
    }
    
    @objc func selectedMethodChanged(r: UISegmentedControl) {
        self.selectedMethod = self.methods[r.selectedSegmentIndex]
    }
    
    func startBraintreePayment() {
        self.showDropIn(clientTokenOrTokenizationKey: selectedMethod.publicKey!)
    }
    
    func showDropIn(clientTokenOrTokenizationKey: String) {
        LoadingOverlay.shared.showOverlay(view: self.view)
        let request =  BTDropInRequest()
        let dropIn = BTDropInController(authorization: clientTokenOrTokenizationKey, request: request) { (controller, result, error) in
            if (error != nil) {
                LoadingOverlay.shared.hideOverlayView()
                DialogBuilder.alertOnError(message: (error?.localizedDescription)!)
            } else if (result?.isCancelled == true) {
                LoadingOverlay.shared.hideOverlayView()
                DialogBuilder.alertOnError(message: NSLocalizedString("alert.error.user.canceled", value: "User Canceled", comment: "alert for user canceling payment"))
            } else if let result = result {
                self.doPayment(method: "braintree", token: (result.paymentMethod?.nonce)!, amount: self.amount!)
            }
            controller.dismiss(animated: true, completion: nil)
        }
        self.present(dropIn!, animated: true, completion: nil)
    }
    
    func startStripePayment() {
        STPPaymentConfiguration.shared().publishableKey = selectedMethod.publicKey!
        guard let _paymentField = paymentField, let _ = _paymentField.cardNumber else {
            DialogBuilder.alertOnError(message: NSLocalizedString("alert.error.card.missing", value: "Card Info Missing", comment: "alert for card info not being entered"))
            return
        }
        let stripeCard = STPCardParams()
        stripeCard.number = _paymentField.cardParams.number
        stripeCard.expMonth = _paymentField.cardParams.expMonth as! UInt
        stripeCard.expYear = _paymentField.cardParams.expYear as! UInt
        stripeCard.cvc = _paymentField.cardParams.cvc
        stripeCard.name = _paymentField.cardParams.token
        STPAPIClient.shared().createToken(withCard: stripeCard) { (token: STPToken?, error: Error?) in
            guard let token = token, error == nil else {
                LoadingOverlay.shared.hideOverlayView()
                let dialog = DialogBuilder.getDialogForMessage(message: error.debugDescription,completion: nil)
                self.present(dialog, animated: true, completion: nil)
                return
            }
            self.doPayment(method: "stripe", token: token.tokenId, amount: self.amount!)
        }
    }
    
    func startFlutterwavePayment() {
        guard let _paymentField = paymentField, let cNumber = _paymentField.cardNumber else {
            DialogBuilder.alertOnError(message: NSLocalizedString("alert.error.card.missing", value: "Card Info Missing", comment: "alert for card info not being entered"))
            return
        }
        let token = "{\"cardNumber\":\(cNumber),\"cvv\":\(paymentField!.cvc!),\"expiryMonth\":\(paymentField!.expirationMonth),\"expiryYear\":\(paymentField!.expirationYear)}"
        self.doPayment(method: "flutterwave", token: token, amount: self.amount!)
    }
    
    @IBAction func onCheckoutClicked(_ sender: Any) {
        guard let amountRowValue = self.amountRow.value else {
            DialogBuilder.alertOnError(message: NSLocalizedString("alert.error.amount.missing", value: "Amount Missing", comment: "Amount is not entered for payment"))
            return
        }
        self.amount = Double(amountRowValue)
        //LoadingOverlay.shared.showOverlay(view: self.navigationController?.view)
        //DispatchQueue.main.asyncAfter(deadline: .now() + 30) {
        //    LoadingOverlay.shared.hideOverlayView()
        //}
        switch self.selectedMethod.type {
        case .Stripe:
            self.startStripePayment()
            
        case .Braintree:
            self.startBraintreePayment()
            
        case .Flutterwave:
            self.startFlutterwavePayment()
        }
        
    }
    
    func doPayment(method: String, token: String, amount: Double) {
        WalletTopUp(gatewayId: selectedMethod.id, currency: currencyRow.value!,token: token, amount: amount).execute { result in
            LoadingOverlay.shared.hideOverlayView()
            switch result {
            case .success(_):
                _ = self.navigationController?.popViewController(animated: true)
                DialogBuilder.alertOnSuccess(message:NSLocalizedString("message.payment.succeeded", value: "Payment Successful", comment: "Alert shown after successful payment."))
                
            case .failure(let error):
                let dialog =  DialogBuilder.getDialogForMessage(message: error.localizedDescription,completion: nil)
                self.present(dialog, animated: true,completion: nil)
            }
        }
    }
}
