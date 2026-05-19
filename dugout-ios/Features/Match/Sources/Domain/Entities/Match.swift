//
//  Match.swift
//  DugoutMatchFeature
//
//  도메인 Match 엔티티. 백엔드 MatchResponse(snake_case) → 이 struct로 변환.
//

import Foundation

public struct Match: Sendable, Equatable, Identifiable {
    public let id: Int64
    public let teamId: Int64
    public let opponentName: String?
    public let opponentTeamId: Int64?
    public let groundId: Int64?
    public let groundName: String?
    public let matchDate: Date          // 날짜만 (시:분:초 = 00:00:00, Asia/Seoul)
    public let gatherTime: TimeOfDay?
    public let matchTime: TimeOfDay
    public let voteDeadline: Date?
    public let status: MatchStatus
    public let memo: String?
    public let createdAt: Date

    public init(
        id: Int64,
        teamId: Int64,
        opponentName: String?,
        opponentTeamId: Int64?,
        groundId: Int64?,
        groundName: String?,
        matchDate: Date,
        gatherTime: TimeOfDay?,
        matchTime: TimeOfDay,
        voteDeadline: Date?,
        status: MatchStatus,
        memo: String?,
        createdAt: Date
    ) {
        self.id = id
        self.teamId = teamId
        self.opponentName = opponentName
        self.opponentTeamId = opponentTeamId
        self.groundId = groundId
        self.groundName = groundName
        self.matchDate = matchDate
        self.gatherTime = gatherTime
        self.matchTime = matchTime
        self.voteDeadline = voteDeadline
        self.status = status
        self.memo = memo
        self.createdAt = createdAt
    }

    /// matchDate + matchTime을 합친 절대 시각 (Asia/Seoul 기준).
    public var scheduledAt: Date {
        let calendar = Calendar.koreaCalendar
        var components = calendar.dateComponents([.year, .month, .day], from: matchDate)
        components.hour = matchTime.hour
        components.minute = matchTime.minute
        components.second = matchTime.second
        return calendar.date(from: components) ?? matchDate
    }

    public var dDayLabel: String {
        let calendar = Calendar.koreaCalendar
        let today = calendar.startOfDay(for: Date())
        let day = calendar.startOfDay(for: matchDate)
        let delta = calendar.dateComponents([.day], from: today, to: day).day ?? 0
        if delta == 0 { return "D-DAY" }
        if delta > 0  { return "D-\(delta)" }
        return "종료"
    }
}

/// LocalTime(HH:mm:ss) 대응 값 타입.
public struct TimeOfDay: Sendable, Equatable, Codable {
    public let hour: Int
    public let minute: Int
    public let second: Int

    public init(hour: Int, minute: Int, second: Int = 0) {
        self.hour = hour
        self.minute = minute
        self.second = second
    }

    /// "HH:mm:ss" 또는 "HH:mm" 파싱.
    public init?(string: String) {
        let parts = string.split(separator: ":").compactMap { Int($0) }
        guard parts.count >= 2, (0...23).contains(parts[0]), (0...59).contains(parts[1]) else {
            return nil
        }
        self.hour = parts[0]
        self.minute = parts[1]
        self.second = parts.count >= 3 ? parts[2] : 0
    }

    /// 백엔드 송신 포맷 "HH:mm:ss".
    public var wireString: String {
        String(format: "%02d:%02d:%02d", hour, minute, second)
    }

    /// 표시 포맷 "오후 7:30".
    public var displayString: String {
        var components = DateComponents()
        components.hour = hour
        components.minute = minute
        let date = Calendar.koreaCalendar.date(from: components) ?? Date()
        return TimeOfDay.displayFormatter.string(from: date)
    }

    private static let displayFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "a h:mm"
        return f
    }()
}

extension Calendar {
    /// Asia/Seoul + ko_KR로 구성된 캘린더. value type + Sendable이라 안전하게 공유.
    static let koreaCalendar: Calendar = {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "Asia/Seoul") ?? .current
        calendar.locale = Locale(identifier: "ko_KR")
        return calendar
    }()
}
