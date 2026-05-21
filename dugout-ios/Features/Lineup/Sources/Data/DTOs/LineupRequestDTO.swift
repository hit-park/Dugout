//
//  LineupRequestDTO.swift
//  DugoutLineupFeature
//

import Foundation

struct LineupEntryPayloadEncodableDTO: Encodable, Sendable {
    let userId: Int64
    let position: String
    let battingOrder: Int?
    let isBench: Bool

    enum CodingKeys: String, CodingKey {
        case position
        case userId        = "user_id"
        case battingOrder  = "batting_order"
        case isBench       = "is_bench"
    }

    init(_ entry: LineupDraftEntry) {
        self.userId = entry.userId
        self.position = entry.position.rawValue
        self.battingOrder = entry.battingOrder
        self.isBench = entry.isBench
    }
}

struct SaveLineupRequestDTO: Encodable, Sendable {
    let entries: [LineupEntryPayloadEncodableDTO]

    init(_ request: SaveLineupRequest) {
        self.entries = request.entries.map(LineupEntryPayloadEncodableDTO.init)
    }
}
