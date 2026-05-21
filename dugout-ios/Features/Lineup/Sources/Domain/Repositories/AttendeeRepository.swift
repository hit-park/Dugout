//
//  AttendeeRepository.swift
//  DugoutLineupFeature
//

import Foundation

public protocol AttendeeRepository: Sendable {
    /// 경기 출석자(ATTEND + LATE) + 등번호를 조인하여 반환.
    /// 백엔드 호출: GET /matches/{matchId}/attendance + GET /teams/{teamId}/members.
    func fetchAttendees(matchId: Int64, teamId: Int64) async throws -> [Attendee]
}
