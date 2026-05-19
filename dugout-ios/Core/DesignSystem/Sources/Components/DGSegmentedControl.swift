//
//  DGSegmentedControl.swift
//  DugoutDesignSystem
//
//  부수(1~4부)·라인업 모드 등 segmented 선택.
//

import SwiftUI

public struct DGSegmentedControl<T: Hashable & Sendable>: View {
    let options: [T]
    let title: (T) -> String
    @Binding var selection: T

    public init(options: [T], selection: Binding<T>, title: @escaping (T) -> String) {
        self.options = options
        self._selection = selection
        self.title = title
    }

    public var body: some View {
        HStack(spacing: 0) {
            ForEach(options, id: \.self) { option in
                let isSelected = option == selection
                Text(title(option))
                    .font(DGFont.label)
                    .foregroundStyle(isSelected ? .white : DGColor.c700)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, DGSpacing.sm + 2)
                    .background(isSelected ? DGColor.p500 : DGColor.c0)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        withAnimation(DGMotion.tabCrossfade) { selection = option }
                    }
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
        .overlay(
            RoundedRectangle(cornerRadius: DGRadius.button)
                .stroke(DGColor.c200, lineWidth: 1)
        )
    }
}

#Preview {
    DGSegmentedControl(options: [1, 2, 3, 4], selection: .constant(2)) { "\($0)부" }
        .padding()
        .background(DGColor.c100)
}
