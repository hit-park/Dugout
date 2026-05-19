//
//  AttendanceVoteViewModel.swift
//  DugoutMatchFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class AttendanceVoteViewModel {
    public enum State: Sendable, Equatable {
        case editing
        case submitting
        case success(AttendanceVote)
        case failed(String)
    }

    public enum MainChoice: Sendable, Equatable {
        case attend
        case absent
        case maybe
    }

    public enum PartialChoice: Sendable, Equatable {
        case none
        case late
        case earlyLeave
    }

    public private(set) var state: State = .editing
    public var mainChoice: MainChoice = .attend {
        didSet {
            guard mainChoice != oldValue else { return }
            if mainChoice != .attend { partialChoice = .none }
            reason = ""
        }
    }
    public var partialChoice: PartialChoice = .none
    public var reason: String = ""

    public let matchId: Int64
    private let existingVote: AttendanceVote?
    private let repository: any AttendanceRepository

    public init(
        matchId: Int64,
        existingVote: AttendanceVote?,
        repository: any AttendanceRepository = AttendanceRepositoryImpl()
    ) {
        self.matchId = matchId
        self.existingVote = existingVote
        self.repository = repository
        if let existingVote {
            let (main, partial) = Self.decompose(existingVote.status)
            self.mainChoice = main
            self.partialChoice = partial
            self.reason = existingVote.reason ?? ""
        }
    }

    public var isUpdate: Bool { existingVote != nil }

    public var resolvedStatus: AttendanceStatus {
        switch mainChoice {
        case .absent: return .absent
        case .maybe: return .maybe
        case .attend:
            switch partialChoice {
            case .none: return .attend
            case .late: return .late
            case .earlyLeave: return .earlyLeave
            }
        }
    }

    public var canSubmit: Bool {
        if case .submitting = state { return false }
        if reason.count > 200 { return false }
        return true
    }

    public var partialEnabled: Bool {
        mainChoice == .attend
    }

    public func submit() async {
        state = .submitting
        let trimmed = reason.trimmingCharacters(in: .whitespaces)
        let request = AttendanceVoteRequest(
            status: resolvedStatus,
            reason: trimmed.isEmpty ? nil : trimmed
        )
        do {
            let vote: AttendanceVote
            if existingVote != nil {
                vote = try await repository.updateVote(matchId: matchId, request: request)
            } else {
                vote = try await postWithRetry(request: request)
            }
            state = .success(vote)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("응답 저장 중 오류가 발생했습니다")
        }
    }

    /// POST 가 ALREADY_VOTED 로 실패하면 PUT 으로 투명 재시도.
    private func postWithRetry(request: AttendanceVoteRequest) async throws -> AttendanceVote {
        do {
            return try await repository.createVote(matchId: matchId, request: request)
        } catch APIError.server(let response, _) where response.code == "ALREADY_VOTED" {
            return try await repository.updateVote(matchId: matchId, request: request)
        }
    }

    private static func decompose(_ status: AttendanceStatus) -> (MainChoice, PartialChoice) {
        switch status {
        case .attend:     return (.attend, .none)
        case .late:       return (.attend, .late)
        case .earlyLeave: return (.attend, .earlyLeave)
        case .absent:     return (.absent, .none)
        case .maybe:      return (.maybe, .none)
        }
    }
}
