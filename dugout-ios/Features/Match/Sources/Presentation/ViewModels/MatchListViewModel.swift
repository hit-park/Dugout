//
//  MatchListViewModel.swift
//  DugoutMatchFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class MatchListViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case loaded([Match])
        case failed(String)
    }

    public private(set) var state: State = .idle
    public private(set) var displayedMonth: Date = Date()
    public var selectedDate: Date? = nil
    public var presentCreateSheet: Bool = false

    public let teamId: Int64
    public let isManager: Bool
    public let currentUserId: Int64
    private let repository: any MatchRepository
    private var loadTask: Task<Void, Never>?

    public init(
        teamId: Int64,
        isManager: Bool,
        currentUserId: Int64,
        repository: any MatchRepository = MatchRepositoryImpl()
    ) {
        self.teamId = teamId
        self.isManager = isManager
        self.currentUserId = currentUserId
        self.repository = repository
    }

    public func load() async {
        state = .loading
        let (from, to) = monthRange(for: displayedMonth)
        do {
            let matches = try await repository.fetchMatches(teamId: teamId, from: from, to: to)
            guard !Task.isCancelled else { return }
            state = .loaded(matches)
        } catch let error as APIError {
            guard !Task.isCancelled else { return }
            state = .failed(error.userMessage)
        } catch {
            guard !Task.isCancelled else { return }
            state = .failed("일정을 불러오지 못했습니다")
        }
    }

    public func goPreviousMonth() async {
        displayedMonth = shifted(months: -1)
        selectedDate = nil
        loadTask?.cancel()
        let task = Task { await load() }
        loadTask = task
        await task.value
    }

    public func goNextMonth() async {
        displayedMonth = shifted(months: 1)
        selectedDate = nil
        loadTask?.cancel()
        let task = Task { await load() }
        loadTask = task
        await task.value
    }

    public func tapCreate() {
        presentCreateSheet = true
    }

    public func onCreated(_ match: Match) async {
        presentCreateSheet = false
        await load()
    }

    public var monthLabel: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ko_KR")
        formatter.dateFormat = "yyyy년 M월"
        return formatter.string(from: displayedMonth)
    }

    public var matches: [Match] {
        if case .loaded(let list) = state { return list }
        return []
    }

    public var filteredMatches: [Match] {
        guard let selectedDate else { return matches }
        let calendar = Calendar.koreaCalendar
        return matches.filter {
            calendar.isDate($0.matchDate, inSameDayAs: selectedDate)
        }
    }

    public func hasMatch(on date: Date) -> Bool {
        let calendar = Calendar.koreaCalendar
        return matches.contains { calendar.isDate($0.matchDate, inSameDayAs: date) }
    }

    // MARK: - Helpers

    private func monthRange(for date: Date) -> (Date, Date) {
        let calendar = Calendar.koreaCalendar
        let comps = calendar.dateComponents([.year, .month], from: date)
        let start = calendar.date(from: comps) ?? date
        let endComps = DateComponents(month: 1, day: -1)
        let end = calendar.date(byAdding: endComps, to: start) ?? date
        return (start, end)
    }

    private func shifted(months delta: Int) -> Date {
        Calendar.koreaCalendar.date(byAdding: .month, value: delta, to: displayedMonth) ?? displayedMonth
    }
}
