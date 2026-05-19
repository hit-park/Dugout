import Foundation
import DugoutCoreNetwork

public struct HomeRepositoryImpl: HomeRepository {
    private let client: APIClient

    public init(client: APIClient = .shared) {
        self.client = client
    }

    public func fetchMyTeams() async throws -> [MyTeam] {
        let endpoint = APIEndpoint(path: "/api/v1/users/me/teams")
        let dtos: [MyTeamDTO] = try await client.request(endpoint)
        return dtos.map { $0.toDomain() }
    }

    public func fetchDashboard(teamId: Int64) async throws -> Dashboard {
        // BACKEND-GAP: GET /home/dashboard 미구현
        // 기존 엔드포인트 조합으로 대체:
        //   - GET /api/v1/teams/{teamId}/matches?status=SCHEDULED → 다음 경기
        //   - dugout-ai 출석예측 → APIEndpoint가 단일 baseURL만 지원하므로 mock
        //   - 공지 API 미구현 → 빈 배열

        let nextMatch = try await fetchNextMatch(teamId: teamId)

        let prediction: AttendancePrediction? = nextMatch.flatMap { match in
            guard match.dDayValue >= 2 else { return nil }
            // BACKEND-GAP: dugout-ai /predict/attendance/{matchId} 미연동
            // APIEndpoint 단일 baseURL 제약 → mock 예측값 반환
            return AttendancePrediction(
                matchId: match.id,
                minCount: 10,
                maxCount: 14,
                totalMembers: 18
            )
        }

        // BACKEND-GAP: 공지 API 미구현 → 빈 배열
        let notices: [Notice] = []

        return Dashboard(
            teamId: teamId,
            nextMatch: nextMatch,
            attendancePrediction: prediction,
            notices: notices
        )
    }

    private func fetchNextMatch(teamId: Int64) async throws -> NextMatch? {
        let endpoint = APIEndpoint(
            path: "/api/v1/teams/\(teamId)/matches",
            queryItems: [URLQueryItem(name: "status", value: "SCHEDULED")]
        )
        let dtos: [MatchListItemDTO] = try await client.request(endpoint)
        let now = Date()
        return dtos
            .compactMap { $0.toNextMatch() }
            .filter { $0.scheduledAt >= now }
            .sorted { $0.scheduledAt < $1.scheduledAt }
            .first
    }
}
