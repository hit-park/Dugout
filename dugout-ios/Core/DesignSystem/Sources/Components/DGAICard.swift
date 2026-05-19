//
//  DGAICard.swift
//  DugoutDesignSystem
//
//  AI 예측 카드. bg p50, border p200, 멤버별 확률 행.
//

import SwiftUI

public struct DGAIPredictionRow: Identifiable, Equatable, Sendable {
    public let id: Int64
    public let name: String
    public let probability: Double  // 0...1

    public init(id: Int64, name: String, probability: Double) {
        self.id = id
        self.name = name
        self.probability = probability
    }
}

public struct DGAICard: View {
    let headline: String
    let rows: [DGAIPredictionRow]

    public init(headline: String, rows: [DGAIPredictionRow] = []) {
        self.headline = headline
        self.rows = rows
    }

    public var body: some View {
        VStack(alignment: .leading, spacing: DGSpacing.md) {
            Text("🤖 AI 예측")
                .font(DGFont.badge)
                .foregroundStyle(DGColor.p600)

            Text(headline)
                .dgText(.cardTitle)
                .foregroundStyle(DGColor.c900)

            ForEach(rows) { row in
                HStack(spacing: DGSpacing.sm) {
                    Text(row.name)
                        .font(DGFont.subText)
                        .foregroundStyle(DGColor.c700)
                        .frame(width: 60, alignment: .leading)
                    GeometryReader { geo in
                        ZStack(alignment: .leading) {
                            Capsule().fill(DGColor.c100)
                            Capsule().fill(DGColor.p500)
                                .frame(width: geo.size.width * row.probability)
                        }
                    }
                    .frame(height: 6)
                    Text("\(Int(row.probability * 100))%")
                        .font(DGFont.mono(size: 11))
                        .foregroundStyle(DGColor.c500)
                        .frame(width: 36, alignment: .trailing)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, DGSpacing.md + 2)
        .padding(.vertical, DGSpacing.md)
        .background(DGColor.p50)
        .clipShape(RoundedRectangle(cornerRadius: DGRadius.card))
        .overlay(
            RoundedRectangle(cornerRadius: DGRadius.card)
                .stroke(DGColor.p200, lineWidth: 1)
        )
    }
}

#Preview {
    DGAICard(headline: "예상 참가 14~16명", rows: [
        .init(id: 1, name: "김주장", probability: 0.92),
        .init(id: 2, name: "이멤버", probability: 0.55),
    ])
    .padding()
    .background(DGColor.c100)
}
