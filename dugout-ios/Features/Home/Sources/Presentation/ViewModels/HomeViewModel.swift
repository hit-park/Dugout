//
//  HomeViewModel.swift
//  DugoutHomeFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class HomeViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case loaded([MyTeam])
        case failed(String)
    }

    public private(set) var state: State = .idle
    private let repository: any HomeRepository

    public init(repository: any HomeRepository = HomeRepositoryImpl()) {
        self.repository = repository
    }

    public func loadTeams() async {
        state = .loading
        do {
            let teams = try await repository.fetchMyTeams()
            state = .loaded(teams)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 목록을 불러오지 못했습니다")
        }
    }
}
