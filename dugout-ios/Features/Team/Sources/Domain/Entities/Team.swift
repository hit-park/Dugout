//
//  Team.swift
//  DugoutTeamFeature
//

import Foundation

/// 팀 상세 Entity.
public struct Team: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64
    public let name: String
    public let logoUrl: String?
    public let region: String
    public let division: Int
    public let homeGroundId: Int64?
    public let activityDays: [String]
    public let activityTime: String?
    public let inviteCode: String?
    public let lineupMode: LineupMode
    public let memberCount: Int
    public let createdAt: Date

    public init(
        id: Int64,
        name: String,
        logoUrl: String?,
        region: String,
        division: Int,
        homeGroundId: Int64?,
        activityDays: [String],
        activityTime: String?,
        inviteCode: String?,
        lineupMode: LineupMode,
        memberCount: Int,
        createdAt: Date
    ) {
        self.id = id
        self.name = name
        self.logoUrl = logoUrl
        self.region = region
        self.division = division
        self.homeGroundId = homeGroundId
        self.activityDays = activityDays
        self.activityTime = activityTime
        self.inviteCode = inviteCode
        self.lineupMode = lineupMode
        self.memberCount = memberCount
        self.createdAt = createdAt
    }
}

public enum LineupMode: String, Sendable, Hashable {
    case balanced = "BALANCED"
    case competitive = "COMPETITIVE"
}
