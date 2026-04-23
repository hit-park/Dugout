//
//  HomeRepositoryImpl.swift
//  DugoutHomeFeature
//

import Foundation
import DugoutCoreNetwork

public struct HomeRepositoryImpl: HomeRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchMyTeams() async throws -> [MyTeam] {
        let endpoint = APIEndpoint(path: "/api/v1/users/me/teams")
        let dtos: [MyTeamDTO] = try await client.request(endpoint)
        return dtos.map { $0.toDomain() }
    }
}
