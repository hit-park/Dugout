//
//  NotificationType.swift
//  Dugout
//

import Foundation

enum NotificationType: String, Sendable {
    case matchCreated = "MATCH_CREATED"
    case lineupConfirmed = "LINEUP_CONFIRMED"
    case attendanceReminder = "ATTENDANCE_REMINDER"
    case attendanceChanged = "ATTENDANCE_CHANGED"
}

struct PushRoute: Sendable, Equatable {
    let type: NotificationType
    let matchId: Int64?
}

extension PushRoute {
    /// APNs userInfo(`[AnyHashable: Any]`)에서 deeplink 라우트 추출. 실패 시 nil.
    init?(userInfo: [AnyHashable: Any]) {
        guard let rawType = userInfo["type"] as? String,
              let type = NotificationType(rawValue: rawType) else {
            return nil
        }
        let matchId = (userInfo["matchId"] as? String).flatMap { Int64($0) }
        self.init(type: type, matchId: matchId)
    }
}
