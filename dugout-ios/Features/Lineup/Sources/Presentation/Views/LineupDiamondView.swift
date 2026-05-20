//
//  LineupDiamondView.swift
//  DugoutLineupFeature
//
//  9 필드 포지션을 다이아몬드 비율 좌표로 배치. DH 는 다이아몬드 외부에 별도 노출.

import SwiftUI
import DugoutDesignSystem

struct LineupDiamondView: View {
    let entriesByPosition: [BaseballPosition: PositionOccupant]

    struct PositionOccupant: Sendable, Equatable {
        let nickname: String
        let jerseyNumber: Int?
    }

    private static let positionCoordinates: [(BaseballPosition, x: CGFloat, y: CGFloat)] = [
        (.centerField, 0.50, 0.10),
        (.leftField,   0.20, 0.18),
        (.rightField,  0.80, 0.18),
        (.shortStop,   0.38, 0.40),
        (.secondBase,  0.62, 0.40),
        (.thirdBase,   0.22, 0.55),
        (.firstBase,   0.78, 0.55),
        (.pitcher,     0.50, 0.70),
        (.catcher,     0.50, 0.92),
    ]

    var body: some View {
        GeometryReader { geo in
            ZStack {
                background(in: geo.size)
                ForEach(Self.positionCoordinates, id: \.0) { (pos, x, y) in
                    chip(for: pos)
                        .position(x: geo.size.width * x, y: geo.size.height * y)
                }
            }
        }
        .aspectRatio(1.0, contentMode: .fit)
    }

    private func background(in size: CGSize) -> some View {
        LinearGradient(
            colors: [DGColor.p50, DGColor.c0],
            startPoint: .top, endPoint: .bottom
        )
        .clipShape(RoundedRectangle(cornerRadius: DGRadius.card))
        .overlay(
            RoundedRectangle(cornerRadius: DGRadius.card)
                .stroke(DGColor.c200, lineWidth: 1)
        )
    }

    private func chip(for position: BaseballPosition) -> some View {
        let occupant = entriesByPosition[position]
        return VStack(spacing: 2) {
            Text(position.shortName)
                .font(DGFont.pretendard(.semibold, size: 11))
                .foregroundStyle(occupant == nil ? DGColor.c500 : DGColor.p600)
            if let occupant {
                Text(label(for: occupant))
                    .font(DGFont.pretendard(.semibold, size: 12))
                    .foregroundStyle(DGColor.c900)
                    .lineLimit(1)
            } else {
                Text("—")
                    .font(DGFont.pretendard(.regular, size: 12))
                    .foregroundStyle(DGColor.c300)
            }
        }
        .frame(width: 56, height: 44)
        .background(occupant == nil ? Color.clear : DGColor.c0)
        .clipShape(RoundedRectangle(cornerRadius: DGRadius.button))
        .overlay(
            RoundedRectangle(cornerRadius: DGRadius.button)
                .stroke(
                    occupant == nil ? DGColor.c200 : DGColor.p500,
                    style: StrokeStyle(lineWidth: 1, dash: occupant == nil ? [3, 3] : [])
                )
        )
    }

    private func label(for occupant: PositionOccupant) -> String {
        let short = String(occupant.nickname.prefix(2))
        if let jersey = occupant.jerseyNumber {
            return "\(short) #\(jersey)"
        }
        return short
    }
}
