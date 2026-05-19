import SwiftUI
import DugoutDesignSystem

public struct OnboardingPositionView: View {
    @Bindable var viewModel: OnboardingViewModel
    let onNext: (User) -> Void

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 12), count: 3)

    public init(viewModel: OnboardingViewModel, onNext: @escaping (User) -> Void) {
        self.viewModel = viewModel
        self.onNext = onNext
    }

    public var body: some View {
        VStack(spacing: 0) {
            DGProgressBar(progress: 2.0 / 3.0)
                .padding(.horizontal, DGSpacing.screenPadding)
                .padding(.top, 8)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 40)

                    Text("주 포지션을 선택해 주세요")
                        .font(DGFont.screenTitle)
                        .foregroundStyle(DGColor.c900)
                    Spacer().frame(height: 8)
                    Text("라인업 추천에 활용돼요")
                        .font(DGFont.bodyText)
                        .foregroundStyle(DGColor.c500)

                    Spacer().frame(height: 32)

                    mainPositionGrid

                    Spacer().frame(height: 32)

                    Text("서브 포지션 (선택)")
                        .font(DGFont.sectionTitle)
                        .foregroundStyle(DGColor.c700)
                    Spacer().frame(height: 12)
                    subPositionChips

                    if case .failed(let msg) = viewModel.saveState {
                        Spacer().frame(height: 12)
                        Text(msg).font(DGFont.subText).foregroundStyle(DGColor.danger)
                    }

                    Spacer().frame(height: 32)
                }
                .padding(.horizontal, DGSpacing.screenPadding)
            }

            Spacer()

            nextButton
                .padding(.horizontal, DGSpacing.screenPadding)
                .padding(.bottom, DGSpacing.lg)
        }
        .background(DGColor.c0.ignoresSafeArea())
        .navigationBarBackButtonHidden(true)
    }

    // MARK: - Subviews

    private var mainPositionGrid: some View {
        VStack(spacing: 12) {
            LazyVGrid(columns: columns, spacing: 12) {
                ForEach(BaseballPosition.gridPositions, id: \.self) { position in
                    positionCard(position)
                }
            }
            positionCard(.designatedHitter)
                .frame(maxWidth: .infinity / 3)
                .frame(maxWidth: UIScreen.main.bounds.width / 3 - DGSpacing.screenPadding)
        }
    }

    private func positionCard(_ position: BaseballPosition) -> some View {
        let isSelected = viewModel.selectedMainPosition == position
        return Button {
            withAnimation(.spring(response: 0.18, dampingFraction: 0.7)) {
                viewModel.selectedMainPosition = isSelected ? nil : position
            }
        } label: {
            VStack(spacing: 4) {
                Text(position.shortName)
                    .font(DGFont.pretendard(.bold, size: 16))
                    .foregroundStyle(isSelected ? .white : DGColor.c700)
                Text(position.displayName)
                    .font(DGFont.subText)
                    .foregroundStyle(isSelected ? .white.opacity(0.85) : DGColor.c400)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 64)
            .background(isSelected ? DGColor.p500 : DGColor.c50)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.card))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.card)
                    .stroke(isSelected ? DGColor.p500 : DGColor.c100, lineWidth: 1)
            )
            .scaleEffect(isSelected ? 1.02 : 1.0)
        }
        .buttonStyle(.plain)
    }

    private var subPositionChips: some View {
        let available = BaseballPosition.allCases.filter { $0 != viewModel.selectedMainPosition }
        return FlowLayout(spacing: 8) {
            ForEach(available, id: \.self) { position in
                DGChip(
                    position.displayName,
                    kind: .selectable(isSelected: viewModel.selectedSubPositions.contains(position))
                ) {
                    viewModel.toggleSubPosition(position)
                }
            }
        }
    }

    private var nextButton: some View {
        DGButton(
            "다음",
            style: .primary,
            isLoading: isSaving,
            isEnabled: viewModel.isPositionValid && !isSaving
        ) {
            Task {
                if let user = await viewModel.savePositionStep() {
                    onNext(user)
                }
            }
        }
    }

    private var isSaving: Bool {
        if case .saving = viewModel.saveState { true } else { false }
    }
}

// MARK: - FlowLayout (chip wrap)

private struct FlowLayout: Layout {
    let spacing: CGFloat

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? 0
        var y: CGFloat = 0
        var x: CGFloat = 0
        var maxRowHeight: CGFloat = 0

        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x + size.width > width, x > 0 {
                y += maxRowHeight + spacing
                x = 0
                maxRowHeight = 0
            }
            x += size.width + spacing
            maxRowHeight = max(maxRowHeight, size.height)
        }
        return CGSize(width: width, height: y + maxRowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var maxRowHeight: CGFloat = 0

        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x + size.width > bounds.maxX, x > bounds.minX {
                y += maxRowHeight + spacing
                x = bounds.minX
                maxRowHeight = 0
            }
            view.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            maxRowHeight = max(maxRowHeight, size.height)
        }
    }
}

#Preview {
    OnboardingPositionView(viewModel: OnboardingViewModel(), onNext: { _ in })
}
