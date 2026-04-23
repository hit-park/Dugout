//
//  HomeRepository.swift
//  DugoutHomeFeature
//

import Foundation

public protocol HomeRepository: Sendable {
    /// 내가 속한 팀 목록 조회.
    func fetchMyTeams() async throws -> [MyTeam]
}
