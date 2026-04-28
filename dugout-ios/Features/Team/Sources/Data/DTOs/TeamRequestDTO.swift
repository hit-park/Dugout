//
//  TeamRequestDTO.swift
//  DugoutTeamFeature
//

import Foundation

/// POST /api/v1/teams 요청 본문.
struct CreateTeamRequestDTO: Encodable, Sendable {
    let name: String
    let region: String
    let division: Int
    let activityDays: [String]
    let activityTime: String?
    let lineupMode: String

    enum CodingKeys: String, CodingKey {
        case name, region, division
        case activityDays = "activity_days"
        case activityTime = "activity_time"
        case lineupMode = "lineup_mode"
    }
}

/// POST /api/v1/teams/join 요청 본문.
struct JoinTeamRequestDTO: Encodable, Sendable {
    let inviteCode: String

    enum CodingKeys: String, CodingKey {
        case inviteCode = "invite_code"
    }
}

/// POST /api/v1/teams/{id}/invite 응답.
struct InviteCodeResponseDTO: Decodable, Sendable {
    let inviteCode: String

    enum CodingKeys: String, CodingKey {
        case inviteCode = "invite_code"
    }
}

/// PUT /api/v1/teams/{id} 요청 본문.
struct UpdateTeamRequestDTO: Encodable, Sendable {
    let name: String?
    let region: String?
    let division: Int?
    let activityDays: [String]?
    let activityTime: String?
    let lineupMode: String?

    enum CodingKeys: String, CodingKey {
        case name, region, division
        case activityDays = "activity_days"
        case activityTime = "activity_time"
        case lineupMode = "lineup_mode"
    }
}
