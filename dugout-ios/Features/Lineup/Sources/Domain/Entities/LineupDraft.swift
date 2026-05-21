//
//  LineupDraft.swift
//  DugoutLineupFeature
//

import Foundation

public struct LineupDraft: Sendable, Equatable {
    public var entries: [LineupDraftEntry]

    public init(entries: [LineupDraftEntry]) {
        self.entries = entries
    }
}
