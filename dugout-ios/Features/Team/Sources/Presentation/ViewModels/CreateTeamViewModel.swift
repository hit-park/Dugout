//
//  CreateTeamViewModel.swift
//  DugoutTeamFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class CreateTeamViewModel {
    public enum State: Sendable {
        case editing
        case submitting
        case success(Team)
        case failed(String)
    }

    public private(set) var state: State = .editing

    public var name: String = ""
    public var region: String = ""
    public var division: Int = 4
    public var activityDays: Set<String> = []
    public var activityTime: String = ""
    public var lineupMode: LineupMode = .balanced

    public let availableDays: [String] = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]
    public let availableDivisions: [Int] = [1, 2, 3, 4]

    private let repository: any TeamRepository

    public init(repository: any TeamRepository = TeamRepositoryImpl()) {
        self.repository = repository
    }

    public var canSubmit: Bool {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        let trimmedRegion = region.trimmingCharacters(in: .whitespaces)
        if case .submitting = state { return false }
        return !trimmedName.isEmpty && !trimmedRegion.isEmpty
    }

    public func submit() async {
        let trimmedName = name.trimmingCharacters(in: .whitespaces)
        let trimmedRegion = region.trimmingCharacters(in: .whitespaces)
        let trimmedTime = activityTime.trimmingCharacters(in: .whitespaces)

        state = .submitting
        let request = CreateTeamRequest(
            name: trimmedName,
            region: trimmedRegion,
            division: division,
            activityDays: Array(activityDays).sorted(),
            activityTime: trimmedTime.isEmpty ? nil : trimmedTime,
            lineupMode: lineupMode
        )
        do {
            let team = try await repository.createTeam(request)
            state = .success(team)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 생성 중 오류가 발생했습니다")
        }
    }
}
