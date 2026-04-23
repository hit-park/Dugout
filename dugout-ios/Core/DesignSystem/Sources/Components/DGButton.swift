//
//  DGButton.swift
//  DugoutDesignSystem
//

import SwiftUI

public struct DGButton: View {
    public enum Style {
        case primary
        case secondary
        case tertiary
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
            HStack {
                if isLoading {
                    ProgressView()
                        .progressViewStyle(.circular)
                        .tint(foregroundColor)
                }
                Text(title)
                    .font(DGFont.headline)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, DGSpacing.md)
            .padding(.horizontal, DGSpacing.lg)
            .background(backgroundColor)
            .foregroundStyle(foregroundColor)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.medium))
        }
        .disabled(!isEnabled || isLoading)
        .opacity(isEnabled ? 1 : 0.5)
    }

    private var backgroundColor: Color {
        switch style {
        case .primary: DGColor.primary
        case .secondary: DGColor.secondary
        case .tertiary: DGColor.surface
        }
    }

    private var foregroundColor: Color {
        switch style {
        case .primary, .secondary: .white
        case .tertiary: DGColor.textPrimary
        }
    }
}

#Preview {
    VStack(spacing: DGSpacing.md) {
        DGButton("카카오로 시작하기", style: .primary) {}
        DGButton("네이버로 시작하기", style: .secondary) {}
        DGButton("취소", style: .tertiary) {}
        DGButton("로딩 중", isLoading: true) {}
        DGButton("비활성", isEnabled: false) {}
    }
    .padding()
}
