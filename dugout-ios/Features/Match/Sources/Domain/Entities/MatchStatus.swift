//
//  MatchStatus.swift
//  DugoutMatchFeature
//
//  백엔드 MatchStatus enum (SCHEDULED/CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED) 대응.
//

import Foundation

public enum MatchStatus: String, Sendable, Codable, CaseIterable {
    case scheduled = "SCHEDULED"
    case confirmed = "CONFIRMED"
    case inProgress = "IN_PROGRESS"
    case completed = "COMPLETED"
    case cancelled = "CANCELLED"

    public var koreanLabel: String {
        switch self {
        case .scheduled: "예정"
        case .confirmed: "확정"
        case .inProgress: "진행 중"
        case .completed: "종료"
        case .cancelled: "취소"
        }
    }
}
