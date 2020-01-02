//
//  Car.swift
//
//  Copyright (c) Minimalistic apps. All rights reserved.
//

import Foundation

public final class Car: Codable {

  // MARK: Declaration for string constants to be used to decode and also serialize.
  enum CodingKeys: String, CodingKey {
    case id = "id"
    case title = "title"
    case media = "media"
  }

  // MARK: Properties
  public var id: Int?
  public var title: String?
  public var media: Media?
}
