//
//  AttendanceVote.swift
//  DugoutMatchFeature
//
//  팀원 1인의 출석 응답 1건.
//

import Foundation

public struct AttendanceVote: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64
    public let matchId: Int64
    public let userId: Int64
    public let nickname: String
    public let status: AttendanceStatus
    public let reason: String?
    public let respondedAt: Date

    public init(
        id: Int64,
        matchId: Int64,
        userId: Int64,
        nickname: String,
        status: AttendanceStatus,
        reason: String?,
        respondedAt: Date
    ) {
        self.id = id
        self.matchId = matchId
        self.userId = userId
        self.nickname = nickname
        self.status = status
        self.reason = reason
        self.respondedAt = respondedAt
    }
}
