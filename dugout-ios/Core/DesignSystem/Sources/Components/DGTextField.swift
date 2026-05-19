//
//  DGTextField.swift
//  DugoutDesignSystem
//
//  label + field + hint. focus ring + valid/error 상태.
//

import SwiftUI

public struct DGTextField: View {
    public enum Status: Equatable, Sendable {
        case normal
        case valid(String)
        case error(String)
    }

    let label: String
    let placeholder: String
    @Binding var text: String
    let status: Status
    let autocapitalize: Bool

    @FocusState private var isFocused: Bool

    public init(
        label: String,
        placeholder: String,
        text: Binding<String>,
        status: Status = .normal,
        autocapitalize: Bool = false
    ) {
        self.label = label
        self.placeholder = placeholder
        self._text = text
        self.status = status
        self.autocapitalize = autocapitalize
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DGSpacing.sm) {
            if !label.isEmpty {
                Text(label)
                    .font(DGFont.label)
                    .foregroundStyle(DGColor.c700)
            }

            TextField(placeholder, text: $text)
                .font(DGFont.bodyText)
                .foregroundStyle(DGColor.c900)
                .textInputAutocapitalization(autocapitalize ? .characters : .never)
                .autocorrectionDisabled()
                .focused($isFocused)
                .padding(.horizontal, DGSpacing.md)
                .frame(height: 48)
                .background(DGColor.c0)
                .clipShape(RoundedRectangle(cornerRadius: DGRadius.field))
                .overlay(
                    RoundedRectangle(cornerRadius: DGRadius.field)
                        .stroke(borderColor, lineWidth: isFocused ? 1.5 : 1)
                )
                .overlay(
                    RoundedRectangle(cornerRadius: DGRadius.field)
                        .stroke(DGColor.p500.opacity(0.22), lineWidth: isFocused ? 3 : 0)
                        .padding(-2)
                )

            switch status {
            case .normal:
                EmptyView()
            case .valid(let msg):
                Text("✓ \(msg)").font(DGFont.subText).foregroundStyle(DGColor.p500)
            case .error(let msg):
                Text(msg).font(DGFont.subText).foregroundStyle(DGColor.danger)
            }
        }
    }

    private var borderColor: Color {
        switch status {
        case .normal: isFocused ? DGColor.p500 : DGColor.c200
        case .valid: DGColor.p500
        case .error: DGColor.danger
        }
    }
}

#Preview {
    VStack(spacing: DGSpacing.lg) {
        DGTextField(label: "닉네임", placeholder: "닉네임을 입력하세요",
                    text: .constant(""))
        DGTextField(label: "닉네임", placeholder: "",
                    text: .constant("두갓"), status: .valid("사용 가능한 닉네임이에요"))
        DGTextField(label: "닉네임", placeholder: "",
                    text: .constant("관리자"), status: .error("이미 사용 중이에요"))
    }
    .padding()
    .background(DGColor.c100)
}
