import SwiftUI
import DugoutDesignSystem

public struct OnboardingNicknameView: View {
    @Bindable var viewModel: OnboardingViewModel
    let onNext: (User) -> Void

    public init(viewModel: OnboardingViewModel, onNext: @escaping (User) -> Void) {
        self.viewModel = viewModel
        self.onNext = onNext
    }

    public var body: some View {
        VStack(spacing: 0) {
            DGProgressBar(progress: 1.0 / 3.0)
                .padding(.horizontal, DGSpacing.screenPadding)
                .padding(.top, 8)

            ScrollView {
                VStack(alignment: .leading, spacing: 0) {
                    Spacer().frame(height: 40)

                    Text("닉네임을 알려주세요")
                        .font(DGFont.screenTitle)
                        .foregroundStyle(DGColor.c900)
                    Spacer().frame(height: 8)
                    Text("팀원들에게 표시되는 이름이에요")
                        .font(DGFont.bodyText)
                        .foregroundStyle(DGColor.c500)

                    Spacer().frame(height: 32)

                    DGTextField(
                        label: "닉네임",
                        placeholder: "2~10자, 한글/영문/숫자",
                        text: $viewModel.nickname,
                        status: nicknameFieldStatus
                    )
                    .onChange(of: viewModel.nickname) { _, value in
                        viewModel.onNicknameChanged(value)
                    }

                    Spacer().frame(height: 20)

                    jerseyNumberField

                    if case .failed(let msg) = viewModel.saveState {
                        Spacer().frame(height: 12)
                        Text(msg)
                            .font(DGFont.subText)
                            .foregroundStyle(DGColor.danger)
                    }
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

    private var jerseyNumberField: some View {
        VStack(alignment: .leading, spacing: DGSpacing.sm) {
            Text("등번호 (선택)")
                .font(DGFont.label)
                .foregroundStyle(DGColor.c700)
            TextField("0 ~ 99", text: $viewModel.jerseyNumber)
                .font(DGFont.bodyText)
                .keyboardType(.numberPad)
                .padding(.horizontal, DGSpacing.md)
                .frame(height: 48)
                .background(DGColor.c0)
                .clipShape(RoundedRectangle(cornerRadius: DGRadius.field))
                .overlay(
                    RoundedRectangle(cornerRadius: DGRadius.field)
                        .stroke(DGColor.c200, lineWidth: 1)
                )
        }
    }

    private var nextButton: some View {
        DGButton(
            "다음",
            style: .primary,
            isLoading: isSaving,
            isEnabled: viewModel.isNicknameValid && !isSaving
        ) {
            Task {
                if let user = await viewModel.saveNicknameStep() {
                    onNext(user)
                }
            }
        }
    }

    // MARK: - Helpers

    private var nicknameFieldStatus: DGTextField.Status {
        switch viewModel.nicknameStatus {
        case .idle:                return .normal
        case .checking:            return .normal
        case .available:           return .valid("사용 가능한 닉네임이에요")
        case .unavailable(let m):  return .error(m)
        case .error(let m):        return .error(m)
        }
    }

    private var isSaving: Bool {
        if case .saving = viewModel.saveState { true } else { false }
    }
}

#Preview {
    OnboardingNicknameView(viewModel: OnboardingViewModel(), onNext: { _ in })
}
