//
//  AttendanceStatusButton.swift
//  DugoutMatchFeature
//
//  출석 응답 시트의 3개 메인 버튼 + 2개 부분 토글에서 공유.
//

import SwiftUI
import DugoutDesignSystem

struct AttendanceStatusButton: View {
    let title: String
    let emoji: String
    let isSelected: Bool
    let isEnabled: Bool
    let action: () -> Void

    init(
        title: String,
        emoji: String,
        isSelected: Bool,
        isEnabled: Bool = true,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.emoji = emoji
        self.isSelected = isSelected
        self.isEnabled = isEnabled
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            VStack(spacing: DGSpacing.xs) {
                Text(emoji)
                    .font(.system(size: 24))
                Text(title)
                    .font(DGFont.pretendard(.semibold, size: 14))
                    .foregroundStyle(textColor)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 72)
            .background(backgroundColor)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.card))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.card)
                    .stroke(borderColor, lineWidth: isSelected ? 2 : 1)
            )
            .opacity(isEnabled ? 1 : 0.4)
        }
        .buttonStyle(DGPressStyle())
        .disabled(!isEnabled)
    }

    private var backgroundColor: Color {
        isSelected ? DGColor.p50 : DGColor.c0
    }

    private var borderColor: Color {
        isSelected ? DGColor.p500 : DGColor.c200
    }

    private var textColor: Color {
        isSelected ? DGColor.p600 : DGColor.c700
    }
}
