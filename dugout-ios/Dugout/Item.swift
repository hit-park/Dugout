//
//  Item.swift
//  Dugout
//
//  Created by 박희태 on 4/21/26.
//

import Foundation
import SwiftData

@Model
final class Item {
    var timestamp: Date
    
    init(timestamp: Date) {
        self.timestamp = timestamp
    }
}
