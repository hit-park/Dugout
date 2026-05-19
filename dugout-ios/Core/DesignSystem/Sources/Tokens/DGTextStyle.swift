//
//  DGTextStyle.swift
//  DugoutDesignSystem
//
//  Font + letter-spacing 묶음. SwiftUI Font 만으로 tracking 표현 불가하여
//  ViewModifier 로 캡슐화. 신규 화면은 .dgText(_:) 사용 권장.
//

import SwiftUI

public enum DGTextStyle {
    case screenTitle
    case sectionTitle
    case cardTitle
    case bodyText
    case subText
    case label
    case badge

    var font: Font {
        switch self {
        case .screenTitle: DGFont.screenTitle
        case .sectionTitle: DGFont.sectionTitle
        case .cardTitle: DGFont.cardTitle
        case .bodyText: DGFont.bodyText
        case .subText: DGFont.subText
        case .label: DGFont.label
        case .badge: DGFont.badge
        }
    }

    var tracking: CGFloat {
        switch self {
        case .screenTitle: -0.5
        case .sectionTitle: -0.3
        case .cardTitle: -0.2
        case .bodyText, .subText, .label, .badge: 0
        }
    }
}

public extension View {
    func dgText(_ style: DGTextStyle) -> some View {
        self.font(style.font).tracking(style.tracking)
    }
}
