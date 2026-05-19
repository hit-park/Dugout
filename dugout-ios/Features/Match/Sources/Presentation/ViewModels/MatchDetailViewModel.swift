//
//  MatchDetailViewModel.swift
//  DugoutMatchFeature
//

import Foundation
import Observation
import DugoutCoreNetwork
import DugoutDesignSystem

@MainActor
@Observable
public final class MatchDetailViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case loaded(MatchDetail)
        case failed(String)
    }

    public struct MatchDetail: Sendable, Equatable {
        public let match: Match
        public let attendance: AttendanceSummary
    }

    public private(set) var state: State = .idle
    public var presentVoteSheet: Bool = false
    public var toast: DGToastItem? = nil

    public let matchId: Int64
    public let currentUserId: Int64
    public let isManager: Bool

    private let matchRepository: any MatchRepository
    private let attendanceRepository: any AttendanceRepository

    public init(
        matchId: Int64,
        currentUserId: Int64,
        isManager: Bool,
        matchRepository: any MatchRepository = MatchRepositoryImpl(),
        attendanceRepository: any AttendanceRepository = AttendanceRepositoryImpl()
    ) {
        self.matchId = matchId
        self.currentUserId = currentUserId
        self.isManager = isManager
        self.matchRepository = matchRepository
        self.attendanceRepository = attendanceRepository
    }

    public func load() async {
        state = .loading
        async let matchFetch = matchRepository.fetchDetail(matchId: matchId)
        async let summaryFetch = attendanceRepository.fetchSummary(matchId: matchId)
        do {
            let (match, summary) = try await (matchFetch, summaryFetch)
            state = .loaded(MatchDetail(match: match, attendance: summary))
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("경기 정보를 불러오지 못했습니다")
        }
    }

    public func tapVote() {
        guard canVote else { return }
        presentVoteSheet = true
    }

    public func tapSummary() {
        toast = DGToastItem(
            message: "출석 요약은 다음 업데이트에 제공돼요",
            kind: .info
        )
    }

    public func onVoteCompleted(_ vote: AttendanceVote) async {
        presentVoteSheet = false
        await load()
    }

    // MARK: - Derived

    public var loadedDetail: MatchDetail? {
        if case .loaded(let detail) = state { return detail }
        return nil
    }

    public var myVote: AttendanceVote? {
        loadedDetail?.attendance.myVote(userId: currentUserId)
    }

    public var canVote: Bool {
        guard let detail = loadedDetail else { return false }
        if detail.match.status == .cancelled { return false }
        if let deadline = detail.match.voteDeadline, deadline < Date() { return false }
        return true
    }

    public var voteBlockedReason: String? {
        guard let detail = loadedDetail else { return nil }
        if detail.match.status == .cancelled { return "취소된 경기예요" }
        if let deadline = detail.match.voteDeadline, deadline < Date() {
            return "투표 마감 시간이 지났어요"
        }
        return nil
    }
}
