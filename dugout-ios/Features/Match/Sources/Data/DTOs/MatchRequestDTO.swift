//
//  MatchRequestDTO.swift
//  DugoutMatchFeature
//

import Foundation

struct CreateMatchRequestDTO: Encodable, Sendable {
    let matchDate: String       // "yyyy-MM-dd"
    let matchTime: String       // "HH:mm:ss"
    let gatherTime: String?
    let opponentName: String?
    let opponentTeamId: Int64?
    let groundId: Int64?
    let groundName: String?
    let voteDeadline: Date?
    let memo: String?

    enum CodingKeys: String, CodingKey {
        case matchDate      = "match_date"
        case matchTime      = "match_time"
        case gatherTime     = "gather_time"
        case opponentName   = "opponent_name"
        case opponentTeamId = "opponent_team_id"
        case groundId       = "ground_id"
        case groundName     = "ground_name"
        case voteDeadline   = "vote_deadline"
        case memo
    }
}
