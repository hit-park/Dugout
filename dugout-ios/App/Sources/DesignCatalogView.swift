//
//  DesignCatalogView.swift
//  Dugout
//
//  Phase 0 검증 수단 — 토큰/컴포넌트 픽셀 대조용. Xcode 캔버스(#Preview)로 확인.
//  docs/design/Dugout Design Spec.html 와 병행 비교.
//

#if DEBUG
import SwiftUI
import DugoutDesignSystem

struct DesignCatalogView: View {
    @State private var nickname = ""
    @State private var code = ""
    @State private var division = 2
    @State private var toast: DGToastItem?

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DGSpacing.xl) {
                group("Colors") {
                    swatchRow(["p50": DGColor.p50, "p100": DGColor.p100, "p500": DGColor.p500, "p700": DGColor.p700])
                    swatchRow(["s100": DGColor.s100, "s500": DGColor.s500, "c100": DGColor.c100, "c500": DGColor.c500])
                    swatchRow(["success": DGColor.success, "warning": DGColor.warning, "danger": DGColor.danger, "info": DGColor.info])
                }
                group("Typography") {
                    Text("화면 제목 26/700").dgText(.screenTitle)
                    Text("섹션 제목 18/700").dgText(.sectionTitle)
                    Text("본문 14/400").dgText(.bodyText)
                    Text("INVITE-CODE").font(DGFont.mono(size: 13, weight: .bold))
                }
                group("Buttons") {
                    DGButton("Primary") {}
                    DGButton("Secondary", style: .secondary) {}
                    DGButton("Destructive", style: .destructive) {}
                    DGButton("Loading", isLoading: true) {}
                }
                group("Social") {
                    DGSocialButton(provider: .kakao) {}
                    DGSocialButton(provider: .google) {}
                }
                group("Badges & Chips") {
                    HStack {
                        DGBadge("주장", variant: .captain)
                        DGBadge("D-2", variant: .dDay)
                        DGChip("두갓FC", kind: .selectable(isSelected: true)) {}
                    }
                }
                group("Fields") {
                    DGTextField(label: "닉네임", placeholder: "닉네임 입력", text: $nickname)
                    DGOTPField(code: $code) { _ in }
                    DGSegmentedControl(options: [1, 2, 3, 4], selection: $division) { "\($0)부" }
                    DGProgressBar(progress: 0.66)
                }
                group("AI Card") {
                    DGAICard(headline: "예상 참가 14~16명", rows: [
                        .init(id: 1, name: "김주장", probability: 0.92),
                        .init(id: 2, name: "이멤버", probability: 0.5),
                    ])
                }
                group("States") {
                    DGErrorState {}
                        .frame(height: 220)
                }
            }
            .padding(DGSpacing.screenPadding)
        }
        .background(DGColor.c100)
        .dgToast(item: $toast)
    }

    private func group(_ title: String, @ViewBuilder _ content: () -> some View) -> some View {
        VStack(alignment: .leading, spacing: DGSpacing.md) {
            Text(title).dgText(.sectionTitle).foregroundStyle(DGColor.c900)
            content()
        }
    }

    private func swatchRow(_ colors: [String: Color]) -> some View {
        HStack {
            ForEach(colors.sorted(by: { $0.key < $1.key }), id: \.key) { name, color in
                VStack(spacing: 4) {
                    RoundedRectangle(cornerRadius: 8).fill(color)
                        .frame(height: 44)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(DGColor.c200))
                    Text(name).font(DGFont.mono(size: 10))
                }
            }
        }
    }
}

#Preview {
    DesignCatalogView()
}
#endif
