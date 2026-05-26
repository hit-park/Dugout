//
//  LineupShareCardView.swift
//  DugoutLineupFeature
//
//  공유 전용 9:16 세로 카드. 1080×1920 으로 ImageRenderer 캡쳐.
//

import SwiftUI
import DugoutDesignSystem

struct LineupShareCardView: View {
    let lineup: Lineup
    let context: LineupShareContext?

    var body: some View {
        VStack(alignment: .leading, spacing: DGSpacing.xl) {
            header
            diamondSection
            battingOrderSection
            if let dh = lineup.entries.first(where: { $0.position == .designatedHitter && !$0.isBench }) {
                dhSection(dh)
            }
            Spacer(minLength: 0)
            footer
        }
        .padding(.horizontal, 48)
        .padding(.vertical, 64)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(DGColor.c0)
    }

    @ViewBuilder
    private var header: some View {
        if let context {
            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                if !context.teamName.isEmpty {
                    HStack(spacing: DGSpacing.sm) {
                        Text("🟢")
                        Text(context.teamName)
                            .font(DGFont.pretendard(.bold, size: 36))
                            .foregroundStyle(DGColor.c900)
                    }
                }
                Text(headerDateLine(context))
                    .font(DGFont.pretendard(.semibold, size: 22))
                    .foregroundStyle(DGColor.c700)
                if let opponent = context.opponentName {
                    Text("vs \(opponent)")
                        .font(DGFont.pretendard(.regular, size: 22))
                        .foregroundStyle(DGColor.c700)
                }
                if let ground = context.groundName {
                    Text("📍 \(ground)")
                        .font(DGFont.pretendard(.regular, size: 20))
                        .foregroundStyle(DGColor.c500)
                }
            }
        }
    }

    private var diamondSection: some View {
        let fieldEntries = lineup.entries.filter { !$0.isBench && $0.position.isField }
        let dict = Dictionary(uniqueKeysWithValues: fieldEntries.map { entry in
            (entry.position, LineupDiamondView.PositionOccupant(
                nickname: entry.nickname, jerseyNumber: nil
            ))
        })
        return LineupDiamondView(entriesByPosition: dict)
            .frame(width: 520, height: 520)
            .frame(maxWidth: .infinity)
    }

    private var battingOrderSection: some View {
        let ordered = lineup.entries
            .filter { !$0.isBench && $0.battingOrder != nil }
            .sorted { ($0.battingOrder ?? 0) < ($1.battingOrder ?? 0) }
        return VStack(alignment: .leading, spacing: DGSpacing.sm) {
            Text("타순")
                .font(DGFont.pretendard(.bold, size: 22))
                .foregroundStyle(DGColor.c900)
            ForEach(ordered) { entry in
                HStack(spacing: DGSpacing.md) {
                    Text("\(entry.battingOrder ?? 0)")
                        .font(DGFont.pretendard(.bold, size: 24))
                        .foregroundStyle(DGColor.p500)
                        .frame(width: 32, alignment: .leading)
                    Text(entry.nickname)
                        .font(DGFont.pretendard(.regular, size: 22))
                    Text("(\(entry.position.shortName))")
                        .font(DGFont.pretendard(.regular, size: 20))
                        .foregroundStyle(DGColor.c500)
                }
            }
        }
    }

    private func dhSection(_ entry: LineupEntry) -> some View {
        HStack(spacing: DGSpacing.md) {
            Text("DH")
                .font(DGFont.pretendard(.bold, size: 22))
                .foregroundStyle(DGColor.p500)
            Text(entry.nickname).font(DGFont.pretendard(.regular, size: 22))
        }
    }

    private var footer: some View {
        HStack {
            Spacer()
            Text("Dugout · AI 라인업")
                .font(DGFont.pretendard(.semibold, size: 16))
                .foregroundStyle(DGColor.c500)
        }
    }

    private func headerDateLine(_ ctx: LineupShareContext) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "M월 d일 (E)"
        return "\(f.string(from: ctx.matchDate)) · \(ctx.matchTimeText)"
    }
}
