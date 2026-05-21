//
//  LineupDTO.swift
//  DugoutLineupFeature
//
//  백엔드 LineupResponse / LineupEntryResponse 매핑.
//

import Foundation

struct LineupDTO: Decodable, Sendable {
    let id: Int64
    let matchId: Int64
    let teamId: Int64
    let isAiGenerated: Bool
    let isConfirmed: Bool
    let confirmedAt: Date?
    let entries: [LineupEntryDTO]

    enum CodingKeys: String, CodingKey {
        case id, entries
        case matchId        = "match_id"
        case teamId         = "team_id"
        case isAiGenerated  = "is_ai_generated"
        case isConfirmed    = "is_confirmed"
        case confirmedAt    = "confirmed_at"
    }

    func toDomain() -> Lineup? {
        Lineup(
            id: id,
            matchId: matchId,
            teamId: teamId,
            isAiGenerated: isAiGenerated,
            isConfirmed: isConfirmed,
            confirmedAt: confirmedAt,
            entries: entries.compactMap { $0.toDomain() }
        )
    }
}

struct LineupEntryDTO: Decodable, Sendable {
    let id: Int64
    let userId: Int64
    let nickname: String
    let position: String
    let battingOrder: Int?
    let isBench: Bool

    enum CodingKeys: String, CodingKey {
        case id, nickname, position
        case userId        = "user_id"
        case battingOrder  = "batting_order"
        case isBench       = "is_bench"
    }

    func toDomain() -> LineupEntry? {
        guard let pos = BaseballPosition(rawValue: position) else { return nil }
        return LineupEntry(
            id: id,
            userId: userId,
            nickname: nickname,
            position: pos,
            battingOrder: battingOrder,
            isBench: isBench
        )
    }
}
