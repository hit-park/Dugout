//
//  Attendee.swift
//  DugoutLineupFeature
//
//  AttendanceSummary 의 votes(status in ATTEND/LATE) + TeamMember(jersey_number) 의 조인 결과.
//

import Foundation

public struct Attendee: Sendable, Equatable, Identifiable, Hashable {
    public let userId: Int64
    public let nickname: String
    public let jerseyNumber: Int?

    public var id: Int64 { userId }

    public init(userId: Int64, nickname: String, jerseyNumber: Int?) {
        self.userId = userId
        self.nickname = nickname
        self.jerseyNumber = jerseyNumber
    }
}
