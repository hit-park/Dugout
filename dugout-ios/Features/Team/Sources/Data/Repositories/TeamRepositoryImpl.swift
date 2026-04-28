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

    public func createTeam(_ request: CreateTeamRequest) async throws -> Team {
        let body = CreateTeamRequestDTO(
            name: request.name,
            region: request.region,
            division: request.division,
            activityDays: request.activityDays,
            activityTime: request.activityTime,
            lineupMode: request.lineupMode.rawValue
        )
        let endpoint = APIEndpoint.json(
            path: "/api/v1/teams",
            method: .post,
            body: body
        )
        let dto: TeamDTO = try await client.request(endpoint)
        return dto.toDomain()
    }

    public func fetchTeam(id: Int64) async throws -> Team {
        let endpoint = APIEndpoint(path: "/api/v1/teams/\(id)")
        let dto: TeamDTO = try await client.request(endpoint)
        return dto.toDomain()
    }

    public func fetchMembers(teamId: Int64) async throws -> [TeamMember] {
        let endpoint = APIEndpoint(path: "/api/v1/teams/\(teamId)/members")
        let dtos: [TeamMemberDTO] = try await client.request(endpoint)
        return dtos.map { $0.toDomain() }
    }

    public func generateInviteCode(teamId: Int64) async throws -> String {
        let endpoint = APIEndpoint(
            path: "/api/v1/teams/\(teamId)/invite",
            method: .post
        )
        let dto: InviteCodeResponseDTO = try await client.request(endpoint)
        return dto.inviteCode
    }

    public func joinTeam(inviteCode: String) async throws -> TeamMember {
        let body = JoinTeamRequestDTO(inviteCode: inviteCode)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/teams/join",
            method: .post,
            body: body
        )
        let dto: TeamMemberDTO = try await client.request(endpoint)
        return dto.toDomain()
    }

    public func updateTeam(id: Int64, request: UpdateTeamRequest) async throws -> Team {
        let body = UpdateTeamRequestDTO(
            name: request.name,
            region: request.region,
            division: request.division,
            activityDays: request.activityDays,
            activityTime: request.activityTime,
            lineupMode: request.lineupMode?.rawValue
        )
        let endpoint = APIEndpoint.json(
            path: "/api/v1/teams/\(id)",
            method: .put,
            body: body
        )
        let dto: TeamDTO = try await client.request(endpoint)
        return dto.toDomain()
    }

    public func updateMember(teamId: Int64, memberId: Int64, role: TeamRole) async throws -> TeamMember {
        let body = UpdateMemberRequestDTO(role: role.rawValue)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/teams/\(teamId)/members/\(memberId)",
            method: .put,
            body: body
        )
        let dto: TeamMemberDTO = try await client.request(endpoint)
        return dto.toDomain()
    }

    public func removeMember(teamId: Int64, memberId: Int64) async throws {
        let endpoint = APIEndpoint(
            path: "/api/v1/teams/\(teamId)/members/\(memberId)",
            method: .delete
        )
        try await client.requestVoid(endpoint)
    }
}
