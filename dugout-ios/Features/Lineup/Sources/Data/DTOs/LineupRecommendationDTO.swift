//
//  LineupRecommendationDTO.swift
//  DugoutLineupFeature
//

import Foundation

struct LineupRecommendationDTO: Decodable, Sendable {
    let matchId: Int64
    let isAiGenerated: Bool
    let source: String
    let entries: [LineupEntryPayloadDTO]

    enum CodingKeys: String, CodingKey {
        case source, entries
        case matchId        = "match_id"
        case isAiGenerated  = "is_ai_generated"
    }
}

struct LineupEntryPayloadDTO: Decodable, Sendable {
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
}
