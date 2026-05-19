//
//  AttendanceSummary.swift
//  DugoutMatchFeature
//
//  GET /api/v1/matches/{matchId}/attendance 응답에 대응.
//  팀 전체 인원 수, 응답 카운트, 상태별 카운트, 응답자 리스트를 묶음.
//

import Foundation

public struct AttendanceSummary: Sendable, Equatable {
    public let matchId: Int64
    public let totalMembers: Int
    public let respondedCount: Int
    public let pendingCount: Int
    public let statusCounts: [AttendanceStatus: Int]
    public let votes: [AttendanceVote]

    public init(
        matchId: Int64,
        totalMembers: Int,
        respondedCount: Int,
        pendingCount: Int,
        statusCounts: [AttendanceStatus: Int],
        votes: [AttendanceVote]
    ) {
        self.matchId = matchId
        self.totalMembers = totalMembers
        self.respondedCount = respondedCount
        self.pendingCount = pendingCount
        self.statusCounts = statusCounts
        self.votes = votes
    }

    /// 현재 로그인 사용자의 응답 추출 (없으면 nil).
    public func myVote(userId: Int64) -> AttendanceVote? {
        votes.first { $0.userId == userId }
    }

    /// 상태별 카운트 (없으면 0).
    public func count(of status: AttendanceStatus) -> Int {
        statusCounts[status] ?? 0
    }
}
