//
//  MatchAttendanceSummaryViewModel.swift
//  DugoutMatchFeature
//

import Foundation
import Observation
import DugoutCoreNetwork
import DugoutDesignSystem

@MainActor
@Observable
public final class MatchAttendanceSummaryViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case loaded(Snapshot)
        case failed(String)
    }

    public struct Snapshot: Sendable, Equatable {
        public let summary: AttendanceSummary
        public let activeMembers: [TeamMemberRef]
    }

    public enum Filter: Sendable, Equatable, Hashable, CaseIterable {
        case all, attend, absent, pending

        public var displayName: String {
            switch self {
            case .all:     "전체"
            case .attend:  "참가"
            case .absent:  "불참"
            case .pending: "미응답"
            }
        }
    }

    public private(set) var state: State = .idle
    public var filter: Filter = .all
    public var toast: DGToastItem? = nil

    public let matchId: Int64
    public let teamId: Int64
    private let attendanceRepository: any AttendanceRepository
    private let teamMemberRepository: any TeamMemberRepository

    public init(
        matchId: Int64,
        teamId: Int64,
        attendanceRepository: any AttendanceRepository = AttendanceRepositoryImpl(),
        teamMemberRepository: any TeamMemberRepository = TeamMemberRepositoryImpl()
    ) {
        self.matchId = matchId
        self.teamId = teamId
        self.attendanceRepository = attendanceRepository
        self.teamMemberRepository = teamMemberRepository
    }

    public func load() async {
        state = .loading
        async let summaryFetch = attendanceRepository.fetchSummary(matchId: matchId)
        async let membersFetch = teamMemberRepository.fetchMembers(teamId: teamId)
        do {
            let (summary, members) = try await (summaryFetch, membersFetch)
            let active = members.filter { $0.isActive }
            state = .loaded(Snapshot(summary: summary, activeMembers: active))
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("출석 현황을 불러오지 못했습니다")
        }
    }

    public func tapNotify(_ member: TeamMemberRef) {
        toast = DGToastItem(
            message: "알림 기능은 준비 중이에요",
            kind: .info
        )
    }

    // MARK: - Derived

    public var loadedSnapshot: Snapshot? {
        if case .loaded(let s) = state { return s }
        return nil
    }

    public var voteRows: [AttendanceVote] {
        guard let s = loadedSnapshot else { return [] }
        let all = s.summary.votes
        switch filter {
        case .all:     return all.filter { $0.status != .maybe }
        case .attend:  return all.filter { [.attend, .late, .earlyLeave].contains($0.status) }
        case .absent:  return all.filter { $0.status == .absent }
        case .pending: return []
        }
    }

    public var maybeRows: [AttendanceVote] {
        guard filter == .all, let s = loadedSnapshot else { return [] }
        return s.summary.votes.filter { $0.status == .maybe }
    }

    public var pendingRows: [TeamMemberRef] {
        guard let s = loadedSnapshot else { return [] }
        switch filter {
        case .all, .pending:
            let responded = Set(s.summary.votes.map { $0.userId })
            return s.activeMembers
                .filter { !responded.contains($0.userId) }
                .sorted { $0.nickname < $1.nickname }
        case .attend, .absent:
            return []
        }
    }

    public var availableFilters: [Filter] {
        guard let s = loadedSnapshot else { return Filter.allCases }
        let responded = Set(s.summary.votes.map { $0.userId })
        let pendingCount = s.activeMembers.filter { !responded.contains($0.userId) }.count
        if pendingCount == 0 {
            return [.all, .attend, .absent]
        }
        return Filter.allCases
    }
}
