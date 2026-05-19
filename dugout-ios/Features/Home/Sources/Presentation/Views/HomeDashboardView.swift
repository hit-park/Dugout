import SwiftUI
import DugoutDesignSystem
import DugoutAuthFeature

struct HomeDashboardView: View {
    @Bindable var viewModel: HomeViewModel
    @Bindable var authViewModel: AuthViewModel

    var body: some View {
        ScrollView {
            VStack(spacing: DGSpacing.md) {
                teamChipBar
                    .padding(.horizontal, DGSpacing.screenPadding)

                dashboardContent
                    .padding(.horizontal, DGSpacing.screenPadding)
            }
            .padding(.top, DGSpacing.md)
            .padding(.bottom, DGSpacing.xl)
        }
    }

    // MARK: - 팀 전환 칩 가로 스크롤

    private var teamChipBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: DGSpacing.sm) {
                ForEach(viewModel.myTeams) { team in
                    DGChip(
                        team.teamName,
                        kind: .selectable(isSelected: viewModel.selectedTeamId == team.teamId)
                    ) {
                        viewModel.selectTeam(team.teamId)
                    }
                }
            }
        }
    }

    // MARK: - 대시보드 컨텐츠

    @ViewBuilder
    private var dashboardContent: some View {
        switch viewModel.dashboardState {
        case .idle:
            EmptyView()
        case .loading:
            dashboardSkeleton
        case .loaded(let dashboard):
            dashboardCards(dashboard)
        case .failed(let msg):
            DGErrorState(message: msg) {
                Task { await viewModel.retryDashboard() }
            }
        }
    }

    // MARK: - Skeleton (로딩 중)

    private var dashboardSkeleton: some View {
        VStack(spacing: DGSpacing.md) {
            ForEach(0..<3, id: \.self) { _ in
                RoundedRectangle(cornerRadius: DGRadius.card)
                    .fill(DGColor.c100)
                    .frame(height: 96)
                    .shimmer()
            }
        }
    }

    // MARK: - 실제 카드들

    private func dashboardCards(_ dashboard: Dashboard) -> some View {
        VStack(spacing: DGSpacing.md) {
            nextMatchCard(dashboard.nextMatch)

            if let prediction = dashboard.attendancePrediction {
                aiPredictionCard(prediction)
            }

            if !dashboard.notices.isEmpty {
                noticesCard(dashboard.notices)
            }
        }
    }

    // MARK: - 다음 경기 카드

    @ViewBuilder
    private func nextMatchCard(_ match: NextMatch?) -> some View {
        DGCard {
            if let match {
                VStack(alignment: .leading, spacing: DGSpacing.sm) {
                    HStack {
                        DGBadge(match.dDayLabel, variant: match.dDayValue > 0 ? .dDay : .neutral)
                        Spacer()
                        Text(match.scheduledAt, style: .date)
                            .font(DGFont.subText)
                            .foregroundStyle(DGColor.c500)
                        Text(match.scheduledAt, style: .time)
                            .font(DGFont.subText)
                            .foregroundStyle(DGColor.c500)
                    }

                    Text(match.opponentName.map { "vs \($0)" } ?? "상대팀 미정")
                        .font(DGFont.sectionTitle)
                        .foregroundStyle(DGColor.c900)

                    if let ground = match.groundName {
                        Label(ground, systemImage: "mappin.circle")
                            .font(DGFont.subText)
                            .foregroundStyle(DGColor.c500)
                    }
                }
            } else {
                HStack(spacing: DGSpacing.md) {
                    Image(systemName: "calendar.badge.plus")
                        .font(.system(size: 28))
                        .foregroundStyle(DGColor.c300)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("예정된 경기가 없어요")
                            .font(DGFont.cardTitle)
                            .foregroundStyle(DGColor.c700)
                        Text("주장이 경기를 등록하면 여기에 표시돼요")
                            .font(DGFont.subText)
                            .foregroundStyle(DGColor.c400)
                    }
                }
            }
        }
    }

    // MARK: - AI 출석 예측 카드

    private func aiPredictionCard(_ prediction: AttendancePrediction) -> some View {
        DGAICard(
            headline: prediction.headline,
            rows: []
        )
    }

    // MARK: - 공지 카드

    private func noticesCard(_ notices: [Notice]) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                Text("공지")
                    .font(DGFont.label)
                    .foregroundStyle(DGColor.c500)
                ForEach(notices) { notice in
                    HStack {
                        Text(notice.title)
                            .font(DGFont.bodyText)
                            .foregroundStyle(DGColor.c900)
                            .lineLimit(1)
                        Spacer()
                        Text(notice.createdAt, style: .date)
                            .font(DGFont.subText)
                            .foregroundStyle(DGColor.c400)
                    }
                }
            }
        }
    }
}

// MARK: - Shimmer ViewModifier

private struct ShimmerModifier: ViewModifier {
    @State private var phase: CGFloat = -1

    func body(content: Content) -> some View {
        content
            .overlay(
                GeometryReader { geo in
                    LinearGradient(
                        stops: [
                            .init(color: .clear, location: 0),
                            .init(color: .white.opacity(0.5), location: 0.4),
                            .init(color: .clear, location: 1),
                        ],
                        startPoint: .init(x: phase, y: 0.5),
                        endPoint: .init(x: phase + 0.6, y: 0.5)
                    )
                    .frame(width: geo.size.width, height: geo.size.height)
                }
            )
            .clipped()
            .onAppear {
                withAnimation(.linear(duration: 1.2).repeatForever(autoreverses: false)) {
                    phase = 1.4
                }
            }
    }
}

private extension View {
    func shimmer() -> some View { modifier(ShimmerModifier()) }
}
