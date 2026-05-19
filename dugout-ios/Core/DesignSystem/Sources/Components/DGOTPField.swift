//
//  DGOTPField.swift
//  DugoutDesignSystem
//
//  6셀 초대 코드 입력. 완성 시 onComplete 자동 콜백. 에러 시 shake.
//

import SwiftUI

public struct DGOTPField: View {
    let length: Int
    @Binding var code: String
    let isError: Bool
    let onComplete: (String) -> Void

    @FocusState private var isFocused: Bool
    @State private var shake: CGFloat = 0

    public init(
        length: Int = 6,
        code: Binding<String>,
        isError: Bool = false,
        onComplete: @escaping (String) -> Void
    ) {
        self.length = length
        self._code = code
        self.isError = isError
        self.onComplete = onComplete
    }

    public var body: some View {
        ZStack {
            TextField("", text: $code)
                .keyboardType(.asciiCapable)
                .textInputAutocapitalization(.characters)
                .autocorrectionDisabled()
                .focused($isFocused)
                .opacity(0.001)
                .onChange(of: code) { _, newValue in
                    let filtered = String(newValue.uppercased().prefix(length))
                    if filtered != code { code = filtered }
                    if filtered.count == length { onComplete(filtered) }
                }

            HStack(spacing: DGSpacing.sm) {
                ForEach(0..<length, id: \.self) { idx in
                    cell(at: idx)
                }
            }
            .allowsHitTesting(false)
        }
        .offset(x: shake)
        .contentShape(Rectangle())
        .onTapGesture { isFocused = true }
        .onChange(of: isError) { _, newValue in
            guard newValue else { return }
            withAnimation(.default) { shake = -8 }
            withAnimation(.spring(response: 0.2, dampingFraction: 0.3).delay(0.05)) { shake = 0 }
        }
        .onAppear { isFocused = true }
    }

    private func cell(at idx: Int) -> some View {
        let chars = Array(code)
        let char = idx < chars.count ? String(chars[idx]) : ""
        let isCurrent = idx == chars.count && isFocused
        return Text(char)
            .font(DGFont.mono(size: 18, weight: .bold))
            .foregroundStyle(DGColor.c900)
            .frame(width: 38, height: 48)
            .background(isError ? DGColor.danger.opacity(0.06) : DGColor.c0)
            .clipShape(RoundedRectangle(cornerRadius: DGRadius.field))
            .overlay(
                RoundedRectangle(cornerRadius: DGRadius.field)
                    .stroke(borderColor(isCurrent: isCurrent), lineWidth: isCurrent || isError ? 2 : 1)
            )
    }

    private func borderColor(isCurrent: Bool) -> Color {
        if isError { return DGColor.danger }
        if isCurrent { return DGColor.p500 }
        return DGColor.c200
    }
}

#Preview {
    VStack(spacing: DGSpacing.xl) {
        DGOTPField(code: .constant("AB3")) { _ in }
        DGOTPField(code: .constant("WRONG1"), isError: true) { _ in }
    }
    .padding()
    .background(DGColor.c100)
}
