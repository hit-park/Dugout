//
//  MyTeamDTO.swift
//  DugoutHomeFeature
//

import Foundation

struct MyTeamDTO: Decodable, Sendable {
    let teamId: Int64
    let teamName: String
    let logoUrl: String?
    let role: String
    let joinedAt: Date

    enum CodingKeys: String, CodingKey {
        case teamId = "team_id"
        case teamName = "team_name"
        case logoUrl = "logo_url"
        case role
        case joinedAt = "joined_at"
    }

    func toDomain() -> MyTeam {
        MyTeam(
            teamId: teamId,
            teamName: teamName,
            logoUrl: logoUrl,
            role: TeamRole(rawValue: role) ?? .member,
            joinedAt: joinedAt
        )
    }
}
