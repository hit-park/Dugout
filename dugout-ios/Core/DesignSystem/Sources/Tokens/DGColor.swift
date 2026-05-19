//
//  DGColor.swift
//  DugoutDesignSystem
//
//  단일 토큰 소스: docs/design/Dugout Design Spec.html
//  라이트 고정 팔레트 (다크모드 비범위 — 앱은 .preferredColorScheme(.light) 강제).
//

import SwiftUI

public enum DGColor {

    // MARK: - Primary · Green

    public static let p50 = Color(hex: 0xE8F5EE)
    public static let p100 = Color(hex: 0xC8E6D2)
    public static let p200 = Color(hex: 0x9FD3B0)
    public static let p300 = Color(hex: 0x73BE8B)
    public static let p500 = Color(hex: 0x2D8A4E)
    public static let p600 = Color(hex: 0x236F40)
    public static let p700 = Color(hex: 0x1A5532)
    public static let p900 = Color(hex: 0x0B2917)

    // MARK: - Secondary · Amber

    public static let s50 = Color(hex: 0xFDF1E8)
    public static let s100 = Color(hex: 0xFADBC4)
    public static let s300 = Color(hex: 0xF0B083)
    public static let s500 = Color(hex: 0xD27640)
    public static let s700 = Color(hex: 0x974D24)

    // MARK: - Neutral · Cream

    public static let c0 = Color(hex: 0xFFFFFF)
    public static let c50 = Color(hex: 0xFAF7F4)
    public static let c100 = Color(hex: 0xF1ECE5)
    public static let c200 = Color(hex: 0xE0DAD0)
    public static let c300 = Color(hex: 0xC2BBAF)
    public static let c400 = Color(hex: 0x9A9389)
    public static let c500 = Color(hex: 0x73706A)
    public static let c700 = Color(hex: 0x46443F)
    public static let c900 = Color(hex: 0x1F1E1B)

    // MARK: - Semantic

    public static let success = Color(hex: 0x16A34A)
    public static let warning = Color(hex: 0xD97706)
    public static let danger = Color(hex: 0xDC2626)
    public static let info = Color(hex: 0x2563EB)

    // MARK: - 기존 시맨틱 별칭 (호출부 호환 — 케이스명 보존, 값만 스펙으로 교체)

    public static let primary = p500
    public static let secondary = s500
    public static let background = c100
    public static let surface = c0
    public static let textPrimary = c900
    public static let textSecondary = c500
    public static let border = c200
}
