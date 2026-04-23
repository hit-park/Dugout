//
//  AuthView.swift
//  DugoutAuthFeature
//

import SwiftUI
import DugoutDesignSystem

public struct AuthView: View {
    @Bindable var viewModel: AuthViewModel
    @State private var devTokenInput: String = ""
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
            ForEach([AuthProvider.kakao, .naver, .google, .apple], id: \.self) { provider in
                DGButton(
                    buttonLabel(for: provider),
                    style: buttonStyle(for: provider),
                    isLoading: isLoading,
                    isEnabled: !isLoading
                ) {
                    showDevInput = true
                }
            }
        }
    }

    private var developerModeButton: some View {
        Button {
            showDevInput = true
        } label: {
            Text("개발자 모드 (토큰 직접 입력)")
                .font(DGFont.caption)
                .foregroundStyle(DGColor.textSecondary)
        }
        .padding(.top, DGSpacing.md)
    }

    private var developerInputSheet: some View {
        NavigationStack {
            Form {
                Section("OAuth Access Token") {
                    TextField("카카오/네이버/구글/애플 토큰", text: $devTokenInput, axis: .vertical)
                        .lineLimit(3...6)
                        .font(DGFont.caption)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                }

                Section {
                    ForEach([AuthProvider.kakao, .naver, .google, .apple], id: \.self) { provider in
                        Button("\(provider.rawValue)로 로그인 시도") {
                            let token = devTokenInput.trimmingCharacters(in: .whitespacesAndNewlines)
                            guard !token.isEmpty else { return }
                            showDevInput = false
                            Task {
                                await viewModel.login(provider: provider, accessToken: token)
                            }
                        }
                    }
                } footer: {
                    Text("실제 OAuth SDK 연동 전 테스트용입니다. 각 제공자가 발급한 실제 토큰이어야 백엔드 검증을 통과합니다.")
                        .font(DGFont.caption)
                }
            }
            .navigationTitle("개발자 로그인")
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
