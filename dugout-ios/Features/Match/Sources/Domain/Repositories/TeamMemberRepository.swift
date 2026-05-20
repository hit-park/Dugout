//
//  TeamMemberRepository.swift
//  DugoutMatchFeature
//

import Foundation

public protocol TeamMemberRepository: Sendable {
    /// 팀 멤버 전체 조회 (active + inactive 모두).
    /// 백엔드 GET /api/v1/teams/{teamId}/members 호출.
    func fetchMembers(teamId: Int64) async throws -> [TeamMemberRef]
}
