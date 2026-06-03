//
//  DGToggle.swift
//  DugoutDesignSystem
//

import SwiftUI

public struct DGToggle: View {
    private let title: String
    @Binding private var isOn: Bool

    public init(_ title: String, isOn: Binding<Bool>) {
        self.title = title
        self._isOn = isOn
    }

    public var body: some View {
        Toggle(isOn: $isOn) {
            Text(title)
                .font(DGFont.pretendard(.regular, size: 16))
                .foregroundStyle(DGColor.c900)
        }
        .tint(DGColor.p500)
    }
}
