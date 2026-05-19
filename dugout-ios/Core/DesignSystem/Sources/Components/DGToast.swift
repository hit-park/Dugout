//
//  DGToast.swift
//  DugoutDesignSystem
//
//  4종 토스트. 하단 중앙, 3초 후 fade-out. .dgToast(item:) 모디파이어.
//

import SwiftUI

public struct DGToastItem: Identifiable, Equatable, Sendable {
    public enum Kind: Sendable {
        case success, warning, danger, info

        var color: Color {
            switch self {
            case .success: DGColor.success
            case .warning: DGColor.warning
            case .danger: DGColor.danger
            case .info: DGColor.info
            }
        }
    }

    public let id = UUID()
    public let message: String
    public let kind: Kind

    public init(message: String, kind: Kind = .info) {
        self.message = message
        self.kind = kind
    }
}

private struct DGToastModifier: ViewModifier {
    @Binding var item: DGToastItem?

    func body(content: Content) -> some View {
        content.overlay(alignment: .bottom) {
            if let item {
                Text(item.message)
                    .font(DGFont.label)
                    .foregroundStyle(.white)
                    .padding(.horizontal, DGSpacing.lg)
                    .padding(.vertical, DGSpacing.sm + 2)
                    .background(item.kind.color)
                    .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
                    .padding(.bottom, DGSpacing.xxl)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                    .task(id: item.id) {
                        try? await Task.sleep(for: .seconds(3))
                        withAnimation { self.item = nil }
                    }
            }
        }
        .animation(.easeInOut(duration: 0.25), value: item)
    }
}

public extension View {
    func dgToast(item: Binding<DGToastItem?>) -> some View {
        modifier(DGToastModifier(item: item))
    }
}

#Preview {
    DGColor.c100
        .dgToast(item: .constant(DGToastItem(message: "저장 완료", kind: .success)))
}
