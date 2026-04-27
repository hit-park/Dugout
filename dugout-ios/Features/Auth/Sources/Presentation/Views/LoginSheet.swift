//
//  LoginSheet.swift
//  DugoutAuthFeature
//

import SwiftUI
import DugoutDesignSystem

/// 인증이 필요한 액션을 시도할 때 표시되는 공용 로그인 시트.
/// dev-login만 지원 (Phase 1). OAuth는 후속.
public struct LoginSheet: View {
    @Bindable var authViewModel: AuthViewModel
    @Environment(\.dismiss) private var dismiss

    @State private var nickname: String = ""

    public init(authViewModel: AuthViewModel) {
        self.authViewModel = authViewModel
    }

    public var body: some View {
        NavigationStack {
            VStack(spacing: DGSpacing.lg) {
                Image(systemName: "person.crop.circle.badge.plus")
                    .font(.system(size: 60))
                    .foregroundStyle(DGColor.primary)
                    .padding(.top, DGSpacing.xl)

                Text("로그인이 필요합니다")
                    .font(DGFont.title2)

                Text("개발 모드에서는 닉네임만 입력하면 로그인됩니다.")
                    .font(DGFont.footnote)
                    .foregroundStyle(DGColor.textSecondary)
                    .multilineTextAlignment(.center)

                TextField("닉네임", text: $nickname)
                    .textFieldStyle(.roundedBorder)
                    .padding(.horizontal, DGSpacing.lg)
                    .disabled(isLoading)

                if case .failed(let message) = authViewModel.state {
                    Text(message)
                        .font(DGFont.footnote)
                        .foregroundStyle(DGColor.warning)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, DGSpacing.lg)
                }

                DGButton(isLoading ? "로그인 중..." : "로그인") {
                    Task {
                        await authViewModel.devLogin(nickname: nickname.trimmingCharacters(in: .whitespaces))
                    }
                }
                .disabled(nickname.trimmingCharacters(in: .whitespaces).isEmpty || isLoading)
                .padding(.horizontal, DGSpacing.lg)

                Spacer()
            }
            .background(DGColor.background)
            .navigationTitle("로그인")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("닫기") { dismiss() }
                }
            }
            .onChange(of: authViewModel.isAuthenticated) { _, isAuth in
                if isAuth { dismiss() }
            }
        }
    }

    private var isLoading: Bool {
        if case .loading = authViewModel.state { return true }
        return false
    }
}
