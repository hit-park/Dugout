//
//  DGSpacing.swift
//  DugoutDesignSystem
//
//  4px 그리드.
//

import CoreGraphics

public enum DGSpacing {
    public static let xs: CGFloat = 4
    public static let sm: CGFloat = 8
    public static let md: CGFloat = 12
    public static let lg: CGFloat = 16
    public static let xl: CGFloat = 24
    public static let xxl: CGFloat = 32

    /// 화면 수평 패딩 (스펙 16px).
    public static let screenPadding: CGFloat = 16
}

public enum DGRadius {
    public static let small: CGFloat = 8
    public static let medium: CGFloat = 12
    public static let large: CGFloat = 16

    public static let card: CGFloat = 14
    public static let button: CGFloat = 10
    public static let buttonLarge: CGFloat = 12
    public static let field: CGFloat = 10
    public static let pill: CGFloat = 9999
}
