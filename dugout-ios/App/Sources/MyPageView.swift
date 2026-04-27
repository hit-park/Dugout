//
//  MyPageView.swift
//  Dugout
//

import SwiftUI
import DugoutAuthFeature
import DugoutDesignSystem

struct MyPageView: View {
    @Bindable var authViewModel: AuthViewModel

    var body: some View {
        NavigationStack {
            VStack(spacing: DGSpacing.lg) {
                Text("마이페이지")
                    .font(DGFont.title2)
                Text("M2에서 구현")
                    .font(DGFont.callout)
                    .foregroundStyle(DGColor.textSecondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(DGColor.background)
            .navigationTitle("마이페이지")
        }
    }
}
