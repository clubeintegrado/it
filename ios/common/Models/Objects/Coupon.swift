//
//  Coupon.swift
//
//  Created by Minimalistic apps on 11/15/18
//  Copyright (c) . All rights reserved.
//

import Foundation

public final class Coupon: Codable {

  public static var shared = Coupon()

  // MARK: Properties
  public var startAt: Date?
  public var isEnabled: Int?
  public var manyTimesUserCanUse: Int?
  public var descriptionValue: String?
  public var manyUsersCanUse: Int?
  private var numberIsFirstTravelOnly: Int?
    public var isFirstTravelOnly: Bool { get {
        return self.numberIsFirstTravelOnly == 0 ? false : true
        }}
  public var expirationAt: Date?
  public var maximumCost: Int?
  public var id: Int?
  public var code: String?
  public var creditGift: Int?
  public var title: String?
  public var discountFlat: Int?
  public var minimumCost: Int?
  public var discountPercent: Int?
}
