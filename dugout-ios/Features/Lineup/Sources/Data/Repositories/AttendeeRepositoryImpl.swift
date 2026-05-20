//
//  AttendeeRepositoryImpl.swift
//  DugoutLineupFeature
//

import Foundation
import DugoutCoreNetwork

public struct AttendeeRepositoryImpl: AttendeeRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchAttendees(matchId: Int64, teamId: Int64) async throws -> [Attendee] {
        async let summaryFetch: AttendanceSummaryRefDTO = client.request(
            APIEndpoint(path: "/api/v1/matches/\(matchId)/attendance")
        )
        async let membersFetch: [TeamMemberRefDTO] = client.request(
            APIEndpoint(path: "/api/v1/teams/\(teamId)/members")
        )

        let (summary, members) = try await (summaryFetch, membersFetch)
        let activeMembersByUserId = Dictionary(
            uniqueKeysWithValues: members.filter { $0.isActive }.map { ($0.userId, $0) }
        )

        return summary.votes
            .filter { $0.status == "ATTEND" || $0.status == "LATE" }
            .compactMap { vote -> Attendee? in
                guard let member = activeMembersByUserId[vote.userId] else {
                    return nil
                }
                return Attendee(
                    userId: vote.userId,
                    nickname: vote.nickname,
                    jerseyNumber: member.jerseyNumber
                )
            }
    }
}
