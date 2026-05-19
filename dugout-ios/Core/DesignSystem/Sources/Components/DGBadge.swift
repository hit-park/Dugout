//
//  DGBadge.swift
//  DugoutDesignSystem
//
//  pill 배지. 역할/포지션/D-N/상태.
//

import SwiftUI

public struct DGBadge: View {
    public enum Variant: Sendable {
        case captain      // 주장
        case manager      // 매니저
        case accountant   // 회계
        case member       // 일반
        case position     // 포지션
        case dDay         // D-N (mono)
        case neutral

        var background: Color {
            switch self {
            case .captain: DGColor.s100
            case .manager: DGColor.p50
            case .accountant: DGColor.warning.opacity(0.14)
            case .member: DGColor.c100
            case .position: DGColor.p100
            case .dDay: DGColor.p500
            case .neutral: DGColor.c100
            }
        }

        var foreground: Color {
            switch self {
            case .captain: DGColor.s700
            case .manager: DGColor.p600
            case .accountant: DGColor.warning
            case .member: DGColor.c700
            case .position: DGColor.p600
            case .dDay: .white
            case .neutral: DGColor.c700
            }
        }
    }

    let text: String
    let variant: Variant

    public init(_ text: String, variant: Variant = .neutral) {
        self.text = text
        self.variant = variant
    }

    public var body: some View {
        Text(text)
            .font(variant == .dDay ? DGFont.mono(size: 11, weight: .bold) : DGFont.badge)
            .foregroundStyle(variant.foreground)
            .padding(.horizontal, DGSpacing.sm)
            .padding(.vertical, DGSpacing.xs)
            .background(variant.background)
            .clipShape(Capsule())
    }
}

#Preview {
    HStack {
        DGBadge("주장", variant: .captain)
        DGBadge("매니저", variant: .manager)
        DGBadge("회계", variant: .accountant)
        DGBadge("일반", variant: .member)
        DGBadge("투수", variant: .position)
        DGBadge("D-2", variant: .dDay)
    }
    .padding()
    .background(DGColor.c100)
}
