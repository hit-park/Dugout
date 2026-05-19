//
//  MatchCalendarGrid.swift
//  DugoutMatchFeature
//
//  월간 7x6 그리드. 경기 있는 날 p500 점 표시, 선택 시 p500 원 배경.
//

import SwiftUI
import DugoutDesignSystem

struct MatchCalendarGrid: View {
    let displayedMonth: Date
    let selectedDate: Date?
    let hasMatch: (Date) -> Bool
    let onSelect: (Date) -> Void

    private let calendar = Calendar.koreaCalendar
    private let weekdaySymbols = ["일", "월", "화", "수", "목", "금", "토"]

    var body: some View {
        VStack(spacing: DGSpacing.sm) {
            weekdayHeader
            grid
        }
    }

    private var weekdayHeader: some View {
        HStack(spacing: 0) {
            ForEach(weekdaySymbols, id: \.self) { symbol in
                Text(symbol)
                    .dgText(.subText)
                    .foregroundStyle(DGColor.c500)
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var grid: some View {
        let dates = monthDates()
        let columns = Array(repeating: GridItem(.flexible(), spacing: 0), count: 7)

        return LazyVGrid(columns: columns, spacing: DGSpacing.sm) {
            ForEach(Array(dates.enumerated()), id: \.offset) { _, date in
                if let date {
                    dayCell(for: date)
                } else {
                    Color.clear.frame(height: 40)
                }
            }
        }
    }

    private func dayCell(for date: Date) -> some View {
        let isSelected = selectedDate.map { calendar.isDate($0, inSameDayAs: date) } ?? false
        let isToday = calendar.isDateInToday(date)
        let day = calendar.component(.day, from: date)

        return Button {
            onSelect(date)
        } label: {
            VStack(spacing: 2) {
                Text("\(day)")
                    .dgText(.bodyText)
                    .foregroundStyle(isSelected ? Color.white : (isToday ? DGColor.p500 : DGColor.c700))
                    .frame(width: 32, height: 32)
                    .background(
                        Circle()
                            .fill(isSelected ? DGColor.p500 : Color.clear)
                    )
                Circle()
                    .fill(hasMatch(date) ? DGColor.p500 : Color.clear)
                    .frame(width: 4, height: 4)
            }
            .frame(maxWidth: .infinity, minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
    }

    /// 해당 월의 1일을 시작으로 7x6 그리드용 nil padding 포함 날짜 배열을 반환.
    private func monthDates() -> [Date?] {
        guard
            let monthStart = calendar.date(
                from: calendar.dateComponents([.year, .month], from: displayedMonth)
            ),
            let dayRange = calendar.range(of: .day, in: .month, for: monthStart)
        else {
            return []
        }
        // 일요일=1 ... 토요일=7 → 그리드 인덱스 0..6
        let firstWeekday = calendar.component(.weekday, from: monthStart) - 1
        var cells: [Date?] = Array(repeating: nil, count: firstWeekday)
        for day in dayRange {
            if let date = calendar.date(byAdding: .day, value: day - 1, to: monthStart) {
                cells.append(date)
            }
        }
        // 6주 그리드(42칸)로 패딩
        while cells.count < 42 { cells.append(nil) }
        return cells
    }
}
