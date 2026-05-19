//
//  DGMotion.swift
//  DugoutDesignSystem
//

import SwiftUI

public enum DGMotion {
    /// 버튼 탭: scale(0.97) + 120ms 오버슛.
    public static let buttonTap = Animation.timingCurve(0.34, 1.56, 0.64, 1, duration: 0.12)
    /// 포지션 카드 탭: spring 180ms 오버슛.
    public static let positionCard = Animation.spring(response: 0.18, dampingFraction: 0.6)
    /// 화면 전환: slide 420ms.
    public static let screenTransition = Animation.easeInOut(duration: 0.42)
    /// 탭 전환: cross-fade 200ms.
    public static let tabCrossfade = Animation.easeInOut(duration: 0.20)
    /// 모달/바텀시트: slide-up 320ms.
    public static let sheetSlide = Animation.easeOut(duration: 0.32)
    /// 팀 칩 전환 skeleton: 280ms.
    public static let skeletonSwap = Animation.easeInOut(duration: 0.28)

    public static let pressedScale: CGFloat = 0.97
}
