//
//  LineupShareSheet.swift
//  DugoutLineupFeature
//
//  ImageRenderer 로 LineupShareCardView 를 UIImage 로 캡쳐한 뒤
//  UIActivityViewController(시스템 share sheet)에 전달.
//

import SwiftUI
import UIKit

struct LineupShareSheet: View {
    let lineup: Lineup
    let shareContext: LineupShareContext?
    @Environment(\.dismiss) private var dismiss
    @State private var renderedImage: UIImage?

    var body: some View {
        Group {
            if let image = renderedImage {
                ShareSheetRepresentable(items: [image]) { dismiss() }
            } else {
                ProgressView()
                    .task { renderedImage = await renderCard() }
            }
        }
    }

    @MainActor
    private func renderCard() async -> UIImage? {
        let card = LineupShareCardView(lineup: lineup, context: shareContext)
            .frame(width: 1080, height: 1920)
        let renderer = ImageRenderer(content: card)
        renderer.scale = 2.0
        return renderer.uiImage
    }
}

private struct ShareSheetRepresentable: UIViewControllerRepresentable {
    let items: [Any]
    let onDismiss: () -> Void

    func makeUIViewController(context: Context) -> UIActivityViewController {
        let vc = UIActivityViewController(activityItems: items, applicationActivities: nil)
        vc.completionWithItemsHandler = { _, _, _, _ in onDismiss() }
        return vc
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
