//
//  JoinTeamViewModel.swift
//  DugoutTeamFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class JoinTeamViewModel {
    public enum State: Sendable {
        case editing
        case submitting
        case success(TeamMember)
        case failed(String)
    }

    public private(set) var state: State = .editing
    public var inviteCode: String = ""

    private let repository: any TeamRepository

    public init(repository: any TeamRepository = TeamRepositoryImpl()) {
        self.repository = repository
    }

    public var canSubmit: Bool {
        let trimmed = inviteCode.trimmingCharacters(in: .whitespaces)
        if case .submitting = state { return false }
        return !trimmed.isEmpty
    }

    public func submit() async {
        let trimmed = inviteCode.trimmingCharacters(in: .whitespaces)
        state = .submitting
        do {
            let member = try await repository.joinTeam(inviteCode: trimmed)
            state = .success(member)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 가입 중 오류가 발생했습니다")
        }
    }
}
