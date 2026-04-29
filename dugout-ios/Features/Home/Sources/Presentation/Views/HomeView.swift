//
//  HomeView.swift
//  DugoutHomeFeature
//

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
        NavigationStack {
            content
                .background(DGColor.background)
                .navigationTitle("내 팀")
                .toolbar { toolbarContent }
        }
        // 로그아웃 시 NavigationStack을 통째로 재생성해 push된 TeamDetailView가
        // 비로그인 상태에서 잔존하지 않도록 identity를 인증 상태에 묶는다.
        .id(authViewModel.isAuthenticated)
        .task {
            if authViewModel.isAuthenticated {
                await viewModel.loadTeams()
            }
        }
        .onChange(of: authViewModel.isAuthenticated) { _, isAuth in
            if isAuth {
                Task { await viewModel.loadTeams() }
            }
        }
        .sheet(item: $viewModel.presentedSheet, onDismiss: {
            viewModel.onSheetDismissed(isAuthenticated: authViewModel.isAuthenticated)
        }) { sheet in
            switch sheet {
            case .createTeam:
                CreateTeamView { await viewModel.onTeamMutated() }
            case .joinTeam:
                JoinTeamView { await viewModel.onTeamMutated() }
            case .login:
                LoginSheet(authViewModel: authViewModel)
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if !authViewModel.isAuthenticated {
            guestContent
        } else {
            switch viewModel.state {
            case .idle, .loading:
                ProgressView("불러오는 중...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .loaded(let teams):
                if teams.isEmpty {
                    emptyState
                } else {
                    teamList(teams: teams)
                }
            case .failed(let message):
                failedState(message: message)
            }
        }
    }

    @ToolbarContentBuilder
    private var toolbarContent: some ToolbarContent {
        if authViewModel.isAuthenticated {
            ToolbarItem(placement: .topBarTrailing) {
                Menu {
                    Button("새로고침") {
                        Task { await viewModel.loadTeams() }
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
    }

    @ViewBuilder
    private var guestContent: some View {
        noTeamPlaceholder(iconSize: 56)
    }

    private func teamList(teams: [MyTeam]) -> some View {
        ScrollView {
            VStack(spacing: DGSpacing.md) {
                ForEach(teams) { team in
                    NavigationLink {
                        TeamDetailView(
                            viewModel: TeamDetailViewModel(
                                teamId: team.teamId,
                                currentUserId: authViewModel.currentUser?.id
                            )
                        )
                    } label: {
                        DGCard {
                            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                                HStack {
                                    Text(team.teamName)
                                        .font(DGFont.title3)
                                        .foregroundStyle(DGColor.textPrimary)
                                    Spacer()
                                    roleBadge(team.role)
                                }
                                Text("가입일: \(team.joinedAt.formatted(date: .abbreviated, time: .omitted))")
                                    .font(DGFont.footnote)
                                    .foregroundStyle(DGColor.textSecondary)
                            }
                        }
                    }
                    .buttonStyle(.plain)
                }

                actionButtons
            }
            .padding(DGSpacing.lg)
        }
    }

    private var actionButtons: some View {
        teamActionButtons
            .padding(.top, DGSpacing.lg)
    }

    private func roleBadge(_ role: TeamRole) -> some View {
        Text(role.displayName)
            .font(DGFont.caption)
            .padding(.horizontal, DGSpacing.sm)
            .padding(.vertical, DGSpacing.xs)
            .background(DGColor.primary.opacity(0.1))
            .foregroundStyle(DGColor.primary)
            .clipShape(Capsule())
    }

    @ViewBuilder
    private var emptyState: some View {
        noTeamPlaceholder(iconSize: 48)
    }

    private func failedState(message: String) -> some View {
        VStack(spacing: DGSpacing.md) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 40))
                .foregroundStyle(DGColor.warning)
            Text(message)
                .font(DGFont.callout)
                .multilineTextAlignment(.center)
                .padding(.horizontal, DGSpacing.xl)
            DGButton("다시 시도") {
                Task { await viewModel.loadTeams() }
            }
            .padding(.horizontal, DGSpacing.xl)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    @ViewBuilder
    private var teamActionButtons: some View {
        VStack(spacing: DGSpacing.sm) {
            DGButton("팀 만들기") {
                viewModel.tapCreateTeam(isAuthenticated: authViewModel.isAuthenticated)
            }
            DGButton("팀 가입하기") {
                viewModel.tapJoinTeam(isAuthenticated: authViewModel.isAuthenticated)
            }
        }
    }

    @ViewBuilder
    private func noTeamPlaceholder(iconSize: CGFloat) -> some View {
        VStack(spacing: DGSpacing.md) {
            Image(systemName: "person.3")
                .font(.system(size: iconSize))
                .foregroundStyle(DGColor.textSecondary)
            Text("아직 소속된 팀이 없어요")
                .font(DGFont.headline)
            Text("팀을 만들거나 초대 코드로 시작해보세요")
                .font(DGFont.footnote)
                .foregroundStyle(DGColor.textSecondary)

            teamActionButtons
                .padding(.horizontal, DGSpacing.xl)
                .padding(.top, DGSpacing.lg)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
