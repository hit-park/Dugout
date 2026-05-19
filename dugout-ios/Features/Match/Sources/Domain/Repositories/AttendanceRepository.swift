//
//  AttendanceRepository.swift
//  DugoutMatchFeature
//

import Foundation

public protocol AttendanceRepository: Sendable {
    /// 경기의 출석 요약 + 응답자 리스트 조회.
    func fetchSummary(matchId: Int64) async throws -> AttendanceSummary

    /// 최초 응답 등록 (POST). 이미 응답이 있으면 ALREADY_VOTED(409) 발생.
    func createVote(matchId: Int64, request: AttendanceVoteRequest) async throws -> AttendanceVote

    /// 응답 변경 (PUT). 응답이 없으면 VOTE_NOT_FOUND(404) 발생.
    func updateVote(matchId: Int64, request: AttendanceVoteRequest) async throws -> AttendanceVote
}

public struct AttendanceVoteRequest: Sendable, Equatable {
    public let status: AttendanceStatus
    public let reason: String?

    public init(status: AttendanceStatus, reason: String? = nil) {
        self.status = status
        self.reason = reason
    }
}
