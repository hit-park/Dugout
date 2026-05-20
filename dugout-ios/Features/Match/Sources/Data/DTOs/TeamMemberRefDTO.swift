//
//  TeamMemberRefDTO.swift
//  DugoutMatchFeature
//
//  백엔드 TeamMemberResponse 매핑 (부분 집합).
//  positions/joinedAt 필드는 Decodable 이 unknown key 로 자동 무시 — Jackson 기본 동작과 호환.
//

import Foundation

struct TeamMemberRefDTO: Decodable, Sendable {
    let id: Int64
    let userId: Int64
    let nickname: String
    let profileImgUrl: String?
    let role: String
    let jerseyNumber: Int?
    let isActive: Bool

    enum CodingKeys: String, CodingKey {
        case id, nickname, role
        case userId         = "user_id"
        case profileImgUrl  = "profile_img_url"
        case jerseyNumber   = "jersey_number"
        case isActive       = "is_active"
    }

    func toDomain() -> TeamMemberRef? {
        guard let parsedRole = TeamRole(rawValue: role) else { return nil }
        return TeamMemberRef(
            id: id,
            userId: userId,
            nickname: nickname,
            profileImgUrl: profileImgUrl,
            jerseyNumber: jerseyNumber,
            role: parsedRole,
            isActive: isActive
        )
    }
}
