//
//  LineupRepositoryImpl.swift
//  DugoutLineupFeature
//

import Foundation
import DugoutCoreNetwork

public struct LineupRepositoryImpl: LineupRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchLineup(matchId: Int64) async throws -> Lineup {
        let endpoint = APIEndpoint(path: "/api/v1/matches/\(matchId)/lineup")
        let dto: LineupDTO = try await client.request(endpoint)
        guard let lineup = dto.toDomain() else {
            throw APIError.decoding("LineupDTO → Lineup 변환 실패")
        }
        return lineup
    }

    public func recommend(matchId: Int64, attendees: [Attendee]) async throws -> LineupRecommendation {
        let endpoint = APIEndpoint(
            path: "/api/v1/matches/\(matchId)/lineup/recommend",
            method: .post
        )
        let dto: LineupRecommendationDTO = try await client.request(endpoint)

        let attendeesByUserId = Dictionary(uniqueKeysWithValues: attendees.map { ($0.userId, $0) })
        let draftEntries: [LineupDraftEntry] = dto.entries.compactMap { payload in
            guard let pos = BaseballPosition(rawValue: payload.position) else { return nil }
            let att = attendeesByUserId[payload.userId]
            return LineupDraftEntry(
                userId: payload.userId,
                nickname: att?.nickname ?? "팀원",
                jerseyNumber: att?.jerseyNumber,
                position: pos,
                battingOrder: payload.battingOrder,
                isBench: payload.isBench
            )
        }

        return LineupRecommendation(
            matchId: dto.matchId,
            source: dto.source,
            isAiGenerated: dto.isAiGenerated,
            draft: LineupDraft(entries: draftEntries)
        )
    }

    public func saveLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup {
        let body = SaveLineupRequestDTO(request)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/matches/\(matchId)/lineup",
            method: .post,
            body: body
        )
        let dto: LineupDTO = try await client.request(endpoint)
        guard let lineup = dto.toDomain() else {
            throw APIError.decoding("LineupDTO → Lineup 변환 실패")
        }
        return lineup
    }

    public func updateLineup(matchId: Int64, request: SaveLineupRequest) async throws -> Lineup {
        let body = SaveLineupRequestDTO(request)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/matches/\(matchId)/lineup",
            method: .put,
            body: body
        )
        let dto: LineupDTO = try await client.request(endpoint)
        guard let lineup = dto.toDomain() else {
            throw APIError.decoding("LineupDTO → Lineup 변환 실패")
        }
        return lineup
    }
}
