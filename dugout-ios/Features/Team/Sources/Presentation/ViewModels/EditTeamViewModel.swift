//
//  EditTeamViewModel.swift
//  DugoutTeamFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class EditTeamViewModel {
    public enum State: Sendable {
        case editing
        case submitting
        case success(Team)
        case failed(String)
    }

    public private(set) var state: State = .editing

    public var name: String
    public var region: String
    public var division: Int
    public var activityDays: Set<DayOfWeek>
    public var activityTime: String
    public var lineupMode: LineupMode

    public let availableDivisions: [Int] = [1, 2, 3, 4]

    private let teamId: Int64
    private let repository: any TeamRepository

    public init(team: Team, repository: any TeamRepository = TeamRepositoryImpl()) {
        self.teamId = team.id
        self.repository = repository
        self.name = team.name
        self.region = team.region
        self.division = team.division
        // 알 수 없는 day code는 silently 무시. DayOfWeek은 ISO 7개로 고정 가정.
        // 백엔드가 새 코드를 추가하면 PUT 시 손실되므로, 그 시점에 enum 확장 필요.
        self.activityDays = Set(team.activityDays.compactMap { DayOfWeek(rawValue: $0) })
        self.activityTime = team.activityTime ?? ""
        self.lineupMode = team.lineupMode
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
        let request = UpdateTeamRequest(
            name: trimmedName,
            region: trimmedRegion,
            division: division,
            activityDays: Array(activityDays).map(\.rawValue).sorted(),
            activityTime: trimmedTime.isEmpty ? nil : trimmedTime,
            lineupMode: lineupMode
        )
        do {
            let team = try await repository.updateTeam(id: teamId, request: request)
            state = .success(team)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 정보 수정 중 오류가 발생했습니다")
        }
    }
}
