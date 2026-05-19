//
//  MatchDTO.swift
//  DugoutMatchFeature
//
//  백엔드 MatchResponse 매핑.
//  Spring Boot Jackson SNAKE_CASE 글로벌 설정이므로 CodingKeys로 명시 매핑.
//

import Foundation

struct MatchDTO: Decodable, Sendable {
    let id: Int64
    let teamId: Int64
    let opponentName: String?
    let opponentTeamId: Int64?
    let groundId: Int64?
    let groundName: String?
    let matchDate: String      // "yyyy-MM-dd" (LocalDate)
    let gatherTime: String?    // "HH:mm:ss" (LocalTime)
    let matchTime: String      // "HH:mm:ss"
    let voteDeadline: Date?    // LocalDateTime ISO8601
    let status: String
    let resultHome: Int?
    let resultAway: Int?
    let memo: String?
    let createdAt: Date

    enum CodingKeys: String, CodingKey {
        case id
        case teamId         = "team_id"
        case opponentName   = "opponent_name"
        case opponentTeamId = "opponent_team_id"
        case groundId       = "ground_id"
        case groundName     = "ground_name"
        case matchDate      = "match_date"
        case gatherTime     = "gather_time"
        case matchTime      = "match_time"
        case voteDeadline   = "vote_deadline"
        case status
        case resultHome     = "result_home"
        case resultAway     = "result_away"
        case memo
        case createdAt      = "created_at"
    }

    func toDomain() -> Match? {
        guard let date = LocalDateFormatter.shared.date(from: matchDate),
              let mTime = TimeOfDay(string: matchTime) else {
            return nil
        }
        let gTime = gatherTime.flatMap(TimeOfDay.init(string:))
        let parsedStatus = MatchStatus(rawValue: status) ?? .scheduled
        return Match(
            id: id,
            teamId: teamId,
            opponentName: opponentName,
            opponentTeamId: opponentTeamId,
            groundId: groundId,
            groundName: groundName,
            matchDate: date,
            gatherTime: gTime,
            matchTime: mTime,
            voteDeadline: voteDeadline,
            status: parsedStatus,
            memo: memo,
            createdAt: createdAt
        )
    }
}

enum LocalDateFormatter {
    static let shared: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "Asia/Seoul")
        f.dateFormat = "yyyy-MM-dd"
        return f
    }()
}
