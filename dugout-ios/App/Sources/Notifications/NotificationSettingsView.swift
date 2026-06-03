//
//  NotificationSettingsView.swift
//  Dugout
//

import SwiftUI
import DugoutDesignSystem

struct NotificationSettingsView: View {
    @StateObject private var viewModel = NotificationSettingsViewModel()

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: DGSpacing.lg) {
                DGToggle("새 경기 일정", isOn: $viewModel.matchCreated)
                DGToggle("라인업 확정", isOn: $viewModel.lineupConfirmed)
                DGToggle("출석 리마인드", isOn: $viewModel.attendanceReminder)
                DGToggle("출석 응답 변경(주장)", isOn: $viewModel.attendanceChanged)

                Divider()

                DGToggle("방해 금지 시간", isOn: $viewModel.dndEnabled)
                if viewModel.dndEnabled {
                    DatePicker("시작", selection: $viewModel.dndStart, displayedComponents: .hourAndMinute)
                    DatePicker("종료", selection: $viewModel.dndEnd, displayedComponents: .hourAndMinute)
                }
            }
            .padding(DGSpacing.lg)
        }
        .navigationTitle("알림 설정")
        .background(DGColor.c100)
        .task { await viewModel.load() }
        .onDisappear { Task { await viewModel.save() } }
    }
}
