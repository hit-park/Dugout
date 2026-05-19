import Foundation

// MARK: - Match DTO (GET /api/v1/teams/{teamId}/matches)

struct MatchListItemDTO: Decodable, Sendable {
    let matchId: Int64
    let scheduledAt: Date
    let opponentName: String?
    let groundName: String?
    let address: String?
    let status: String?

    enum CodingKeys: String, CodingKey {
        case matchId = "match_id"
        case scheduledAt = "scheduled_at"
        case opponentName = "opponent_name"
        case groundName = "ground_name"
        case address
        case status
    }

    func toNextMatch() -> NextMatch {
        NextMatch(
            id: matchId,
            opponentName: opponentName,
            scheduledAt: scheduledAt,
            groundName: groundName,
            address: address
        )
    }
}

// MARK: - AI Attendance Prediction DTO (dugout-ai)

struct AttendancePredictionDTO: Decodable, Sendable {
    let matchId: Int64
    let minAttendance: Int
    let maxAttendance: Int
    let totalMembers: Int

    enum CodingKeys: String, CodingKey {
        case matchId = "match_id"
        case minAttendance = "min_attendance"
        case maxAttendance = "max_attendance"
        case totalMembers = "total_members"
    }

    func toDomain() -> AttendancePrediction {
        AttendancePrediction(
            matchId: matchId,
            minCount: minAttendance,
            maxCount: maxAttendance,
            totalMembers: totalMembers
        )
    }
}
