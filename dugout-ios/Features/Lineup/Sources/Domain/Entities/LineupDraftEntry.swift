//
//  LineupDraftEntry.swift
//  DugoutLineupFeature
//
//  편집 중 라인업 엔트리. 서버 저장 전이라 id 가 UUID, position/battingOrder/isBench 가 var.
//

import Foundation

public struct LineupDraftEntry: Sendable, Equatable, Identifiable, Hashable {
    public let id: UUID
    public let userId: Int64
    public let nickname: String
    public let jerseyNumber: Int?
    public var position: BaseballPosition
    public var battingOrder: Int?
    public var isBench: Bool

    public init(
        id: UUID = UUID(),
        userId: Int64,
        nickname: String,
        jerseyNumber: Int?,
        position: BaseballPosition,
        battingOrder: Int?,
        isBench: Bool
    ) {
        self.id = id
        self.userId = userId
        self.nickname = nickname
        self.jerseyNumber = jerseyNumber
        self.position = position
        self.battingOrder = battingOrder
        self.isBench = isBench
    }
}
