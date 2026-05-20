//
//  TeamRole.swift
//  DugoutMatchFeature
//
//  HomeFeature / TeamFeature 의 TeamRole 과 동일한 정의 —
//  Feature 독립 원칙에 따라 각자 소유 (TeamFeature TeamMember.swift 주석 참조).
//

import Foundation

public enum TeamRole: String, Sendable, Hashable, CaseIterable {
    case captain    = "CAPTAIN"
    case manager    = "MANAGER"
    case accountant = "ACCOUNTANT"
    case member     = "MEMBER"

    public var displayName: String {
        switch self {
        case .captain: "주장"
        case .manager: "매니저"
        case .accountant: "회계"
        case .member: "일반"
        }
    }
}
