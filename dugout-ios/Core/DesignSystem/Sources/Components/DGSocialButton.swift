//
//  DGSocialButton.swift
//  DugoutDesignSystem
//
//  OAuth 4종 브랜드색 버튼. height 52, radius 12, 15/700.
//

import SwiftUI

public struct DGSocialButton: View {
    public enum Provider: Sendable {
        case kakao, naver, apple, google

        var title: String {
            switch self {
            case .kakao: "카카오로 시작하기"
            case .naver: "네이버로 시작하기"
            case .apple: "Apple로 시작하기"
            case .google: "Google로 시작하기"
            }
        }

        var background: Color {
            switch self {
            case .kakao: Color(hex: 0xFEE500)
            case .naver: Color(hex: 0x03C75A)
            case .apple: Color(hex: 0x000000)
            case .google: Color(hex: 0xFFFFFF)
            }
        }

        var foreground: Color {
            switch self {
            case .kakao: Color(hex: 0x3C1E1E)
            case .naver, .apple: .white
            case .google: Color(hex: 0x1F1E1B)
            }
        }

        var hasBorder: Bool { self == .google }
    }

    let provider: Provider
    let isLoading: Bool
    let isEnabled: Bool
    let action: () -> Void

    public init(
        provider: Provider,
        isLoading: Bool = false,
        isEnabled: Bool = true,
        action: @escaping () -> Void
    ) {
        self.provider = provider
        self.isLoading = isLoading
        self.isEnabled = isEnabled
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: DGSpacing.sm) {
                if isLoading {
                    ProgressView().progressViewStyle(.circular).tint(provider.foreground)
                }
                Text(provider.title)
                    .font(DGFont.pretendard(.bold, size: 15))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 52)
            .background(provider.background)
            .foregroundStyle(provider.foreground)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.buttonLarge))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.buttonLarge)
                    .stroke(DGColor.c200, lineWidth: provider.hasBorder ? 1 : 0)
            )
        }
        .buttonStyle(DGPressStyle())
        .disabled(!isEnabled || isLoading)
        .opacity(isEnabled ? 1 : 0.5)
    }
}

#Preview {
    VStack(spacing: DGSpacing.md) {
        DGSocialButton(provider: .kakao) {}
        DGSocialButton(provider: .naver) {}
        DGSocialButton(provider: .apple) {}
        DGSocialButton(provider: .google) {}
    }
    .padding()
    .background(DGColor.c100)
}
