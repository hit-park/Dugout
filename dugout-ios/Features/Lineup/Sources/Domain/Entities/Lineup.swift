//
//  Lineup.swift
//  DugoutLineupFeature
//
//  서버 저장 상태의 라인업.
//

import Foundation

public struct Lineup: Sendable, Equatable, Identifiable {
    public let id: Int64
    public let matchId: Int64
    public let teamId: Int64
    public let isAiGenerated: Bool
    public let isConfirmed: Bool
    public let confirmedAt: Date?
    public let entries: [LineupEntry]

    public init(
        id: Int64,
        matchId: Int64,
        teamId: Int64,
        isAiGenerated: Bool,
        isConfirmed: Bool,
        confirmedAt: Date?,
        entries: [LineupEntry]
    ) {
        self.id = id
        self.matchId = matchId
        self.teamId = teamId
        self.isAiGenerated = isAiGenerated
        self.isConfirmed = isConfirmed
        self.confirmedAt = confirmedAt
        self.entries = entries
    }
}
