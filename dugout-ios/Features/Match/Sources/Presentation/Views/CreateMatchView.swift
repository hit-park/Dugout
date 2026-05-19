//
//  CreateMatchView.swift
//  DugoutMatchFeature
//

import SwiftUI
import DugoutDesignSystem

public struct CreateMatchView: View {
    @Environment(\.dismiss) private var dismiss
    @State private var viewModel: CreateMatchViewModel
    private let onCreated: (Match) -> Void

    public init(teamId: Int64, onCreated: @escaping (Match) -> Void) {
        _viewModel = State(initialValue: CreateMatchViewModel(teamId: teamId))
        self.onCreated = onCreated
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: DGSpacing.lg) {
                    sectionDateTime
                    sectionOpponentGround
                    sectionDeadline
                    sectionMemo
                    if case .failed(let message) = viewModel.state {
                        Text(message)
                            .dgText(.subText)
                            .foregroundStyle(DGColor.danger)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    submitButton
                }
                .padding(DGSpacing.lg)
            }
            .background(DGColor.c100)
            .navigationTitle("경기 등록")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                        .disabled(viewModel.isSubmitting)
                }
            }
            .onChange(of: viewModel.state) { _, newValue in
                if case .success(let match) = newValue {
                    onCreated(match)
                    dismiss()
                }
            }
            .interactiveDismissDisabled(viewModel.isSubmitting)
        }
    }

    // MARK: - Sections

    private var sectionDateTime: some View {
        section("날짜 / 시간") {
            DatePicker("경기 날짜", selection: $viewModel.matchDate, displayedComponents: .date)
                .environment(\.locale, Locale(identifier: "ko_KR"))
            DatePicker("경기 시간", selection: $viewModel.matchTime, displayedComponents: .hourAndMinute)
                .environment(\.locale, Locale(identifier: "ko_KR"))
            Toggle("집합 시간 분리", isOn: $viewModel.hasGatherTime)
            if viewModel.hasGatherTime {
                DatePicker("집합 시간", selection: $viewModel.gatherTime, displayedComponents: .hourAndMinute)
                    .environment(\.locale, Locale(identifier: "ko_KR"))
            }
        }
    }

    private var sectionOpponentGround: some View {
        section("상대 / 장소") {
            DGTextField(label: "상대팀", placeholder: "선택 입력", text: $viewModel.opponentName)
            DGTextField(label: "구장", placeholder: "선택 입력", text: $viewModel.groundName)
        }
    }

    private var sectionDeadline: some View {
        section("출석 마감") {
            Toggle("마감 시간 설정", isOn: $viewModel.hasVoteDeadline)
            if viewModel.hasVoteDeadline {
                DatePicker("마감 시각", selection: $viewModel.voteDeadline)
                    .environment(\.locale, Locale(identifier: "ko_KR"))
            }
        }
    }

    private var sectionMemo: some View {
        section("메모") {
            DGTextField(label: "메모", placeholder: "선택 입력", text: $viewModel.memo)
        }
    }

    private func section<Content: View>(
        _ title: String,
        @ViewBuilder content: @escaping () -> Content
    ) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text(title).dgText(.cardTitle)
                content()
            }
        }
    }

    private var submitButton: some View {
        DGButton(
            "경기 등록",
            style: .primary,
            isLoading: viewModel.isSubmitting,
            isEnabled: viewModel.canSubmit
        ) {
            Task { await viewModel.submit() }
        }
    }
}

extension CreateMatchViewModel.State: Equatable {
    public static func == (lhs: CreateMatchViewModel.State, rhs: CreateMatchViewModel.State) -> Bool {
        switch (lhs, rhs) {
        case (.editing, .editing), (.submitting, .submitting): true
        case (.success(let a), .success(let b)): a.id == b.id
        case (.failed(let a), .failed(let b)): a == b
        default: false
        }
    }
}
