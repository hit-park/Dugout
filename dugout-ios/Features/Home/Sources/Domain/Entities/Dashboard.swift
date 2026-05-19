import Foundation

public struct Dashboard: Sendable, Equatable {
    public let teamId: Int64
    public let nextMatch: NextMatch?
    public let attendancePrediction: AttendancePrediction?
    public let notices: [Notice]

    public init(
        teamId: Int64,
        nextMatch: NextMatch?,
        attendancePrediction: AttendancePrediction?,
        notices: [Notice]
    ) {
        self.teamId = teamId
        self.nextMatch = nextMatch
        self.attendancePrediction = attendancePrediction
        self.notices = notices
    }
}

public struct NextMatch: Sendable, Equatable, Identifiable {
    public let id: Int64
    public let opponentName: String?
    public let scheduledAt: Date
    public let groundName: String?
    public let address: String?

    public init(
        id: Int64,
        opponentName: String?,
        scheduledAt: Date,
        groundName: String?,
        address: String?
    ) {
        self.id = id
        self.opponentName = opponentName
        self.scheduledAt = scheduledAt
        self.groundName = groundName
        self.address = address
    }

    /// D-N 값. 오늘이면 0, 과거면 음수
    public var dDayValue: Int {
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: Date())
        let matchDay = calendar.startOfDay(for: scheduledAt)
        return calendar.dateComponents([.day], from: today, to: matchDay).day ?? 0
    }

    public var dDayLabel: String {
        let d = dDayValue
        if d == 0 { return "D-DAY" }
        if d > 0  { return "D-\(d)" }
        return "종료"
    }
}

public struct AttendancePrediction: Sendable, Equatable {
    public let matchId: Int64
    public let minCount: Int
    public let maxCount: Int
    public let totalMembers: Int

    public init(matchId: Int64, minCount: Int, maxCount: Int, totalMembers: Int) {
        self.matchId = matchId
        self.minCount = minCount
        self.maxCount = maxCount
        self.totalMembers = totalMembers
    }

    public var headline: String { "예상 참가 \(minCount)~\(maxCount)명" }
}

public struct Notice: Sendable, Equatable, Identifiable {
    public let id: Int64
    public let title: String
    public let createdAt: Date

    public init(id: Int64, title: String, createdAt: Date) {
        self.id = id
        self.title = title
        self.createdAt = createdAt
    }
}
