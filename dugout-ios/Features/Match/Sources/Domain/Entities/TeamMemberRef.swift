//
//  TeamMemberRef.swift
//  DugoutMatchFeature
//
//  팀 멤버 식별·표시용 minimal 도메인 타입. MATCH-5 미응답자 노출에 사용.
//  백엔드 TeamMemberResponse 의 부분 집합 (positions/joinedAt 미포함 — 본 PR 범위 외).
//

import Foundation

public struct TeamMemberRef: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64            // TeamMember.id
    public let userId: Int64        // 응답자 매칭 키
    public let nickname: String
    public let profileImgUrl: String?
    public let jerseyNumber: Int?
    public let role: TeamRole
    public let isActive: Bool

    public init(
        id: Int64,
        userId: Int64,
        nickname: String,
        profileImgUrl: String?,
        jerseyNumber: Int?,
        role: TeamRole,
        isActive: Bool
    ) {
        self.id = id
        self.userId = userId
        self.nickname = nickname
        self.profileImgUrl = profileImgUrl
        self.jerseyNumber = jerseyNumber
        self.role = role
        self.isActive = isActive
    }
}
