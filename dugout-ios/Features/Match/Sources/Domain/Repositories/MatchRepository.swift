//
//  MatchRepository.swift
//  DugoutMatchFeature
//

import Foundation

public protocol MatchRepository: Sendable {
    /// 팀의 경기 목록 (월 단위 조회 지원).
    /// - Parameters:
    ///   - teamId: 팀 ID
    ///   - from: 시작일 (포함). nil이면 전체.
    ///   - to: 종료일 (포함). nil이면 전체.
    func fetchMatches(teamId: Int64, from: Date?, to: Date?) async throws -> [Match]

    /// 경기 등록 (주장/매니저만).
    func createMatch(teamId: Int64, request: CreateMatchRequest) async throws -> Match
}

public struct CreateMatchRequest: Sendable {
    public let matchDate: Date
    public let matchTime: TimeOfDay
    public let gatherTime: TimeOfDay?
    public let opponentName: String?
    public let groundName: String?
    public let voteDeadline: Date?
    public let memo: String?

    public init(
        matchDate: Date,
        matchTime: TimeOfDay,
        gatherTime: TimeOfDay? = nil,
        opponentName: String? = nil,
        groundName: String? = nil,
        voteDeadline: Date? = nil,
        memo: String? = nil
    ) {
        self.matchDate = matchDate
        self.matchTime = matchTime
        self.gatherTime = gatherTime
        self.opponentName = opponentName
        self.groundName = groundName
        self.voteDeadline = voteDeadline
        self.memo = memo
    }
}
