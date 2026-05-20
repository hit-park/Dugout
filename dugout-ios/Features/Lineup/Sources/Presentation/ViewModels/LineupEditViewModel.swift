//
//  LineupEditViewModel.swift
//  DugoutLineupFeature

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class LineupEditViewModel {
    public enum State: Sendable, Equatable {
        case editing
        case submitting
        case success(Lineup)
        case failed(String)
    }
    public enum SelectedTab: Sendable, Equatable, Hashable {
        case roster
        case battingOrder
    }

    public private(set) var state: State = .editing
    public var draft: LineupDraft
    public var selectedTab: SelectedTab = .roster
    public var presentAssignSheet: Bool = false
    public var assignTargetEntry: LineupDraftEntry? = nil

    public let matchId: Int64
    public let isUpdate: Bool
    public let showOverwriteBanner: Bool
    public let attendees: [Attendee]
    private let repository: any LineupRepository

    public init(
        matchId: Int64,
        source: LineupViewModel.EditSource,
        existingLineupExists: Bool,
        repository: any LineupRepository = LineupRepositoryImpl()
    ) {
        self.matchId = matchId
        self.repository = repository

        switch source {
        case .empty(let attendees):
            self.attendees = attendees
            self.draft = LineupDraft(entries: attendees.map {
                LineupDraftEntry(
                    userId: $0.userId,
                    nickname: $0.nickname,
                    jerseyNumber: $0.jerseyNumber,
                    position: .designatedHitter,
                    battingOrder: nil,
                    isBench: true
                )
            })
            self.isUpdate = existingLineupExists
            self.showOverwriteBanner = false

        case .recommendation(let rec, let attendees):
            self.attendees = attendees
            self.draft = rec.draft
            self.isUpdate = existingLineupExists
            self.showOverwriteBanner = existingLineupExists

        case .existing(let lineup, let attendees):
            self.attendees = attendees
            let attendeesByUserId = Dictionary(
                uniqueKeysWithValues: attendees.map { ($0.userId, $0) }
            )
            self.draft = LineupDraft(entries: lineup.entries.map { entry in
                LineupDraftEntry(
                    userId: entry.userId,
                    nickname: entry.nickname,
                    jerseyNumber: attendeesByUserId[entry.userId]?.jerseyNumber,
                    position: entry.position,
                    battingOrder: entry.battingOrder,
                    isBench: entry.isBench
                )
            })
            self.isUpdate = true
            self.showOverwriteBanner = false
        }
    }

    // MARK: - Actions

    public func openAssignSheet(for entry: LineupDraftEntry) {
        assignTargetEntry = entry
        presentAssignSheet = true
    }

    public func closeAssignSheet() {
        presentAssignSheet = false
        assignTargetEntry = nil
    }

    /// AssignSheet 확정 콜백. 충돌 시 기존 entry 를 unassigned(bench) 로 자동 리셋.
    public func applyAssignment(
        userId: Int64,
        position: BaseballPosition,
        battingOrder: Int?,
        isBench: Bool
    ) {
        var newEntries = draft.entries

        if !isBench {
            for i in newEntries.indices {
                if newEntries[i].userId != userId
                    && newEntries[i].position == position
                    && !newEntries[i].isBench {
                    newEntries[i].position = .designatedHitter
                    newEntries[i].battingOrder = nil
                    newEntries[i].isBench = true
                }
            }
            if let order = battingOrder {
                for i in newEntries.indices {
                    if newEntries[i].userId != userId
                        && newEntries[i].battingOrder == order {
                        newEntries[i].battingOrder = nil
                    }
                }
            }
        }

        if let idx = newEntries.firstIndex(where: { $0.userId == userId }) {
            newEntries[idx].position = position
            newEntries[idx].battingOrder = isBench ? nil : battingOrder
            newEntries[idx].isBench = isBench
        }

        draft.entries = newEntries
        closeAssignSheet()
    }

    public func sendToBench(_ entry: LineupDraftEntry) {
        applyAssignment(
            userId: entry.userId,
            position: .designatedHitter,
            battingOrder: nil,
            isBench: true
        )
    }

    public func swapBattingOrder(from order1: Int, to order2: Int) {
        var newEntries = draft.entries
        let idx1 = newEntries.firstIndex { $0.battingOrder == order1 }
        let idx2 = newEntries.firstIndex { $0.battingOrder == order2 }
        if let idx1, let idx2 {
            newEntries[idx1].battingOrder = order2
            newEntries[idx2].battingOrder = order1
            draft.entries = newEntries
        }
    }

    // MARK: - Validation

    public var validationErrors: [String] {
        var errors: [String] = []
        let active = draft.entries.filter { !$0.isBench }
        let fieldEntries = active.filter { $0.position.isField }

        // 1. 필드 9 포지션 모두 채워야 함
        let occupiedFieldPositions = Set(fieldEntries.map(\.position))
        for needed in BaseballPosition.fieldPositions {
            if !occupiedFieldPositions.contains(needed) {
                errors.append("\(needed.displayName) 자리가 비어있어요")
            }
        }
        // 2. 동일 필드 포지션 중복
        let positionCounts = Dictionary(grouping: fieldEntries, by: \.position)
            .mapValues(\.count)
        for (pos, n) in positionCounts where n > 1 {
            errors.append("\(pos.displayName)에 \(n)명이 배정됐어요")
        }
        // 3. DH 가 2명 이상
        let dhCount = active.filter { $0.position == .designatedHitter }.count
        if dhCount > 1 {
            errors.append("지명타자는 1명만 배정할 수 있어요")
        }
        // 4. 타순 1~9 모두 채워야 함
        let orders = active.compactMap(\.battingOrder).sorted()
        if Set(orders) != Set(1...9) {
            for missing in (1...9).filter({ !orders.contains($0) }) {
                errors.append("\(missing)번 타순이 비어있어요")
            }
        }
        // 5. 동일 타순 중복
        let orderCounts = Dictionary(grouping: orders, by: { $0 }).mapValues(\.count)
        for (n, count) in orderCounts where count > 1 {
            errors.append("\(n)번 타순에 \(count)명이 배정됐어요")
        }

        return errors
    }

    public var canSubmit: Bool {
        if case .submitting = state { return false }
        return validationErrors.isEmpty
    }

    // MARK: - Submit

    public func submit() async {
        state = .submitting
        let request = SaveLineupRequest(entries: draft.entries)
        do {
            let lineup: Lineup
            if isUpdate {
                lineup = try await repository.updateLineup(matchId: matchId, request: request)
            } else {
                lineup = try await postWithRetry(request: request)
            }
            state = .success(lineup)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("라인업 저장 중 오류가 발생했습니다")
        }
    }

    private func postWithRetry(request: SaveLineupRequest) async throws -> Lineup {
        do {
            return try await repository.saveLineup(matchId: matchId, request: request)
        } catch APIError.server(let response, _) where response.code == "LINEUP_ALREADY_EXISTS" {
            return try await repository.updateLineup(matchId: matchId, request: request)
        }
    }
}
