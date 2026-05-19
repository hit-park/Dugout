import SwiftUI
import DugoutDesignSystem
import DugoutAuthFeature
import DugoutTeamFeature

public struct HomeView: View {
    @State private var viewModel: HomeViewModel
    @Bindable var authViewModel: AuthViewModel

    public init(
        viewModel: HomeViewModel = HomeViewModel(),
        authViewModel: AuthViewModel
    ) {
        _viewModel = State(wrappedValue: viewModel)
        self.authViewModel = authViewModel
    }

    public var body: some View {
        content
            .background(DGColor.c100.ignoresSafeArea())
            .id(authViewModel.isAuthenticated)
            .task {
                guard authViewModel.isAuthenticated else { return }
                await viewModel.loadTeams()
            }
            .onChange(of: authViewModel.isAuthenticated) { _, isAuth in
                if isAuth { Task { await viewModel.loadTeams() } }
            }
            .sheet(item: $viewModel.presentedSheet) { sheet in
                switch sheet {
                case .createTeam:
                    CreateTeamView { await viewModel.onTeamMutated() }
                case .joinTeam:
                    JoinTeamView { await viewModel.onTeamMutated() }
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        if !authViewModel.isAuthenticated {
            emptyHome
        } else {
            switch viewModel.teamsState {
            case .idle, .loading:
                DGLoadingState()
            case .loaded:
                if viewModel.hasNoTeams {
                    emptyHome
                } else {
                    HomeDashboardView(viewModel: viewModel, authViewModel: authViewModel)
                }
            case .failed(let msg):
                DGErrorState(message: msg) {
                    Task { await viewModel.loadTeams() }
                }
            }
        }
    }

    // MARK: - HOME-1: 팀 없는 Empty 상태

    private var emptyHome: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 80)

                VStack(spacing: DGSpacing.md) {
                    Image(systemName: "baseball.diamond.bases")
                        .font(.system(size: 64))
                        .foregroundStyle(DGColor.p200)
                    Text("아직 소속된 팀이 없어요")
                        .font(DGFont.sectionTitle)
                        .foregroundStyle(DGColor.c700)
                    Text("팀을 만들거나 초대 코드로 합류해 보세요")
                        .font(DGFont.bodyText)
                        .foregroundStyle(DGColor.c400)
                        .multilineTextAlignment(.center)
                }

                Spacer().frame(height: 48)

                VStack(spacing: DGSpacing.sm) {
                    DGButton("팀 만들기", style: .primary) {
                        viewModel.tapCreateTeam()
                    }
                    DGButton("초대 코드로 참가", style: .secondary) {
                        viewModel.tapJoinTeam()
                    }
                }
                .padding(.horizontal, DGSpacing.screenPadding)
            }
            .frame(maxWidth: .infinity)
        }
    }
}
