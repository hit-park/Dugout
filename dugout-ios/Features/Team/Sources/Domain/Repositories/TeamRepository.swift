//
//  TeamRepository.swift
//  DugoutTeamFeature
//

import Foundation

public protocol TeamRepository: Sendable {
    func fetchTeam(id: Int64) async throws -> Team
}
