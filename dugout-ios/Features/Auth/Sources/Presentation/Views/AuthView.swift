//
//  AuthView.swift
//  DugoutAuthFeature
//

import SwiftUI
import DugoutDesignSystem

public struct AuthView: View {
    @Bindable var viewModel: AuthViewModel
    @State private var devNicknameInput: String = ""
    @State private var showDevInput = false

    public init(viewModel: AuthViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        ScrollView {
            VStack(spacing: DGSpacing.xl) {
                Spacer().frame(height: DGSpacing.xxl)

                logo

                VStack(spacing: DGSpacing.sm) {
                    Text("사회인 야구팀 운영,\n더그아웃 하나로.")
                        .font(DGFont.title)
                        .multilineTextAlignment(.center)

                    Text("출석 · 라인업 · 회비 · 매칭을 AI가 도와드려요")
                        .font(DGFont.callout)
                        .foregroundStyle(DGColor.textSecondary)
                        .multilineTextAlignment(.center)
                }

                Spacer().frame(height: DGSpacing.xl)

                if case .loading = viewModel.state {
                    ProgressView().padding()
                }

                if case let .failed(message) = viewModel.state {
                    Text(message)
                        .font(DGFont.footnote)
                        .foregroundStyle(DGColor.danger)
                        .padding(.horizontal, DGSpacing.lg)
                }

                loginButtons

                developerModeButton
            }
            .padding(.horizontal, DGSpacing.xl)
        }
        .background(DGColor.background.ignoresSafeArea())
        .sheet(isPresented: $showDevInput) {
            developerInputSheet
        }
    }

    private var logo: some View {
        VStack {
            Image(systemName: "baseball.fill")
                .font(.system(size: 72))
                .foregroundStyle(DGColor.primary)
            Text("Dugout")
                .font(.system(size: 40, weight: .heavy, design: .rounded))
                .foregroundStyle(DGColor.primary)
        }
    }

    private var loginButtons: some View {
        VStack(spacing: DGSpacing.md) {
            ForEach(AuthProvider.oauthProviders, id: \.self) { provider in
                DGButton(
                    buttonLabel(for: provider),
                    style: buttonStyle(for: provider),
                    isLoading: isLoading,
                    isEnabled: false // OAuth SDK 연동 전까지 비활성 (개발자 모드 사용)
                ) {}
            }
        }
    }

    private var developerModeButton: some View {
        Button {
            showDevInput = true
        } label: {
            Text("개발자 모드 (닉네임으로 로그인)")
                .font(DGFont.caption)
                .foregroundStyle(DGColor.textSecondary)
        }
        .padding(.top, DGSpacing.md)
    }

    private var developerInputSheet: some View {
        NavigationStack {
            Form {
                Section("닉네임") {
                    TextField("예: 테스트주장", text: $devNicknameInput)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                }

                Section {
                    Button("개발 로그인") {
                        let nickname = devNicknameInput.trimmingCharacters(in: .whitespacesAndNewlines)
                        guard !nickname.isEmpty else { return }
                        showDevInput = false
                        Task {
                            await viewModel.devLogin(nickname: nickname)
                        }
                    }
                    .disabled(devNicknameInput.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                } footer: {
                    Text("백엔드 로컬 프로필에서만 동작합니다. 카카오 SDK 연동 전 앱 플로우를 검증하는 용도입니다. 같은 닉네임으로 재로그인하면 기존 계정으로 복귀합니다.")
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

    private var isLoading: Bool {
        if case .loading = viewModel.state { true } else { false }
    }

    private func buttonLabel(for provider: AuthProvider) -> String {
        switch provider {
        case .kakao: "카카오로 시작하기"
        case .naver: "네이버로 시작하기"
        case .google: "Google로 시작하기"
        case .apple: "Apple로 시작하기"
        case .dev: "개발 로그인"
        }
    }

    private func buttonStyle(for provider: AuthProvider) -> DGButton.Style {
        switch provider {
        case .kakao: .primary
        default: .secondary
        }
    }
}

#Preview {
    AuthView(viewModel: AuthViewModel())
}
