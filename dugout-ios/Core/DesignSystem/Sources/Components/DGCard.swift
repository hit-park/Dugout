//
//  DGCard.swift
//  DugoutDesignSystem
//
//  스펙: radius 14, bg c0, padding 14, border c200 1px.
//  시그니처 호환 — 기존 호출부 무변경 (옵션 기본값).
//

import SwiftUI

public struct DGCard<Content: View>: View {
    let padding: CGFloat
    let background: Color
    let content: () -> Content

    public init(
        padding: CGFloat = DGSpacing.lg,
        background: Color = DGColor.c0,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.padding = padding
        self.background = background
        self.content = content
    }

    public var body: some View {
        content()
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(padding)
            .background(background)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.card))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.card)
                    .stroke(DGColor.c200, lineWidth: 1)
            )
    }
}

#Preview {
    DGCard {
        VStack(alignment: .leading, spacing: DGSpacing.sm) {
            Text("두갓FC").dgText(.cardTitle)
            Text("서울 강남구 · 4부")
                .dgText(.subText)
                .foregroundStyle(DGColor.c500)
        }
    }
    .padding()
    .background(DGColor.c100)
}
