
// This file was generated from JSON Schema using quicktype, do not modify it directly.
// To parse the JSON, add this file to your project and do:
//
//   let chatMessage = try? newJSONDecoder().decode(ChatMessage.self, from: jsonData)

import Foundation
import UIKit
import MessageKit

// MARK: - ChatMessageElement
public class ChatMessage: Codable, MessageType {
    let id: Int?
    let sentAt: String?
    let content: String?
    let travelID: Int?
    let sentBy: String?
    let state: String?
    let travel: Request?

    public var sender: SenderType { get {
        if(self.sentBy == "driver") {
            return Request.shared.driver!
        } else {
            return Request.shared.rider!
        }
        }
    }
    
    public var messageId: String { get {
        return String(self.id!)
        }
    }
    
    public var sentDate: Date { get {
        return Date.distantPast
        }}
    
    public var kind: MessageKind { get {
        return MessageKind.text(self.content!)
        }
    }
    
    
    enum CodingKeys: String, CodingKey {
        case id = "id"
        case sentAt = "sent_at"
        case content = "content"
        case travelID = "travel_id"
        case sentBy = "sent_by"
        case state = "state"
        case travel = "travel"
    }
    
    init(id: Int?, sentAt: String?, content: String?, travelID: Int?, sentBy: String?, state: String?, travel: Request?) {
        self.id = id
        self.sentAt = sentAt
        self.content = content
        self.travelID = travelID
        self.sentBy = sentBy
        self.state = state
        self.travel = travel
    }
}

typealias ChatMessages = [ChatMessage]
