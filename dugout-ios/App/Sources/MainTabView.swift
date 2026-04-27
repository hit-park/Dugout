//
//  MainTabView.swift
//  Dugout
//

import SwiftUI
import DugoutAuthFeature
import DugoutHomeFeature

struct MainTabView: View {
    @Bindable var authViewModel: AuthViewModel

    var body: some View {
        TabView {
            HomeView(authViewModel: authViewModel)
                .tabItem {
                    Label("홈", systemImage: "house.fill")
                }

            MyPageView(authViewModel: authViewModel)
                .tabItem {
                    Label("마이페이지", systemImage: "person.crop.circle")
                }
        }
    }
}
