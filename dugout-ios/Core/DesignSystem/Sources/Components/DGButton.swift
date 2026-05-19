//
//  DGButton.swift
//  DugoutDesignSystem
//
//  스펙: height 50, radius 10, 15/600, press scale 0.97.
//  시그니처 호환 — 기존 호출부(.primary/.secondary/.tertiary) 무변경.
//

import SwiftUI

public struct DGButton: View {
    public enum Style {
        case primary
        case secondary
        case tertiary
        case destructive
    }

    let title: String
    let style: Style
    let isLoading: Bool
    let isEnabled: Bool
    let action: () -> Void

    public init(
        _ title: String,
        style: Style = .primary,
        isLoading: Bool = false,
        isEnabled: Bool = true,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.style = style
        self.isLoading = isLoading
        self.isEnabled = isEnabled
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            HStack(spacing: DGSpacing.sm) {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(foreground)
                }
                Text(title)
                    .font(DGFont.pretendard(.semibold, size: 15))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 50)
            .background(background)
            .foregroundStyle(foreground)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.button)
                    .stroke(borderColor, lineWidth: borderWidth)
            )
        }
        .buttonStyle(DGPressStyle())
        .disabled(!isEnabled || isLoading)
        .opacity(isEnabled ? 1 : 0.5)
    }

    private var background: Color {
        switch style {
        case .primary: DGColor.p500
        case .secondary, .tertiary: DGColor.c0
        case .destructive: DGColor.danger
        }
    }

    private var foreground: Color {
        switch style {
        case .primary, .destructive: .white
        case .secondary: DGColor.p500
        case .tertiary: DGColor.c700
        }
    }

    private var borderColor: Color {
        switch style {
        case .secondary: DGColor.p500
        case .tertiary: DGColor.c200
        default: .clear
        }
    }

    private var borderWidth: CGFloat {
        switch style {
        case .secondary, .tertiary: 1
        default: 0
        }
    }
}

/// 눌림 시 scale(0.97) + 120ms 오버슛.
public struct DGPressStyle: ButtonStyle {
    public init() {}
    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? DGMotion.pressedScale : 1)
            .animation(DGMotion.buttonTap, value: configuration.isPressed)
    }
}

#Preview {
    VStack(spacing: DGSpacing.md) {
        DGButton("팀 만들기", style: .primary) {}
        DGButton("초대 코드로 참여", style: .secondary) {}
        DGButton("나중에 결정하기", style: .tertiary) {}
        DGButton("추방", style: .destructive) {}
        DGButton("로딩 중", isLoading: true) {}
        DGButton("비활성", isEnabled: false) {}
    }
    .padding()
    .background(DGColor.c100)
}
