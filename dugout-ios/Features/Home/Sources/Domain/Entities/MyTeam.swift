//
//  MyTeam.swift
//  DugoutHomeFeature
//

import Foundation

/// 홈 대시보드에서 표시하는 내 소속 팀 Entity.
/// HomeFeature 전용 — TeamFeature의 Team과는 다른 projection.
public struct MyTeam: Sendable, Equatable, Identifiable, Hashable {
    public let teamId: Int64
    public let teamName: String
    public let logoUrl: String?
    public let role: TeamRole
    public let joinedAt: Date

    public var id: Int64 { teamId }

    public init(
        teamId: Int64,
        teamName: String,
        logoUrl: String?,
        role: TeamRole,
        joinedAt: Date
    ) {
        self.teamId = teamId
        self.teamName = teamName
        self.logoUrl = logoUrl
        self.role = role
        self.joinedAt = joinedAt
    }
}

public enum TeamRole: String, Sendable, Hashable, CaseIterable {
    case captain = "CAPTAIN"
    case manager = "MANAGER"
    case accountant = "ACCOUNTANT"
    case member = "MEMBER"

    public var displayName: String {
        switch self {
        case .captain: "주장"
        case .manager: "매니저"
        case .accountant: "회계"
        case .member: "일반"
        }
    }
}
