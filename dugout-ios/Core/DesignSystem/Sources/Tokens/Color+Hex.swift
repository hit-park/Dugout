//
//  Color+Hex.swift
//  DugoutDesignSystem
//

import SwiftUI

extension Color {
    /// `0xRRGGBB` 정수로 라이트 고정 색 생성. 디자인 토큰 정의 전용.
    init(hex: UInt32) {
        let r = Double((hex >> 16) & 0xFF) / 255
        let g = Double((hex >> 8) & 0xFF) / 255
        let b = Double(hex & 0xFF) / 255
        self.init(.sRGB, red: r, green: g, blue: b, opacity: 1)
    }
}
