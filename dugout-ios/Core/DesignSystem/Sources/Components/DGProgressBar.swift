//
//  DGProgressBar.swift
//  DugoutDesignSystem
//
//  온보딩 진행 바. height 4, p500 fill.
//

import SwiftUI

public struct DGProgressBar: View {
    let progress: Double  // 0...1

    public init(progress: Double) {
        self.progress = min(max(progress, 0), 1)
    }

    public var body: some View {
        GeometryReader { geo in
            ZStack(alignment: .leading) {
                Capsule().fill(DGColor.c100)
                Capsule().fill(DGColor.p500)
                    .frame(width: geo.size.width * progress)
            }
        }
        .frame(height: 4)
        .animation(.easeInOut(duration: 0.25), value: progress)
    }
}

#Preview {
    VStack(spacing: DGSpacing.lg) {
        DGProgressBar(progress: 0.33)
        DGProgressBar(progress: 0.66)
        DGProgressBar(progress: 1.0)
    }
    .padding()
    .background(DGColor.c100)
}
