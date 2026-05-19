//
//  DugoutApp.swift
//  Dugout
//

import SwiftUI
import DugoutDesignSystem
import DugoutAuthFeature

@main
struct DugoutApp: App {
    @State private var authViewModel = AuthViewModel()
    @State private var isReady = false

    init() {
        DGFontRegistrar.registerIfNeeded()
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if isReady {
                    MainTabView(authViewModel: authViewModel)
                } else {
                    SplashView()
                }
            }
            .preferredColorScheme(.light)
            .task {
                await authViewModel.checkAuthStatus()
                isReady = true
            }
        }
    }
}
