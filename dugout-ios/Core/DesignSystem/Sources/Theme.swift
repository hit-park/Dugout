//
//  Theme.swift
//  DugoutDesignSystem
//

import SwiftUI

/// Dugout 디자인 시스템 상수.
public enum DGColor {
    public static let primary = Color(red: 34 / 255, green: 94 / 255, blue: 58 / 255)    // 야구장 잔디 느낌
    public static let secondary = Color(red: 194 / 255, green: 75 / 255, blue: 42 / 255) // 점토색 베이스
    public static let background = Color(.systemGroupedBackground)
    public static let surface = Color(.systemBackground)
    public static let textPrimary = Color(.label)
    public static let textSecondary = Color(.secondaryLabel)
    public static let border = Color(.separator)
    public static let success = Color.green
    public static let warning = Color.orange
    public static let danger = Color.red
}

public enum DGSpacing {
    public static let xs: CGFloat = 4
    public static let sm: CGFloat = 8
    public static let md: CGFloat = 12
    public static let lg: CGFloat = 16
    public static let xl: CGFloat = 24
    public static let xxl: CGFloat = 32
}

public enum DGRadius {
    public static let small: CGFloat = 8
    public static let medium: CGFloat = 12
    public static let large: CGFloat = 16
}

public enum DGFont {
    public static let title = Font.system(.title, design: .default, weight: .bold)
    public static let title2 = Font.system(.title2, design: .default, weight: .bold)
    public static let title3 = Font.system(.title3, design: .default, weight: .semibold)
    public static let headline = Font.system(.headline)
    public static let body = Font.system(.body)
    public static let callout = Font.system(.callout)
    public static let footnote = Font.system(.footnote)
    public static let caption = Font.system(.caption)
}
