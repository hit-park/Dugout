//
//  DugoutApp.swift
//  Dugout
//

import SwiftUI
import DugoutAuthFeature

@main
struct DugoutApp: App {
    @State private var authViewModel = AuthViewModel()
    @State private var isReady = false

    var body: some Scene {
        WindowGroup {
            Group {
                if isReady {
                    MainTabView(authViewModel: authViewModel)
                } else {
                    SplashView {
                        isReady = true
                    }
                }
            }
            .task {
                await authViewModel.checkAuthStatus()
            }
        }
    }
}
