//
//  ChatViewController.swift
//  rider
//
//  Created by Manly Man on 7/21/19.
//  Copyright © 2019 minimal. All rights reserved.
//

import UIKit

import MessageKit
import InputBarAccessoryView

class ChatViewController: MessagesViewController {
    func messageReceived(message: ChatMessage) {
        messages.append(message)
        self.messagesCollectionView.reloadData()
        self.messagesCollectionView.scrollToBottom(animated: true)
    }
    
    var messages: [ChatMessage] = []
    var sender: SenderType!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        self.title = NSLocalizedString("controller.chat.title", value: "Chat", comment: "Title of chat screen")
        messagesCollectionView.messagesDataSource = self
        messagesCollectionView.messagesLayoutDelegate = self
        messagesCollectionView.messagesDisplayDelegate = self
        self.messageInputBar.delegate = self
        GetMessages().execute() { result in
            switch result {
            case .success(let response):
                self.messages = response
                self.messagesCollectionView.reloadData()
                break;
                
            case .failure(_):
                
                break;
            }
            
        }
    }
    
    func insertMessage(_ message: ChatMessage) {
        messages.append(message)
        self.messagesCollectionView.reloadData()
        self.messagesCollectionView.scrollToBottom(animated: true)
    }
}

extension ChatViewController: MessagesDataSource {

    func currentSender() -> SenderType {
        return sender
    }
    
    func numberOfSections(in messagesCollectionView: MessagesCollectionView) -> Int {
        return messages.count
    }
    
    func messageForItem(at indexPath: IndexPath, in messagesCollectionView: MessagesCollectionView) -> MessageType {
        return messages[indexPath.section]
    }
}

extension ChatViewController: InputBarAccessoryViewDelegate {
    
    func inputBar(_ inputBar: InputBarAccessoryView, didPressSendButtonWith text: String) {
        
        // Here we can parse for which substrings were autocompleted
        let attributedText = messageInputBar.inputTextView.attributedText!
        let range = NSRange(location: 0, length: attributedText.length)
        attributedText.enumerateAttribute(.autocompleted, in: range, options: []) { (_, range, _) in
            
            let substring = attributedText.attributedSubstring(from: range)
            let context = substring.attribute(.autocompletedContext, at: 0, effectiveRange: nil)
            print("Autocompleted: `", substring, "` with context: ", context ?? [])
        }
        messageInputBar.inputTextView.text = String()
        messageInputBar.invalidatePlugins()
        
        // Send button activity animation
        messageInputBar.sendButton.startAnimating()
        messageInputBar.inputTextView.placeholder = "Sending..."
        SendMessage(content: text).execute() { result in
            switch result {
            case .success(let response):
                self.messageInputBar.sendButton.stopAnimating()
                self.messageInputBar.inputTextView.placeholder = "Aa"
                self.insertMessage(response)
                self.messagesCollectionView.scrollToBottom(animated: true)
                
            case .failure(let error):
                self.messageInputBar.sendButton.stopAnimating()
                error.showAlert()
            }
        }
    }
    
    private func insertMessages(_ messages: [ChatMessage]) {
        for message in messages {
            insertMessage(message)
        }
    }
    
    
}


extension ChatViewController: MessagesDisplayDelegate, MessagesLayoutDelegate {}
