//
//  TravelTableViewController.swift
//  Rider
//
//  Copyright © 2018 minimalistic apps. All rights reserved.
//

import UIKit


class TripHistoryCollectionViewController: UICollectionViewController, UICollectionViewDelegateFlowLayout {
    //MARK: Properties
    let cellIdentifier = "TripHistoryCollectionViewCell"
    
    var travels = [Request]()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        let nibCell = UINib(nibName: cellIdentifier, bundle: nil)
        collectionView?.register(nibCell, forCellWithReuseIdentifier: cellIdentifier)
        self.refreshList(self)
    }

    @IBAction func refreshList(_ sender: Any) {
        GetRequestHistory().execute() { result in
            switch result {
            case .success(let response):
                self.travels = response
                self.collectionView?.reloadData()
                
            case .failure(let error):
                error.showAlert()
            }
        }
    }
    
    func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
        let kWhateverHeightYouWant = 180
        return CGSize(width: collectionView.bounds.size.width, height: CGFloat(kWhateverHeightYouWant))
    }
    
    override func numberOfSections(in tableView: UICollectionView) -> Int {
        return 1
    }

    override func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return travels.count
    }
    
    override func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        guard let cell = self.collectionView?.dequeueReusableCell(withReuseIdentifier: cellIdentifier, for: indexPath) as? TripHistoryCollectionViewCell  else {
            fatalError("The dequeued cell is not an instance of TripHistoryTableCell.")
        }
        // Fetches the appropriate meal for the data source layout.
        let travel = travels[indexPath.row]
        let dateFormatter = DateFormatter()
        dateFormatter.dateStyle = .medium
        dateFormatter.timeStyle = .medium
        cell.pickupLabel.text = travel.addresses[0]
        if travel.addresses.count > 1 {
            cell.destinationLabel.text = travel.addresses.last
        }
        if let startTimestamp = travel.startTimestamp {
            cell.startTimeLabel.text = dateFormatter.string(from: Date(timeIntervalSince1970: TimeInterval(startTimestamp / 1000)))
        }
        if let finishTimestamp = travel.finishTimestamp {
            cell.finishTimeLabel.text = dateFormatter.string(from: Date(timeIntervalSince1970: TimeInterval(finishTimestamp / 1000)))
        }
        cell.textCost.text = MyLocale.formattedCurrency(amount: travel.costAfterCoupon ?? 0, currency: travel.currency!)
        
        cell.textStatus.text = travel.status!.rawValue.splitBefore(separator: { $0.isUppercase }).map{String($0)}.joined(separator: " ")

        
        return cell
    }
}

extension Character {
    var isUpperCase: Bool { return String(self) == String(self).uppercased() }
}

extension Sequence {
    func splitBefore(
        separator isSeparator: (Iterator.Element) throws -> Bool
    ) rethrows -> [AnySequence<Iterator.Element>] {
        var result: [AnySequence<Iterator.Element>] = []
        var subSequence: [Iterator.Element] = []

        var iterator = self.makeIterator()
        while let element = iterator.next() {
            if try isSeparator(element) {
                if !subSequence.isEmpty {
                    result.append(AnySequence(subSequence))
                }
                subSequence = [element]
            }
            else {
                subSequence.append(element)
            }
        }
        result.append(AnySequence(subSequence))
        return result
    }
}
