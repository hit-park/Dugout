import Foundation

public protocol HomeRepository: Sendable {
    func fetchMyTeams() async throws -> [MyTeam]
    func fetchDashboard(teamId: Int64) async throws -> Dashboard
}
