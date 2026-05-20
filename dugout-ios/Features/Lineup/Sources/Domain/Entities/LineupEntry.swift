//
//  LineupEntry.swift
//  DugoutLineupFeature
//

import Foundation

public struct LineupEntry: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64
    public let userId: Int64
    public let nickname: String
    public let position: BaseballPosition
    public let battingOrder: Int?
    public let isBench: Bool

    public init(
        id: Int64,
        userId: Int64,
        nickname: String,
        position: BaseballPosition,
        battingOrder: Int?,
        isBench: Bool
    ) {
        self.id = id
        self.userId = userId
        self.nickname = nickname
        self.position = position
        self.battingOrder = battingOrder
        self.isBench = isBench
    }
}
