//
//  LineupViewModel.swift
//  DugoutLineupFeature

import Foundation
import Observation
import DugoutCoreNetwork
import DugoutDesignSystem

@MainActor
@Observable
public final class LineupViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case empty
        case loaded(Lineup)
        case recommending
        case failed(String)
    }

    public enum EditSource: Sendable, Equatable {
        case empty(attendees: [Attendee])
        case recommendation(LineupRecommendation, attendees: [Attendee])
        case existing(Lineup, attendees: [Attendee])
    }

    public private(set) var state: State = .idle
    public var presentEdit: Bool = false
    public var editSource: EditSource? = nil
    public var toast: DGToastItem? = nil

    public let matchId: Int64
    public let teamId: Int64
    public let isManager: Bool
    private let lineupRepository: any LineupRepository
    private let attendeeRepository: any AttendeeRepository

    public init(
        matchId: Int64,
        teamId: Int64,
        isManager: Bool,
        lineupRepository: any LineupRepository = LineupRepositoryImpl(),
        attendeeRepository: any AttendeeRepository = AttendeeRepositoryImpl()
    ) {
        self.matchId = matchId
        self.teamId = teamId
        self.isManager = isManager
        self.lineupRepository = lineupRepository
        self.attendeeRepository = attendeeRepository
    }

    public func load() async {
        state = .loading
        do {
            let lineup = try await lineupRepository.fetchLineup(matchId: matchId)
            state = .loaded(lineup)
        } catch APIError.server(let response, _) where response.code == "LINEUP_NOT_FOUND" {
            state = .empty
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("라인업을 불러오지 못했습니다")
        }
    }

    public func tapRecommend() async {
        let previousState = state
        state = .recommending
        do {
            let attendees = try await attendeeRepository.fetchAttendees(
                matchId: matchId, teamId: teamId
            )
            let recommendation = try await lineupRepository.recommend(
                matchId: matchId, attendees: attendees
            )
            editSource = .recommendation(recommendation, attendees: attendees)
            state = previousState
            presentEdit = true
        } catch APIError.server(let response, _) where response.code == "INSUFFICIENT_ATTENDEES" {
            state = previousState
            toast = DGToastItem(message: "출석자가 9명 미만이에요", kind: .warning)
        } catch APIError.server(let response, _) where response.code == "AI_SERVICE_UNAVAILABLE" {
            state = previousState
            toast = DGToastItem(
                message: "AI 서비스에 일시적으로 접근할 수 없어요", kind: .danger
            )
        } catch let error as APIError {
            state = previousState
            toast = DGToastItem(message: error.userMessage, kind: .danger)
        } catch {
            state = previousState
            toast = DGToastItem(message: "라인업 추천 중 오류가 발생했어요", kind: .danger)
        }
    }

    public func tapEditExisting() async {
        guard case .loaded(let lineup) = state else { return }
        do {
            let attendees = try await attendeeRepository.fetchAttendees(
                matchId: matchId, teamId: teamId
            )
            editSource = .existing(lineup, attendees: attendees)
            presentEdit = true
        } catch let error as APIError {
            toast = DGToastItem(message: error.userMessage, kind: .danger)
        } catch {
            toast = DGToastItem(message: "출석자 정보를 불러오지 못했어요", kind: .danger)
        }
    }

    public func tapWriteFromScratch() async {
        do {
            let attendees = try await attendeeRepository.fetchAttendees(
                matchId: matchId, teamId: teamId
            )
            editSource = .empty(attendees: attendees)
            presentEdit = true
        } catch let error as APIError {
            toast = DGToastItem(message: error.userMessage, kind: .danger)
        } catch {
            toast = DGToastItem(message: "출석자 정보를 불러오지 못했어요", kind: .danger)
        }
    }

    public func onEditCompleted(_ lineup: Lineup) {
        state = .loaded(lineup)
        presentEdit = false
        editSource = nil
    }

    public func onEditCancelled() {
        presentEdit = false
        editSource = nil
    }

    public var hasExistingLineup: Bool {
        if case .loaded = state { return true }
        return false
    }
}
