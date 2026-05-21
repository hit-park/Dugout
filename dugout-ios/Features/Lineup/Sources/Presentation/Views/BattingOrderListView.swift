//
//  BattingOrderListView.swift
//  DugoutLineupFeature

import SwiftUI
import DugoutDesignSystem

struct BattingOrderListView: View {
    let entries: [LineupEntry]

    var body: some View {
        VStack(alignment: .leading, spacing: DGSpacing.sm) {
            ForEach(entries.sorted { ($0.battingOrder ?? 0) < ($1.battingOrder ?? 0) }) { entry in
                row(entry)
            }
        }
    }

    private func row(_ entry: LineupEntry) -> some View {
        HStack(spacing: DGSpacing.sm) {
            Text("\(entry.battingOrder ?? 0)")
                .font(DGFont.pretendard(.bold, size: 16))
                .foregroundStyle(DGColor.p500)
                .frame(width: 24, alignment: .leading)
            Text(entry.nickname).dgText(.bodyText)
            Text("(\(entry.position.shortName))")
                .dgText(.subText)
                .foregroundStyle(DGColor.c500)
            Spacer()
        }
    }
}
