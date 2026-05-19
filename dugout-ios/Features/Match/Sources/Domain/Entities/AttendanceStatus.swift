//
//  AttendanceStatus.swift
//  DugoutMatchFeature
//
//  백엔드 AttendanceStatus enum (ATTEND/ABSENT/MAYBE/LATE/EARLY_LEAVE) 대응.
//

import Foundation

public enum AttendanceStatus: String, Sendable, Codable, CaseIterable, Hashable {
    case attend     = "ATTEND"
    case absent     = "ABSENT"
    case maybe      = "MAYBE"
    case late       = "LATE"
    case earlyLeave = "EARLY_LEAVE"

    public var koreanLabel: String {
        switch self {
        case .attend: "참가"
        case .absent: "불참"
        case .maybe: "미정"
        case .late: "늦참"
        case .earlyLeave: "조퇴"
        }
    }

    public var emoji: String {
        switch self {
        case .attend: "✅"
        case .absent: "❌"
        case .maybe: "❓"
        case .late: "⏰"
        case .earlyLeave: "🚪"
        }
    }
}
