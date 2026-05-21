//
//  LineupRecommendation.swift
//  DugoutLineupFeature
//

import Foundation

public struct LineupRecommendation: Sendable, Equatable {
    public let matchId: Int64
    public let source: String
    public let isAiGenerated: Bool
    public let draft: LineupDraft

    public init(matchId: Int64, source: String, isAiGenerated: Bool, draft: LineupDraft) {
        self.matchId = matchId
        self.source = source
        self.isAiGenerated = isAiGenerated
        self.draft = draft
    }
}
