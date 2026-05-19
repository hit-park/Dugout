import SwiftUI
import DugoutDesignSystem

public struct AuthView: View {
    @Bindable var viewModel: AuthViewModel
    let onLoginSuccess: (User) -> Void
    let onBrowseWithoutLogin: () -> Void

    @State private var devNicknameInput = ""
    @State private var showDevInput = false

    public init(
        viewModel: AuthViewModel,
        onLoginSuccess: @escaping (User) -> Void,
        onBrowseWithoutLogin: @escaping () -> Void
    ) {
        self.viewModel = viewModel
        self.onLoginSuccess = onLoginSuccess
        self.onBrowseWithoutLogin = onBrowseWithoutLogin
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                Spacer().frame(height: 80)
                logo
                Spacer().frame(height: 48)
                tagline
                Spacer().frame(height: 56)
                socialButtons
                Spacer().frame(height: 20)
                browseButton
                Spacer().frame(height: 24)

                #if DEBUG
                devModeButton
                Spacer().frame(height: 16)
                #endif
            }
            .padding(.horizontal, DGSpacing.screenPadding)
        }
        .background(DGColor.c0.ignoresSafeArea())
        .onChange(of: viewModel.currentUser) { _, user in
            if let user { onLoginSuccess(user) }
        }
        .sheet(isPresented: $showDevInput) { devInputSheet }
    }

    // MARK: - Subviews

    private var logo: some View {
        VStack(spacing: 8) {
            Image(systemName: "baseball.fill")
                .font(.system(size: 64, weight: .bold))
                .foregroundStyle(DGColor.p500)
            Text("Dugout")
                .font(DGFont.pretendard(.extrabold, size: 36))
                .foregroundStyle(DGColor.p500)
                .tracking(-0.8)
        }
    }

    private var tagline: some View {
        VStack(spacing: 6) {
            Text("사회인 야구팀 운영,\n더그아웃 하나로.")
                .font(DGFont.screenTitle)
                .multilineTextAlignment(.center)
                .foregroundStyle(DGColor.c900)
            Text("출석 · 라인업 · 회비 · 매칭을 AI가 도와드려요")
                .font(DGFont.bodyText)
                .foregroundStyle(DGColor.c500)
                .multilineTextAlignment(.center)
        }
    }

    private var socialButtons: some View {
        VStack(spacing: DGSpacing.sm) {
            if case .failed(let msg) = viewModel.state {
                Text(msg)
                    .font(DGFont.subText)
                    .foregroundStyle(DGColor.danger)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }

            DGSocialButton(
                provider: .kakao,
                isLoading: isLoadingFor(.kakao),
                isEnabled: !isLoading
            ) {
                Task { await viewModel.login(provider: .kakao, accessToken: "oauth-placeholder") }
            }
            DGSocialButton(
                provider: .naver,
                isLoading: isLoadingFor(.naver),
                isEnabled: !isLoading
            ) {
                Task { await viewModel.login(provider: .naver, accessToken: "oauth-placeholder") }
            }
            DGSocialButton(
                provider: .apple,
                isLoading: isLoadingFor(.apple),
                isEnabled: !isLoading
            ) {
                Task { await viewModel.login(provider: .apple, accessToken: "oauth-placeholder") }
            }
            DGSocialButton(
                provider: .google,
                isLoading: isLoadingFor(.google),
                isEnabled: !isLoading
            ) {
                Task { await viewModel.login(provider: .google, accessToken: "oauth-placeholder") }
            }
        }
    }

    private var browseButton: some View {
        Button(action: onBrowseWithoutLogin) {
            Text("둘러보기")
                .font(DGFont.label)
                .foregroundStyle(DGColor.c500)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .overlay(
                    RoundedRectangle(cornerRadius: DGRadius.button)
                        .stroke(DGColor.c200, lineWidth: 1)
                )
        }
        .buttonStyle(DGPressStyle())
    }

    #if DEBUG
    private var devModeButton: some View {
        Button { showDevInput = true } label: {
            Text("개발자 모드")
                .font(DGFont.subText)
                .foregroundStyle(DGColor.c400)
        }
    }

    private var devInputSheet: some View {
        NavigationStack {
            Form {
                Section("닉네임") {
                    TextField("예: 테스트주장", text: $devNicknameInput)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                }
                Section {
                    Button("개발 로그인") {
                        let nick = devNicknameInput.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !nick.isEmpty else { return }
                        showDevInput = false
                        Task { await viewModel.devLogin(nickname: nick) }
                    }
                    .disabled(devNicknameInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                } footer: {
                    Text("백엔드 로컬 프로필 전용. 같은 닉네임으로 재로그인하면 기존 계정으로 복귀.")
                        .font(DGFont.caption)
                }
            }
            .navigationTitle("개발 로그인")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("닫기") { showDevInput = false }
                }
            }
        }
    }
    #endif

    // MARK: - Helpers

    private var isLoading: Bool {
        if case .loading = viewModel.state { true } else { false }
    }

    private func isLoadingFor(_ provider: AuthProvider) -> Bool {
        // OAuth SDK 미연동 상태에서는 어떤 버튼을 눌렀는지 추적 불가 → 전체 loading 공유
        isLoading
    }
}

#Preview {
    AuthView(viewModel: AuthViewModel(), onLoginSuccess: { _ in }, onBrowseWithoutLogin: {})
}
