//
//  RootView.swift
//  Dugout
//

import SwiftUI
import DugoutAuthFeature
import DugoutHomeFeature

struct RootView: View {
    @Bindable var authViewModel: AuthViewModel

    var body: some View {
        switch authViewModel.state {
        case .idle, .loading, .failed:
            AuthView(viewModel: authViewModel)
        case .authenticated:
            HomeView {
                Task { await authViewModel.logout() }
            }
        }
    }
}

#Preview("Login") {
    RootView(authViewModel: AuthViewModel())
}
