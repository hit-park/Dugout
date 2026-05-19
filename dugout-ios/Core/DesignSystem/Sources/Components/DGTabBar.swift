//
//  DGTabBar.swift
//  DugoutDesignSystem
//
//  커스텀 하단 탭바. h64 + safe area, blur, top border.
//  컨테이너 조립(per-tab NavigationStack)은 App 레이어 책임 — 본 컴포넌트는 바 UI.
//

import SwiftUI

public struct DGTabItem<Tag: Hashable & Sendable>: Identifiable, Sendable {
    public let id: Tag
    public let title: String
    public let systemImage: String

    public init(id: Tag, title: String, systemImage: String) {
        self.id = id
        self.title = title
        self.systemImage = systemImage
    }
}

public struct DGTabBar<Tag: Hashable & Sendable>: View {
    let items: [DGTabItem<Tag>]
    @Binding var selection: Tag

    public init(items: [DGTabItem<Tag>], selection: Binding<Tag>) {
        self.items = items
        self._selection = selection
    }

    public var body: some View {
        HStack(spacing: 0) {
            ForEach(items) { item in
                let isActive = item.id == selection
                VStack(spacing: 4) {
                    Image(systemName: item.systemImage)
                        .font(.system(size: 20))
                    Text(item.title)
                        .font(DGFont.mono(size: 10))
                }
                .foregroundStyle(isActive ? DGColor.p500 : DGColor.c400)
                .frame(maxWidth: .infinity)
                .contentShape(Rectangle())
                .onTapGesture {
                    withAnimation(DGMotion.tabCrossfade) { selection = item.id }
                }
            }
        }
        .frame(height: 64)
        .padding(.bottom, DGSpacing.sm)
        .background(
            DGColor.c0.opacity(0.92)
                .background(.ultraThinMaterial)
                .ignoresSafeArea(edges: .bottom)
        )
        .overlay(alignment: .top) {
            Rectangle().fill(DGColor.c200).frame(height: 1)
        }
    }
}

#Preview {
    struct Demo: View {
        @State var sel = 0
        var body: some View {
            VStack {
                Spacer()
                DGTabBar(items: [
                    .init(id: 0, title: "홈", systemImage: "house.fill"),
                    .init(id: 1, title: "일정", systemImage: "calendar"),
                    .init(id: 2, title: "매칭", systemImage: "figure.baseball"),
                    .init(id: 3, title: "팀", systemImage: "person.3.fill"),
                    .init(id: 4, title: "마이", systemImage: "person.crop.circle"),
                ], selection: $sel)
            }
            .background(DGColor.c100)
        }
    }
    return Demo()
}
