import Foundation

// MARK: - Match DTO (GET /api/v1/teams/{teamId}/matches)

struct MatchListItemDTO: Decodable, Sendable {
    let id: Int64
    let opponentName: String?
    let groundName: String?
    let matchDate: String   // "yyyy-MM-dd"
    let matchTime: String   // "HH:mm:ss"
    let status: String?

    enum CodingKeys: String, CodingKey {
        case id
        case opponentName = "opponent_name"
        case groundName   = "ground_name"
        case matchDate    = "match_date"
        case matchTime    = "match_time"
        case status
    }

    func toNextMatch() -> NextMatch? {
        guard let scheduledAt = combine(date: matchDate, time: matchTime) else { return nil }
        return NextMatch(
            id: id,
            opponentName: opponentName,
            scheduledAt: scheduledAt,
            groundName: groundName,
            address: nil   // BACKEND-GAP: Match 엔티티에 주소 없음. Ground 도메인 연동 시 보강
        )
    }

    private static let scheduledAtFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "Asia/Seoul")
        f.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return f
    }()

    private func combine(date: String, time: String) -> Date? {
        Self.scheduledAtFormatter.date(from: "\(date) \(time)")
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
