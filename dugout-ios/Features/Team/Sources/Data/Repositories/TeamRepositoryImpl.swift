//
//  TeamRepositoryImpl.swift
//  DugoutTeamFeature
//

import Foundation
import DugoutCoreNetwork

public struct TeamRepositoryImpl: TeamRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchTeam(id: Int64) async throws -> Team {
        let endpoint = APIEndpoint(path: "/api/v1/teams/\(id)")
        let dto: TeamDTO = try await client.request(endpoint)
        return dto.toDomain()
    }
}
