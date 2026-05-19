import SwiftUI
import DugoutDesignSystem

public struct OnboardingStartModeView: View {
    @Bindable var viewModel: OnboardingViewModel
    let onCreateTeam: () -> Void
    let onJoinTeam: () -> Void
    let onMercenary: () -> Void
    let onSkip: () -> Void

    @State private var selected: OnboardingStartMode? = nil

    public init(
        viewModel: OnboardingViewModel,
        onCreateTeam: @escaping () -> Void,
        onJoinTeam: @escaping () -> Void,
        onMercenary: @escaping () -> Void,
        onSkip: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.onCreateTeam = onCreateTeam
        self.onJoinTeam = onJoinTeam
        self.onMercenary = onMercenary
        self.onSkip = onSkip
    }

    public var body: some View {
        VStack(spacing: 0) {
            DGProgressBar(progress: 1.0)
                .padding(.horizontal, DGSpacing.screenPadding)
                .padding(.top, 8)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 40)

                    Text("어떻게 시작할까요?")
                        .font(DGFont.screenTitle)
                        .foregroundStyle(DGColor.c900)
                    Spacer().frame(height: 8)
                    Text("나중에 언제든지 변경할 수 있어요")
                        .font(DGFont.bodyText)
                        .foregroundStyle(DGColor.c500)

                    Spacer().frame(height: 32)

                    VStack(spacing: 12) {
                        modeCard(
                            mode: .createTeam,
                            icon: "person.badge.plus",
                            title: "팀 만들기",
                            subtitle: "새 팀을 만들고 팀원을 초대해요"
                        )
                        modeCard(
                            mode: .joinTeam,
                            icon: "link",
                            title: "팀 참가",
                            subtitle: "초대 코드로 기존 팀에 합류해요"
                        )
                        modeCard(
                            mode: .mercenary,
                            icon: "figure.baseball",
                            title: "용병으로 뛰기",
                            subtitle: "다양한 팀의 경기에 참가해요"
                        )
                    }

                    if case .failed(let msg) = viewModel.saveState {
                        Spacer().frame(height: 12)
                        Text(msg).font(DGFont.subText).foregroundStyle(DGColor.danger)
                    }

                    Spacer().frame(height: 16)
                }
                .padding(.horizontal, DGSpacing.screenPadding)
            }

            Spacer()

            VStack(spacing: 12) {
                DGButton(
                    "시작하기",
                    style: .primary,
                    isLoading: isSaving,
                    isEnabled: selected != nil && !isSaving
                ) {
                    guard let mode = selected else { return }
                    Task { await proceed(with: mode) }
                }

                Button {
                    Task { await proceed(with: nil) }
                } label: {
                    Text("나중에 결정하기")
                        .font(DGFont.label)
                        .foregroundStyle(DGColor.c400)
                }
                .disabled(isSaving)
            }
            .padding(.horizontal, DGSpacing.screenPadding)
            .padding(.bottom, DGSpacing.lg)
        }
        .background(DGColor.c0.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
    }

    // MARK: - Subviews

    private func modeCard(
        mode: OnboardingStartMode,
        icon: String,
        title: String,
        subtitle: String
    ) -> some View {
        let isSelected = selected == mode
        return Button {
            withAnimation(.spring(response: 0.18, dampingFraction: 0.7)) {
                selected = isSelected ? nil : mode
            }
        } label: {
            HStack(spacing: DGSpacing.md) {
                ZStack {
                    Circle()
                        .fill(isSelected ? DGColor.p500 : DGColor.c100)
                        .frame(width: 48, height: 48)
                    Image(systemName: icon)
                        .font(.system(size: 20))
                        .foregroundStyle(isSelected ? .white : DGColor.c500)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(DGFont.cardTitle)
                        .foregroundStyle(DGColor.c900)
                    Text(subtitle)
                        .font(DGFont.subText)
                        .foregroundStyle(DGColor.c500)
                }

                Spacer()

                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 22))
                    .foregroundStyle(isSelected ? DGColor.p500 : DGColor.c200)
            }
            .padding(DGSpacing.md)
            .background(isSelected ? DGColor.p50 : DGColor.c0)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.card))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.card)
                    .stroke(isSelected ? DGColor.p500 : DGColor.c100, lineWidth: isSelected ? 1.5 : 1)
            )
        }
        .buttonStyle(.plain)
    }

    // MARK: - Helpers

    private var isSaving: Bool {
        if case .saving = viewModel.saveState { true } else { false }
    }

    private func proceed(with mode: OnboardingStartMode?) async {
        _ = await viewModel.saveStartMode(mode)
        switch mode {
        case .createTeam:  onCreateTeam()
        case .joinTeam:    onJoinTeam()
        case .mercenary:   onMercenary()
        case .none:        onSkip()
        }
    }
}

#Preview {
    OnboardingStartModeView(
        viewModel: OnboardingViewModel(),
        onCreateTeam: {}, onJoinTeam: {}, onMercenary: {}, onSkip: {}
    )
}
