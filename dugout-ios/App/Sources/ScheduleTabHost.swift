import SwiftUI
import DugoutCoreNetwork
import DugoutAuthFeature
import DugoutHomeFeature
import DugoutMatchFeature
import DugoutDesignSystem

struct ScheduleTabHost: View {
    @Bindable var authViewModel: AuthViewModel
    @Bindable var router: AppRouter
    @State private var teams: [MyTeam]?
    @State private var errorMessage: String?
    private let repository: any HomeRepository = HomeRepositoryImpl()

    var body: some View {
        Group {
            if let errorMessage {
                DGErrorState(message: errorMessage) {
                    Task { await load() }
                }
            } else if let teams {
                if let firstTeam = teams.first,
                   let currentUserId = authViewModel.currentUser?.id {
                    NavigationStack(path: $router.schedulePath) {
                        MatchListView(
                            teamId: firstTeam.teamId,
                            isManager: firstTeam.role == .captain || firstTeam.role == .manager,
                            currentUserId: currentUserId
                        )
                    }
                } else {
                    DGEmptyState(
                        icon: "⚾",
                        title: "팀과 함께 시작해요",
                        message: "팀에 참가하면 경기 일정을 볼 수 있어요"
                    )
                    .background(DGColor.c100)
                }
            } else {
                DGLoadingState(preset: .list)
                    .background(DGColor.c100)
            }
        }
        .task { await load() }
    }

    private func load() async {
        errorMessage = nil
        do {
            let result = try await repository.fetchMyTeams()
            teams = result
        } catch let error as APIError {
            errorMessage = error.userMessage
        } catch {
            errorMessage = "팀 정보를 불러오지 못했습니다"
        }
    }
}
