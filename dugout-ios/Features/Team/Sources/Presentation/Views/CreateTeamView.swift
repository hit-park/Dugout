//
//  CreateTeamView.swift
//  DugoutTeamFeature
//

import SwiftUI
import DugoutDesignSystem

public struct CreateTeamView: View {
    @State private var viewModel: CreateTeamViewModel
    private let onCompleted: @MainActor () async -> Void

    @Environment(\.dismiss) private var dismiss

    public init(
        viewModel: CreateTeamViewModel = CreateTeamViewModel(),
        onCompleted: @escaping @MainActor () async -> Void
    ) {
        _viewModel = State(wrappedValue: viewModel)
        self.onCompleted = onCompleted
    }

    public var body: some View {
        NavigationStack {
            Form {
                Section("팀 정보") {
                    TextField("팀 이름", text: $viewModel.name)
                    TextField("지역 (예: 서울 강남)", text: $viewModel.region)
                    Picker("부수", selection: $viewModel.division) {
                        ForEach(viewModel.availableDivisions, id: \.self) { div in
                            Text("\(div)부").tag(div)
                        }
                    }
                }

                Section("활동 요일") {
                    ForEach(viewModel.availableDays, id: \.self) { day in
                        Toggle(displayDay(day), isOn: Binding(
                            get: { viewModel.activityDays.contains(day) },
                            set: { isOn in
                                if isOn { viewModel.activityDays.insert(day) }
                                else { viewModel.activityDays.remove(day) }
                            }
                        ))
                    }
                }

                Section("활동 시간 (선택)") {
                    TextField("예: 18:00-21:00", text: $viewModel.activityTime)
                }

                Section("라인업 모드") {
                    Picker("모드", selection: $viewModel.lineupMode) {
                        Text("균등 출전 (BALANCED)").tag(LineupMode.balanced)
                        Text("실력 우선 (COMPETITIVE)").tag(LineupMode.competitive)
                    }
                    .pickerStyle(.segmented)
                }

                if case .failed(let message) = viewModel.state {
                    Section {
                        Text(message)
                            .foregroundStyle(DGColor.warning)
                    }
                }
            }
            .navigationTitle("팀 만들기")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("취소") { dismiss() }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button("생성") {
                        Task {
                            await viewModel.submit()
                            if case .success = viewModel.state {
                                await onCompleted()
                                dismiss()
                            }
                        }
                    }
                    .disabled(!viewModel.canSubmit)
                }
            }
            .interactiveDismissDisabled(isSubmitting)
        }
    }

    private var isSubmitting: Bool {
        if case .submitting = viewModel.state { return true }
        return false
    }

    private func displayDay(_ code: String) -> String {
        switch code {
        case "MON": "월"
        case "TUE": "화"
        case "WED": "수"
        case "THU": "목"
        case "FRI": "금"
        case "SAT": "토"
        case "SUN": "일"
        default: code
        }
    }
}
