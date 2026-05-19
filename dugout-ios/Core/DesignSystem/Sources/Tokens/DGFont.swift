//
//  DGFont.swift
//  DugoutDesignSystem
//
//  Pretendard(본문) + JetBrains Mono(코드·상태). 폰트 미등록 시 .custom이
//  시스템 폰트로 graceful fallback (DGFontRegistration 참고).
//

import SwiftUI

public enum DGFont {

    // MARK: - Pretendard 페이스 매핑

    public enum PretendardWeight: String {
        case regular = "Pretendard-Regular"
        case semibold = "Pretendard-SemiBold"
        case bold = "Pretendard-Bold"
        case extrabold = "Pretendard-ExtraBold"
    }

    public static func pretendard(_ weight: PretendardWeight, size: CGFloat) -> Font {
        Font.custom(weight.rawValue, size: size)
    }

    public static func mono(size: CGFloat, weight: Font.Weight = .regular) -> Font {
        let name = weight == .bold || weight == .heavy || weight == .semibold
            ? "JetBrainsMono-Bold"
            : "JetBrainsMono-Regular"
        return Font.custom(name, size: size)
    }

    // MARK: - 스펙 시맨틱 스케일

    public static let screenTitle = pretendard(.bold, size: 26)
    public static let sectionTitle = pretendard(.bold, size: 18)
    public static let cardTitle = pretendard(.bold, size: 16)
    public static let bodyText = pretendard(.regular, size: 14)
    public static let subText = pretendard(.regular, size: 13)
    public static let label = pretendard(.semibold, size: 12)
    public static let badge = pretendard(.semibold, size: 11)

    // MARK: - 기존 케이스 (호출부 호환 — 값만 Pretendard로 교체)

    public static let title = pretendard(.bold, size: 26)
    public static let title2 = pretendard(.bold, size: 22)
    public static let title3 = pretendard(.bold, size: 16)
    public static let headline = pretendard(.semibold, size: 15)
    public static let body = pretendard(.regular, size: 14)
    public static let callout = pretendard(.regular, size: 13)
    public static let footnote = pretendard(.regular, size: 12)
    public static let caption = pretendard(.semibold, size: 11)
}
