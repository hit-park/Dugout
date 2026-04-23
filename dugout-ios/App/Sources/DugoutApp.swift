//
//  DugoutApp.swift
//  Dugout
//

import SwiftUI
import DugoutAuthFeature

@main
struct DugoutApp: App {
    @State private var authViewModel = AuthViewModel()

    var body: some Scene {
        WindowGroup {
            RootView(authViewModel: authViewModel)
                .task {
                    await authViewModel.checkAuthStatus()
                }
        }
    }
}
