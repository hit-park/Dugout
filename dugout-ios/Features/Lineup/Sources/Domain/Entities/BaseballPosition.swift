//
//  BaseballPosition.swift
//  DugoutLineupFeature
//
//  AuthFeature 의 BaseballPosition 과 동일한 정의 —
//  Feature 독립 원칙에 따라 각자 소유 (TeamRole 패턴과 동일).
//

import Foundation

public enum BaseballPosition: String, Sendable, Hashable, CaseIterable, Codable {
    case pitcher          = "P"
    case catcher          = "C"
    case firstBase        = "1B"
    case secondBase       = "2B"
    case thirdBase        = "3B"
    case shortStop        = "SS"
    case leftField        = "LF"
    case centerField      = "CF"
    case rightField       = "RF"
    case designatedHitter = "DH"

    public var displayName: String {
        switch self {
        case .pitcher:           "투수"
        case .catcher:           "포수"
        case .firstBase:         "1루수"
        case .secondBase:        "2루수"
        case .thirdBase:         "3루수"
        case .shortStop:         "유격수"
        case .leftField:         "좌익수"
        case .centerField:       "중견수"
        case .rightField:        "우익수"
        case .designatedHitter:  "지명타자"
        }
    }

    public var shortName: String { rawValue }
    public var isField: Bool { self != .designatedHitter }

    public static var fieldPositions: [BaseballPosition] {
        [.pitcher, .catcher, .firstBase, .secondBase, .thirdBase,
         .shortStop, .leftField, .centerField, .rightField]
    }
}
