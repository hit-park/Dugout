//
//  AttendanceVoteSheet.swift
//  DugoutMatchFeature
//

import SwiftUI
import DugoutDesignSystem

public struct AttendanceVoteSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var viewModel: AttendanceVoteViewModel

    private let matchTitle: String
    private let onCompleted: (AttendanceVote) -> Void

    public init(
        matchId: Int64,
        matchTitle: String,
        existingVote: AttendanceVote?,
        onCompleted: @escaping (AttendanceVote) -> Void
    ) {
        _viewModel = State(
            initialValue: AttendanceVoteViewModel(
                matchId: matchId,
                existingVote: existingVote
            )
        )
        self.matchTitle = matchTitle
        self.onCompleted = onCompleted
    }

    public var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: DGSpacing.lg) {
                    contextCard
                    mainChoiceSection
                    partialChoiceSection
                    reasonSection
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
            .navigationTitle("출석 응답")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소") { dismiss() }
                }
            }
            .onChange(of: viewModel.state) { _, newValue in
                if case .success(let vote) = newValue {
                    onCompleted(vote)
                    dismiss()
                }
            }
        }
    }

    // MARK: - 컨텍스트

    private var contextCard: some View {
        DGCard {
            HStack(spacing: DGSpacing.sm) {
                Text("⚾")
                Text(matchTitle)
                    .dgText(.bodyText)
                    .foregroundStyle(DGColor.c700)
            }
        }
    }

    // MARK: - 메인

    private var mainChoiceSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("응답").dgText(.cardTitle)
                HStack(spacing: DGSpacing.sm) {
                    AttendanceStatusButton(
                        title: AttendanceStatus.attend.koreanLabel,
                        emoji: AttendanceStatus.attend.emoji,
                        isSelected: viewModel.mainChoice == .attend
                    ) {
                        viewModel.mainChoice = .attend
                    }
                    AttendanceStatusButton(
                        title: AttendanceStatus.absent.koreanLabel,
                        emoji: AttendanceStatus.absent.emoji,
                        isSelected: viewModel.mainChoice == .absent
                    ) {
                        viewModel.mainChoice = .absent
                    }
                    AttendanceStatusButton(
                        title: AttendanceStatus.maybe.koreanLabel,
                        emoji: AttendanceStatus.maybe.emoji,
                        isSelected: viewModel.mainChoice == .maybe
                    ) {
                        viewModel.mainChoice = .maybe
                    }
                }
            }
        }
    }

    // MARK: - 부분 참여

    private var partialChoiceSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("부분 참여").dgText(.cardTitle)
                if !viewModel.partialEnabled {
                    Text("참가일 때만 선택할 수 있어요")
                        .dgText(.label)
                        .foregroundStyle(DGColor.c500)
                }
                HStack(spacing: DGSpacing.sm) {
                    AttendanceStatusButton(
                        title: AttendanceStatus.late.koreanLabel,
                        emoji: AttendanceStatus.late.emoji,
                        isSelected: viewModel.partialChoice == .late,
                        isEnabled: viewModel.partialEnabled
                    ) {
                        viewModel.partialChoice = viewModel.partialChoice == .late ? .none : .late
                    }
                    AttendanceStatusButton(
                        title: AttendanceStatus.earlyLeave.koreanLabel,
                        emoji: AttendanceStatus.earlyLeave.emoji,
                        isSelected: viewModel.partialChoice == .earlyLeave,
                        isEnabled: viewModel.partialEnabled
                    ) {
                        viewModel.partialChoice = viewModel.partialChoice == .earlyLeave ? .none : .earlyLeave
                    }
                }
            }
        }
    }

    // MARK: - 사유

    private var reasonSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.md) {
                Text("사유 (선택)").dgText(.cardTitle)
                DGTextField(
                    label: "",
                    placeholder: "예: 회식, 부상 등",
                    text: $viewModel.reason,
                    status: viewModel.reason.count > 200
                        ? .error("200자 이내로 입력해주세요")
                        : .normal
                )
            }
        }
    }

    // MARK: - 저장

    private var submitButton: some View {
        DGButton(
            "응답 저장",
            style: .primary,
            isLoading: isSubmitting,
            isEnabled: viewModel.canSubmit
        ) {
            Task { await viewModel.submit() }
        }
    }

    private var isSubmitting: Bool {
        if case .submitting = viewModel.state { return true }
        return false
    }
}
