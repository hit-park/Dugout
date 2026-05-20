//
//  AttendeeJoinDTOs.swift
//  DugoutLineupFeature
//
//  Lineup 모듈 자체 — Match 모듈의 동명 DTO 와 별개로 자체 minimal 정의 (Feature 독립 원칙).
//  GET /matches/{matchId}/attendance + GET /teams/{teamId}/members 응답의 필요 필드만 포함.
//

import Foundation

struct AttendanceSummaryRefDTO: Decodable, Sendable {
    let votes: [AttendanceVoteRefDTO]
}

struct AttendanceVoteRefDTO: Decodable, Sendable {
    let userId: Int64
    let nickname: String
    let status: String

    enum CodingKeys: String, CodingKey {
        case nickname, status
        case userId = "user_id"
    }
}

struct TeamMemberRefDTO: Decodable, Sendable {
    let userId: Int64
    let nickname: String
    let jerseyNumber: Int?
    let isActive: Bool

    enum CodingKeys: String, CodingKey {
        case nickname
        case userId        = "user_id"
        case jerseyNumber  = "jersey_number"
        case isActive      = "is_active"
    }
}
