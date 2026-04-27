//
//  SplashView.swift
//  Dugout
//

import SwiftUI
import DugoutDesignSystem

struct SplashView: View {
    let onReady: @MainActor () -> Void

    var body: some View {
        VStack(spacing: DGSpacing.md) {
            Image(systemName: "baseball.diamond.bases")
                .font(.system(size: 80))
                .foregroundStyle(DGColor.primary)
            Text("Dugout")
                .font(DGFont.title)
                .foregroundStyle(DGColor.textPrimary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DGColor.background)
        .task {
            try? await Task.sleep(for: .milliseconds(1200))
            onReady()
        }
    }
}

#Preview {
    SplashView(onReady: {})
}
