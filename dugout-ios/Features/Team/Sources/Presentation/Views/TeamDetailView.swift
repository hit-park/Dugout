//
//  TeamDetailView.swift
//  DugoutTeamFeature
//

import SwiftUI
import UIKit
import DugoutDesignSystem

public struct TeamDetailView: View {
    @State private var viewModel: TeamDetailViewModel

    public init(viewModel: TeamDetailViewModel) {
        _viewModel = State(wrappedValue: viewModel)
    }

    public var body: some View {
        Group {
            switch viewModel.state {
            case .idle, .loading:
                ProgressView("불러오는 중...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .loaded(let data):
                ScrollView {
                    VStack(alignment: .leading, spacing: DGSpacing.lg) {
                        teamSection(team: data.team)
                        if viewModel.canShowInviteCode {
                            inviteCodeSection
                        }
                        membersSection(members: data.members)
                    }
                    .padding(DGSpacing.lg)
                }
            case .failed(let message):
                VStack(spacing: DGSpacing.md) {
                    Image(systemName: "exclamationmark.triangle")
                        .font(.system(size: 40))
                        .foregroundStyle(DGColor.warning)
                    Text(message)
                        .multilineTextAlignment(.center)
                    DGButton("다시 시도") {
                        Task { await viewModel.load() }
                    }
                    .padding(.horizontal, DGSpacing.xl)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .background(DGColor.background)
        .navigationTitle(navigationTitle)
        .navigationBarTitleDisplayMode(.inline)
        .task {
            if case .idle = viewModel.state {
                await viewModel.load()
            }
        }
    }

    private var navigationTitle: String {
        if case .loaded(let data) = viewModel.state {
            return data.team.name
        }
        return "팀 상세"
    }

    private func teamSection(team: Team) -> some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                Text(team.name)
                    .font(DGFont.title2)
                infoRow(label: "지역", value: team.region)
                infoRow(label: "부수", value: "\(team.division)부")
                if !team.activityDays.isEmpty {
                    infoRow(label: "활동 요일", value: activityDaysDisplay(team.activityDays))
                }
                if let time = team.activityTime, !time.isEmpty {
                    infoRow(label: "활동 시간", value: time)
                }
            }
            .padding(DGSpacing.sm)
        }
    }

    private var inviteCodeSection: some View {
        DGCard {
            VStack(alignment: .leading, spacing: DGSpacing.sm) {
                Text("초대 코드")
                    .font(DGFont.headline)

                if let code = viewModel.inviteCode {
                    HStack {
                        Text(code)
                            .font(DGFont.title3)
                            .monospaced()
                        Spacer()
                        Button {
                            UIPasteboard.general.string = code
                        } label: {
                            Image(systemName: "doc.on.doc")
                        }
                    }
                } else if viewModel.isGeneratingInviteCode {
                    ProgressView("생성 중...")
                } else {
                    DGButton("초대 코드 생성") {
                        Task { await viewModel.generateInviteCode() }
                    }
                }

                if let error = viewModel.inviteCodeError {
                    Text(error)
                        .font(DGFont.footnote)
                        .foregroundStyle(DGColor.warning)
                }
            }
            .padding(DGSpacing.sm)
        }
    }

    private func membersSection(members: [TeamMember]) -> some View {
        VStack(alignment: .leading, spacing: DGSpacing.sm) {
            HStack {
                Text("멤버 (\(members.count))")
                    .font(DGFont.headline)
                Spacer()
            }
            ForEach(members) { member in
                DGCard {
                    HStack(spacing: DGSpacing.md) {
                        Image(systemName: "person.crop.circle.fill")
                            .font(.system(size: 32))
                            .foregroundStyle(DGColor.textSecondary)
                        VStack(alignment: .leading, spacing: DGSpacing.xs) {
                            HStack {
                                Text(member.nickname)
                                    .font(DGFont.callout)
                                if let jersey = member.jerseyNumber {
                                    Text("#\(jersey)")
                                        .font(DGFont.caption)
                                        .foregroundStyle(DGColor.textSecondary)
                                }
                            }
                            HStack(spacing: DGSpacing.xs) {
                                Text(member.role.displayName)
                                    .font(DGFont.caption)
                                    .padding(.horizontal, DGSpacing.xs)
                                    .padding(.vertical, 2)
                                    .background(DGColor.primary.opacity(0.1))
                                    .foregroundStyle(DGColor.primary)
                                    .clipShape(Capsule())
                                if !member.positions.isEmpty {
                                    Text(member.positions.joined(separator: ", "))
                                        .font(DGFont.caption)
                                        .foregroundStyle(DGColor.textSecondary)
                                }
                            }
                        }
                        Spacer()
                    }
                    .padding(DGSpacing.sm)
                }
            }
        }
    }

    private func infoRow(label: String, value: String) -> some View {
        HStack {
            Text(label)
                .foregroundStyle(DGColor.textSecondary)
            Spacer()
            Text(value)
        }
        .font(DGFont.callout)
    }

    private func activityDaysDisplay(_ codes: [String]) -> String {
        codes.map { code in
            DayOfWeek(rawValue: code)?.displayName ?? code
        }.joined(separator: ", ")
    }
}
