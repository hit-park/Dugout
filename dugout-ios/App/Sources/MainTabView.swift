//
//  MainTabView.swift
//  Dugout
//
//  커스텀 5탭 컨테이너 (DGTabBar). 미구현 탭은 placeholder — 앱 무중단.
//

import SwiftUI
import DugoutDesignSystem
import DugoutAuthFeature
import DugoutHomeFeature

struct MainTabView: View {
    @Bindable var authViewModel: AuthViewModel
    @Bindable var router: AppRouter

    private let tabs: [DGTabItem<AppTab>] = [
        .init(id: .home, title: "홈", systemImage: "house.fill"),
        .init(id: .schedule, title: "일정", systemImage: "calendar"),
        .init(id: .matching, title: "매칭", systemImage: "figure.baseball"),
        .init(id: .team, title: "팀", systemImage: "person.3.fill"),
        .init(id: .my, title: "마이", systemImage: "person.crop.circle"),
    ]

    var body: some View {
        VStack(spacing: 0) {
            content
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            DGTabBar(items: tabs, selection: $router.selectedTab)
        }
        .ignoresSafeArea(.keyboard)
        .background(DGColor.c100)
    }

    @ViewBuilder
    private var content: some View {
        switch router.selectedTab {
        case .home:
            HomeView(authViewModel: authViewModel)
        case .schedule:
            ScheduleTabHost()
        case .matching:
            placeholder(title: "매칭", message: "팀·용병 매칭 기능을 준비 중이에요")
        case .team:
            placeholder(title: "팀", message: "팀 목록 기능을 준비 중이에요")
        case .my:
            MyPageView(authViewModel: authViewModel)
        }
    }

    private func placeholder(title: String, message: String) -> some View {
        DGEmptyState(icon: "⚾", title: title, message: message)
            .background(DGColor.c100)
    }
}

