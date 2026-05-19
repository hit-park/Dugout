//
//  AppRouter.swift
//  Dugout
//
//  탭 selection + 탭별 NavigationPath 일원화. 딥링크는 후속 Phase에서 확장.
//

import SwiftUI

enum AppTab: Hashable, Sendable, CaseIterable {
    case home, schedule, matching, team, my
}

@MainActor
@Observable
final class AppRouter {
    var selectedTab: AppTab = .home

    var homePath = NavigationPath()
    var schedulePath = NavigationPath()
    var matchingPath = NavigationPath()
    var teamPath = NavigationPath()
    var myPath = NavigationPath()
}
