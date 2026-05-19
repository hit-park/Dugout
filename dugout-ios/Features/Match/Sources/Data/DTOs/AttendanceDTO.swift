//
//  AttendanceDTO.swift
//  DugoutMatchFeature
//
//  백엔드 AttendanceResponse / AttendanceSummaryResponse 매핑.
//  Spring Boot Jackson SNAKE_CASE 글로벌 설정이므로 CodingKeys 명시.
//

import Foundation

struct AttendanceVoteDTO: Decodable, Sendable {
    let id: Int64
    let matchId: Int64
    let userId: Int64
    let nickname: String
    let status: String
    let reason: String?
    let respondedAt: Date

    enum CodingKeys: String, CodingKey {
        case id, nickname, status, reason
        case matchId      = "match_id"
        case userId       = "user_id"
        case respondedAt  = "responded_at"
    }

    func toDomain() -> AttendanceVote? {
        guard let parsedStatus = AttendanceStatus(rawValue: status) else { return nil }
        return AttendanceVote(
            id: id,
            matchId: matchId,
            userId: userId,
            nickname: nickname,
            status: parsedStatus,
            reason: reason,
            respondedAt: respondedAt
        )
    }
}

struct AttendanceSummaryDTO: Decodable, Sendable {
    let matchId: Int64
    let totalMembers: Int
    let respondedCount: Int
    let pendingCount: Int
    let statusCounts: [String: Int]
    let votes: [AttendanceVoteDTO]

    enum CodingKeys: String, CodingKey {
        case matchId         = "match_id"
        case totalMembers    = "total_members"
        case respondedCount  = "responded_count"
        case pendingCount    = "pending_count"
        case statusCounts    = "status_counts"
        case votes
    }

    func toDomain() -> AttendanceSummary {
        var counts: [AttendanceStatus: Int] = [:]
        for status in AttendanceStatus.allCases {
            counts[status] = statusCounts[status.rawValue] ?? 0
        }
        return AttendanceSummary(
            matchId: matchId,
            totalMembers: totalMembers,
            respondedCount: respondedCount,
            pendingCount: pendingCount,
            statusCounts: counts,
            votes: votes.compactMap { $0.toDomain() }
        )
    }
}
