//
//  LineupRepository.swift
//  DugoutLineupFeature
//

import Foundation

public protocol LineupRepository: Sendable {
    /// 라인업 조회. 404/LINEUP_NOT_FOUND 는 호출 측에서 .empty 로 매핑.
    func fetchLineup(matchId: Int64) async throws -> Lineup

    /// AI 추천 호출. 백엔드는 matchId 만 받음 (출석자는 백엔드가 자체 조회).
    /// `attendees` 는 응답 entries 에 nickname/jerseyNumber 가 없으므로
    /// 클라이언트가 LineupDraft 로 변환할 때 조인용으로 전달 (Repository 가 내부 enrich).
    func recommend(matchId: Int64, attendees: [Attendee]) async throws -> LineupRecommendation

    func saveLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup
    func updateLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup
    func confirmLineup(matchId: Int64) async throws -> Lineup
}

public struct SaveLineupRequest: Sendable, Equatable {
    public let entries: [LineupDraftEntry]

    public init(entries: [LineupDraftEntry]) {
        self.entries = entries
    }
}
