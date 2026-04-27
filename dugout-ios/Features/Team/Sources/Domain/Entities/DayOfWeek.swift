//
//  DayOfWeek.swift
//  DugoutTeamFeature
//

import Foundation

/// 활동 요일 코드. API와 통신할 때는 `rawValue` 그대로 사용.
public enum DayOfWeek: String, Sendable, Hashable, CaseIterable {
    case mon = "MON"
    case tue = "TUE"
    case wed = "WED"
    case thu = "THU"
    case fri = "FRI"
    case sat = "SAT"
    case sun = "SUN"

    public var displayName: String {
        switch self {
        case .mon: "월"
        case .tue: "화"
        case .wed: "수"
        case .thu: "목"
        case .fri: "금"
        case .sat: "토"
        case .sun: "일"
        }
    }
}
