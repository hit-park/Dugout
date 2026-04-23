//
//  TeamDTO.swift
//  DugoutTeamFeature
//

import Foundation

struct TeamDTO: Decodable, Sendable {
    let id: Int64
    let name: String
    let logoUrl: String?
    let region: String
    let division: Int
    let homeGroundId: Int64?
    let activityDays: [String]
    let activityTime: String?
    let inviteCode: String?
    let lineupMode: String
    let memberCount: Int
    let createdAt: Date

    enum CodingKeys: String, CodingKey {
        case id, name, region, division
        case logoUrl = "logo_url"
        case homeGroundId = "home_ground_id"
        case activityDays = "activity_days"
        case activityTime = "activity_time"
        case inviteCode = "invite_code"
        case lineupMode = "lineup_mode"
        case memberCount = "member_count"
        case createdAt = "created_at"
    }

    func toDomain() -> Team {
        Team(
            id: id,
            name: name,
            logoUrl: logoUrl,
            region: region,
            division: division,
            homeGroundId: homeGroundId,
            activityDays: activityDays,
            activityTime: activityTime,
            inviteCode: inviteCode,
            lineupMode: LineupMode(rawValue: lineupMode) ?? .balanced,
            memberCount: memberCount,
            createdAt: createdAt
        )
    }
}
