//
//  CreateMatchViewModel.swift
//  DugoutMatchFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class CreateMatchViewModel {
    public enum State: Sendable {
        case editing
        case submitting
        case success(Match)
        case failed(String)
    }

    public private(set) var state: State = .editing

    public var matchDate: Date = Calendar.koreaCalendar.startOfDay(for: defaultDate())
    public var matchTime: Date = defaultTimeOfDay(hour: 8)
    public var hasGatherTime: Bool = true
    public var gatherTime: Date = defaultTimeOfDay(hour: 7, minute: 30)
    public var opponentName: String = ""
    public var groundName: String = ""
    public var hasVoteDeadline: Bool = false
    public var voteDeadline: Date = defaultDeadline()
    public var memo: String = ""

    public let teamId: Int64
    private let repository: any MatchRepository

    public init(
        teamId: Int64,
        repository: any MatchRepository = MatchRepositoryImpl()
    ) {
        self.teamId = teamId
        self.repository = repository
    }

    public var canSubmit: Bool {
        if case .submitting = state { return false }
        return true
    }

    public var isSubmitting: Bool {
        if case .submitting = state { return true }
        return false
    }

    public func submit() async {
        state = .submitting
        let trimmedOpponent = opponentName.trimmingCharacters(in: .whitespaces)
        let trimmedGround = groundName.trimmingCharacters(in: .whitespaces)
        let trimmedMemo = memo.trimmingCharacters(in: .whitespaces)

        let request = CreateMatchRequest(
            matchDate: matchDate,
            matchTime: timeOfDay(from: matchTime),
            gatherTime: hasGatherTime ? timeOfDay(from: gatherTime) : nil,
            opponentName: trimmedOpponent.isEmpty ? nil : trimmedOpponent,
            groundName: trimmedGround.isEmpty ? nil : trimmedGround,
            voteDeadline: hasVoteDeadline ? voteDeadline : nil,
            memo: trimmedMemo.isEmpty ? nil : trimmedMemo
        )

        do {
            let match = try await repository.createMatch(teamId: teamId, request: request)
            state = .success(match)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("경기 등록 중 오류가 발생했습니다")
        }
    }

    private func timeOfDay(from date: Date) -> TimeOfDay {
        let comps = Calendar.koreaCalendar.dateComponents([.hour, .minute], from: date)
        return TimeOfDay(hour: comps.hour ?? 0, minute: comps.minute ?? 0)
    }

    private static func defaultDate() -> Date {
        Calendar.koreaCalendar.date(byAdding: .day, value: 7, to: Date()) ?? Date()
    }

    private static func defaultTimeOfDay(hour: Int, minute: Int = 0) -> Date {
        var comps = Calendar.koreaCalendar.dateComponents([.year, .month, .day], from: Date())
        comps.hour = hour
        comps.minute = minute
        return Calendar.koreaCalendar.date(from: comps) ?? Date()
    }

    private static func defaultDeadline() -> Date {
        Calendar.koreaCalendar.date(byAdding: .day, value: 6, to: Date()) ?? Date()
    }
}
