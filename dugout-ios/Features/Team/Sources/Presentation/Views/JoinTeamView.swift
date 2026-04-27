//
//  JoinTeamView.swift
//  DugoutTeamFeature
//

import SwiftUI
import DugoutDesignSystem

public struct JoinTeamView: View {
    @State private var viewModel: JoinTeamViewModel
    private let onCompleted: @MainActor () async -> Void

    @Environment(\.dismiss) private var dismiss

    public init(
        viewModel: JoinTeamViewModel = JoinTeamViewModel(),
        onCompleted: @escaping @MainActor () async -> Void
    ) {
        _viewModel = State(wrappedValue: viewModel)
        self.onCompleted = onCompleted
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: DGSpacing.lg) {
                Image(systemName: "ticket")
                    .font(.system(size: 60))
                    .foregroundStyle(DGColor.primary)
                    .padding(.top, DGSpacing.xl)

                Text("초대 코드 입력")
                    .font(DGFont.title2)

                Text("팀 주장이 공유한 초대 코드를 입력하세요.")
                    .font(DGFont.footnote)
                    .foregroundStyle(DGColor.textSecondary)
                    .multilineTextAlignment(.center)

                TextField("초대 코드", text: $viewModel.inviteCode)
                    .textFieldStyle(.roundedBorder)
                    .textInputAutocapitalization(.characters)
                    .autocorrectionDisabled()
                    .padding(.horizontal, DGSpacing.lg)
                    .disabled(isSubmitting)

                if case .failed(let message) = viewModel.state {
                    Text(message)
                        .font(DGFont.footnote)
                        .foregroundStyle(DGColor.warning)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, DGSpacing.lg)
                }

                DGButton(isSubmitting ? "가입 중..." : "가입하기") {
                    Task {
                        await viewModel.submit()
                        if case .success = viewModel.state {
                            await onCompleted()
                            dismiss()
                        }
                    }
                }
                .disabled(!viewModel.canSubmit)
                .padding(.horizontal, DGSpacing.lg)

                Spacer()
            }
            .background(DGColor.background)
            .navigationTitle("팀 가입")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
            .interactiveDismissDisabled(isSubmitting)
        }
    }

    private var isSubmitting: Bool {
        if case .submitting = viewModel.state { return true }
        return false
    }
}
