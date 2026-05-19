//
//  MatchRepositoryImpl.swift
//  DugoutMatchFeature
//

import Foundation
import DugoutCoreNetwork

public struct MatchRepositoryImpl: MatchRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchMatches(teamId: Int64, from: Date?, to: Date?) async throws -> [Match] {
        var queryItems: [URLQueryItem] = []
        if let from {
            queryItems.append(.init(name: "from", value: LocalDateFormatter.shared.string(from: from)))
        }
        if let to {
            queryItems.append(.init(name: "to", value: LocalDateFormatter.shared.string(from: to)))
        }
        let endpoint = APIEndpoint(
            path: "/api/v1/teams/\(teamId)/matches",
            queryItems: queryItems
        )
        let dtos: [MatchDTO] = try await client.request(endpoint)
        return dtos.compactMap { $0.toDomain() }
    }

    public func createMatch(teamId: Int64, request: CreateMatchRequest) async throws -> Match {
        let body = CreateMatchRequestDTO(
            matchDate: LocalDateFormatter.shared.string(from: request.matchDate),
            matchTime: request.matchTime.wireString,
            gatherTime: request.gatherTime?.wireString,
            opponentName: request.opponentName,
            opponentTeamId: nil,
            groundId: nil,
            groundName: request.groundName,
            voteDeadline: request.voteDeadline.map { LocalDateTimeFormatter.shared.string(from: $0) },
            memo: request.memo
        )
        let endpoint = APIEndpoint.json(
            path: "/api/v1/teams/\(teamId)/matches",
            method: .post,
            body: body
        )
        let dto: MatchDTO = try await client.request(endpoint)
        guard let match = dto.toDomain() else {
            throw APIError.decoding("MatchDTO → Match 변환 실패")
        }
        return match
    }
}
