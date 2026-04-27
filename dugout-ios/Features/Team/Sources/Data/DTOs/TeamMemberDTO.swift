//
//  TeamMemberDTO.swift
//  DugoutTeamFeature
//

import Foundation

/// GET /api/v1/teams/{id}/members 응답 / 가입/수정 응답.
struct TeamMemberDTO: Decodable, Sendable {
    let id: Int64
    let userId: Int64
    let nickname: String
    let profileImgUrl: String?
    let role: String
    let jerseyNumber: Int?
    let positions: [String]
    let isActive: Bool
    let joinedAt: Date

    enum CodingKeys: String, CodingKey {
        case id, nickname, role, positions
        case userId = "user_id"
        case profileImgUrl = "profile_img_url"
        case jerseyNumber = "jersey_number"
        case isActive = "is_active"
        case joinedAt = "joined_at"
    }

    func toDomain() -> TeamMember {
        TeamMember(
            id: id,
            userId: userId,
            nickname: nickname,
            profileImgUrl: profileImgUrl,
            role: TeamRole(rawValue: role) ?? .member,
            jerseyNumber: jerseyNumber,
            positions: positions,
            isActive: isActive,
            joinedAt: joinedAt
        )
    }
}
