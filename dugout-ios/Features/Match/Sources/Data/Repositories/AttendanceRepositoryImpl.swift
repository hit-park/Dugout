//
//  AttendanceRepositoryImpl.swift
//  DugoutMatchFeature
//

import Foundation
import DugoutCoreNetwork

public struct AttendanceRepositoryImpl: AttendanceRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchSummary(matchId: Int64) async throws -> AttendanceSummary {
        let endpoint = APIEndpoint(path: "/api/v1/matches/\(matchId)/attendance")
        let dto: AttendanceSummaryDTO = try await client.request(endpoint)
        return dto.toDomain()
    }

    public func createVote(
        matchId: Int64,
        request: AttendanceVoteRequest
    ) async throws -> AttendanceVote {
        let body = AttendanceVoteRequestDTO(request)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/matches/\(matchId)/attendance",
            method: .post,
            body: body
        )
        let dto: AttendanceVoteDTO = try await client.request(endpoint)
        guard let vote = dto.toDomain() else {
            throw APIError.decoding("AttendanceVoteDTO → AttendanceVote 변환 실패")
        }
        return vote
    }

    public func updateVote(
        matchId: Int64,
        request: AttendanceVoteRequest
    ) async throws -> AttendanceVote {
        let body = AttendanceVoteRequestDTO(request)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/matches/\(matchId)/attendance",
            method: .put,
            body: body
        )
        let dto: AttendanceVoteDTO = try await client.request(endpoint)
        guard let vote = dto.toDomain() else {
            throw APIError.decoding("AttendanceVoteDTO → AttendanceVote 변환 실패")
        }
        return vote
    }
}
