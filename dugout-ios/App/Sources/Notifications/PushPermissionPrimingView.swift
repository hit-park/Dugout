//
//  PushPermissionPrimingView.swift
//  Dugout
//

import DugoutDesignSystem
import SwiftUI

struct PushPermissionPrimingView: View {
    let onAllow: () async -> Void
    let onSkip: () -> Void

    var body: some View {
        VStack(spacing: DGSpacing.xl) {
            Spacer()
            Image(systemName: "bell.badge.fill")
                .font(.system(size: 72))
                .foregroundStyle(DGColor.p500)
            Text("알림을 받아볼까요?")
                .font(DGFont.pretendard(.bold, size: 24))
                .foregroundStyle(DGColor.c900)
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                bullet(emoji: "⚾", text: "경기 일정 등록")
                bullet(emoji: "📋", text: "라인업 확정")
                bullet(emoji: "⏰", text: "출석 응답 리마인드")
            }
            .padding(.horizontal, DGSpacing.xl)
            Spacer()
            VStack(spacing: DGSpacing.md) {
                DGButton("알림 허용", style: .primary) {
                    Task { await onAllow() }
                }
                DGButton("나중에", style: .tertiary, action: onSkip)
            }
            .padding(.horizontal, DGSpacing.xl)
            .padding(.bottom, DGSpacing.xxl)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DGColor.c0)
    }

    private func bullet(emoji: String, text: String) -> some View {
        HStack(spacing: DGSpacing.md) {
            Text(emoji).font(.system(size: 22))
            Text(text)
                .font(DGFont.pretendard(.regular, size: 18))
                .foregroundStyle(DGColor.c700)
        }
    }
}
