//
//  Promotion.swift
//
//  Created by Minimalistic Apps on 11/15/18
//  Copyright (c) . All rights reserved.
//

import Foundation

public final class Promotion: Codable {

  // MARK: Declaration for string constants to be used to decode and also serialize.
  enum CodingKeys: String, CodingKey {
    case descriptionValue = "description"
    case title = "title"
    case id = "id"
    case startTimestamp = "start_timestamp"
    case expirationTimestamp = "expiration_timestamp"
  }

  // MARK: Properties
  public var descriptionValue: String?
  public var title: String?
  public var id: Int?
  public var startTimestamp: String?
  public var expirationTimestamp: String?
}
