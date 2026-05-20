//
//  AttendanceVoteRow.swift
//  DugoutMatchFeature
//
//  MATCH-3 (경기 상세 / 출석 현황) + MATCH-5 (출석 요약) 공통 row.
//

import SwiftUI
import DugoutDesignSystem

struct AttendanceVoteRow: View {
    let vote: AttendanceVote

    var body: some View {
        HStack(spacing: DGSpacing.sm) {
            Text(vote.status.emoji)
            Text(vote.nickname).dgText(.bodyText)
            if let reason = vote.reason, !reason.isEmpty {
                Text("· \(reason)")
                    .dgText(.subText)
                    .foregroundStyle(DGColor.c500)
                    .lineLimit(1)
            }
            Spacer()
            Text(Self.shortTime(vote.respondedAt))
                .dgText(.label)
                .foregroundStyle(DGColor.c500)
        }
    }

    private static let timeFormatter: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "ko_KR")
        f.dateFormat = "HH:mm"
        return f
    }()

    private static func shortTime(_ date: Date) -> String {
        timeFormatter.string(from: date)
    }
}
