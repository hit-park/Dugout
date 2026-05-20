//
//  TeamMemberRepositoryImpl.swift
//  DugoutMatchFeature
//

import Foundation
import DugoutCoreNetwork

public struct TeamMemberRepositoryImpl: TeamMemberRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchMembers(teamId: Int64) async throws -> [TeamMemberRef] {
        let endpoint = APIEndpoint(path: "/api/v1/teams/\(teamId)/members")
        let dtos: [TeamMemberRefDTO] = try await client.request(endpoint)
        return dtos.compactMap { $0.toDomain() }
    }
}
