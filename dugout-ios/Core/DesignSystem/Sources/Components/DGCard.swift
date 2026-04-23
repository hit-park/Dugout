//
//  DGCard.swift
//  DugoutDesignSystem
//

import SwiftUI

public struct DGCard<Content: View>: View {
    let content: () -> Content

    public init(@ViewBuilder content: @escaping () -> Content) {
        self.content = content
    }

    public var body: some View {
        content()
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(DGSpacing.lg)
            .background(DGColor.surface)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.medium))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.medium)
                    .stroke(DGColor.border, lineWidth: 0.5)
            )
    }
}

#Preview {
    DGCard {
        VStack(alignment: .leading, spacing: DGSpacing.sm) {
            Text("두갓FC")
                .font(DGFont.title3)
            Text("서울 강남구 · 4부")
                .font(DGFont.footnote)
                .foregroundStyle(DGColor.textSecondary)
        }
    }
    .padding()
    .background(DGColor.background)
}
