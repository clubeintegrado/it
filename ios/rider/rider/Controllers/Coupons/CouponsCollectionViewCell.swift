//
//  TripHistoryCollectionViewCell.swift
//  rider
//
//  Copyright © 2018 minimal. All rights reserved.
//

import UIKit


class CouponsCollectionViewCell: UICollectionViewCell {
    public var coupon: Coupon?
    @IBOutlet weak var title: UILabel!
    @IBOutlet weak var textdescription: UILabel!
    @IBOutlet weak var background: GradientView!
    
    override func layoutSubviews() {
        super.layoutSubviews()
        if coupon != nil {
            title.text = coupon?.title
            textdescription.text = coupon?.descriptionValue
        }
    }
}
