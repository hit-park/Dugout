//
//  DGChip.swift
//  DugoutDesignSystem
//
//  단일/다중 선택 pill. 팀전환·요일·서브포지션·추천닉네임 공용.
//

import SwiftUI

public struct DGChip: View {
    public enum Kind: Sendable {
        case selectable(isSelected: Bool)
        case dashed   // "+ 팀 추가"
    }

    let title: String
    let kind: Kind
    let action: () -> Void

    public init(_ title: String, kind: Kind, action: @escaping () -> Void) {
        self.title = title
        self.kind = kind
        self.action = action
    }

    public var body: some View {
        Button(action: action) {
            Text(title)
                .font(DGFont.label)
                .foregroundStyle(foreground)
                .padding(.horizontal, DGSpacing.md)
                .padding(.vertical, DGSpacing.sm)
                .background(background)
                .overlay(
                    Capsule().strokeBorder(
                        borderColor,
                        style: StrokeStyle(lineWidth: 1, dash: isDashed ? [4] : [])
                    )
                )
                .clipShape(Capsule())
        }
        .buttonStyle(DGPressStyle())
    }

    private var isSelected: Bool {
        if case .selectable(let s) = kind { return s }
        return false
    }

    private var isDashed: Bool {
        if case .dashed = kind { return true }
        return false
    }

    private var background: Color {
        isSelected ? DGColor.p500 : DGColor.c0
    }

    private var foreground: Color {
        if isDashed { return DGColor.c400 }
        return isSelected ? .white : DGColor.c700
    }

    private var borderColor: Color {
        isSelected ? .clear : DGColor.c200
    }
}

#Preview {
    HStack {
        DGChip("두갓FC", kind: .selectable(isSelected: true)) {}
        DGChip("블루웨이브", kind: .selectable(isSelected: false)) {}
        DGChip("+ 팀 추가", kind: .dashed) {}
    }
    .padding()
    .background(DGColor.c100)
}
