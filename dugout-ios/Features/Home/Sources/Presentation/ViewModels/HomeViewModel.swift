import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class HomeViewModel {
    public enum TeamsState: Sendable {
        case idle
        case loading
        case loaded([MyTeam])
        case failed(String)
    }

    public enum DashboardState: Sendable {
        case idle
        case loading
        case loaded(Dashboard)
        case failed(String)
    }

    public enum PresentedSheet: Identifiable, Sendable {
        case createTeam
        case joinTeam

        public var id: String {
            switch self {
            case .createTeam: "createTeam"
            case .joinTeam: "joinTeam"
            }
        }
    }

    public private(set) var teamsState: TeamsState = .idle
    public private(set) var dashboardState: DashboardState = .idle
    public var selectedTeamId: Int64? = nil
    public var presentedSheet: PresentedSheet?

    private let repository: any HomeRepository
    private var dashboardTask: Task<Void, Never>?

    public init(repository: any HomeRepository = HomeRepositoryImpl()) {
        self.repository = repository
    }

    // MARK: - Teams

    public func loadTeams() async {
        teamsState = .loading
        do {
            let teams = try await repository.fetchMyTeams()
            teamsState = .loaded(teams)
            if let first = teams.first, selectedTeamId == nil {
                selectedTeamId = first.teamId
                await loadDashboard(teamId: first.teamId)
            }
        } catch let error as APIError {
            teamsState = .failed(error.userMessage)
        } catch {
            teamsState = .failed("팀 목록을 불러오지 못했습니다")
        }
    }

    // MARK: - Dashboard

    public func selectTeam(_ teamId: Int64) {
        guard teamId != selectedTeamId else { return }
        selectedTeamId = teamId
        dashboardTask?.cancel()
        dashboardTask = Task {
            // 280ms skeleton: 너무 빠른 깜빡임 방지
            dashboardState = .loading
            try? await Task.sleep(for: .milliseconds(280))
            guard !Task.isCancelled else { return }
            await loadDashboard(teamId: teamId)
        }
    }

    public func loadDashboard(teamId: Int64) async {
        dashboardState = .loading
        do {
            let dashboard = try await repository.fetchDashboard(teamId: teamId)
            dashboardState = .loaded(dashboard)
        } catch let error as APIError {
            dashboardState = .failed(error.userMessage)
        } catch {
            dashboardState = .failed("대시보드를 불러오지 못했습니다")
        }
    }

    public func retryDashboard() async {
        guard let teamId = selectedTeamId else { return }
        await loadDashboard(teamId: teamId)
    }

    // MARK: - Team actions

    public func tapCreateTeam() { presentedSheet = .createTeam }
    public func tapJoinTeam() { presentedSheet = .joinTeam }

    public func onTeamMutated() async {
        presentedSheet = nil
        selectedTeamId = nil
        await loadTeams()
    }

    // MARK: - Helpers

    public var myTeams: [MyTeam] {
        if case .loaded(let teams) = teamsState { return teams }
        return []
    }

    public var hasNoTeams: Bool {
        if case .loaded(let teams) = teamsState { return teams.isEmpty }
        return false
    }
}
