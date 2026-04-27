//
//  HomeView.swift
//  DugoutHomeFeature
//

import SwiftUI
import DugoutDesignSystem
import DugoutAuthFeature

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
            Group {
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
            .background(DGColor.background)
            .navigationTitle("내 팀")
            .toolbar {
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
        .task {
            await viewModel.loadTeams()
        }
    }

    private func teamList(teams: [MyTeam]) -> some View {
        ScrollView {
            VStack(spacing: DGSpacing.md) {
                ForEach(teams) { team in
                    DGCard {
                        VStack(alignment: .leading, spacing: DGSpacing.sm) {
                            HStack {
                                Text(team.teamName)
                                    .font(DGFont.title3)
                                Spacer()
                                roleBadge(team.role)
                            }
                            Text("가입일: \(team.joinedAt.formatted(date: .abbreviated, time: .omitted))")
                                .font(DGFont.footnote)
                                .foregroundStyle(DGColor.textSecondary)
                        }
                    }
                }
            }
            .padding(DGSpacing.lg)
        }
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

    private var emptyState: some View {
        VStack(spacing: DGSpacing.md) {
            Image(systemName: "person.3")
                .font(.system(size: 48))
                .foregroundStyle(DGColor.textSecondary)
            Text("아직 소속된 팀이 없습니다")
                .font(DGFont.headline)
            Text("팀을 만들거나 초대 코드로 참가해보세요")
                .font(DGFont.footnote)
                .foregroundStyle(DGColor.textSecondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
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
}
