//
//  TeamMember.swift
//  DugoutTeamFeature
//

import Foundation

/// 팀 멤버 Entity (GET /teams/{id}/members 응답 항목).
public struct TeamMember: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64
    public let userId: Int64
    public let nickname: String
    public let profileImgUrl: String?
    public let role: TeamRole
    public let jerseyNumber: Int?
    public let positions: [String]
    public let isActive: Bool
    public let joinedAt: Date

    public init(
        id: Int64,
        userId: Int64,
        nickname: String,
        profileImgUrl: String?,
        role: TeamRole,
        jerseyNumber: Int?,
        positions: [String],
        isActive: Bool,
        joinedAt: Date
    ) {
        self.id = id
        self.userId = userId
        self.nickname = nickname
        self.profileImgUrl = profileImgUrl
        self.role = role
        self.jerseyNumber = jerseyNumber
        self.positions = positions
        self.isActive = isActive
        self.joinedAt = joinedAt
    }
}

/// TeamFeature 내부용 TeamRole.
/// HomeFeature의 TeamRole과 동일한 정의 — Feature 독립 원칙에 따라 각자 소유.
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

    public var canManageTeam: Bool {
        self == .captain || self == .manager
    }
}
