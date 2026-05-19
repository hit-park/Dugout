//
//  DGStateViews.swift
//  DugoutDesignSystem
//
//  전역 상태 표준: Empty / Loading(skeleton) / Error.
//

import SwiftUI

// MARK: - Empty

public struct DGEmptyState: View {
    let icon: String
    let title: String
    let message: String
    let primaryTitle: String?
    let primaryAction: (() -> Void)?
    let secondaryTitle: String?
    let secondaryAction: (() -> Void)?

    public init(
        icon: String = "⚾",
        title: String,
        message: String,
        primaryTitle: String? = nil,
        primaryAction: (() -> Void)? = nil,
        secondaryTitle: String? = nil,
        secondaryAction: (() -> Void)? = nil
    ) {
        self.icon = icon
        self.title = title
        self.message = message
        self.primaryTitle = primaryTitle
        self.primaryAction = primaryAction
        self.secondaryTitle = secondaryTitle
        self.secondaryAction = secondaryAction
    }

    public var body: some View {
        VStack(spacing: DGSpacing.md) {
            Text(icon).font(.system(size: 72))
            Text(title).dgText(.sectionTitle).foregroundStyle(DGColor.c900)
            Text(message)
                .dgText(.bodyText)
                .foregroundStyle(DGColor.c500)
                .multilineTextAlignment(.center)
            if let primaryTitle, let primaryAction {
                DGButton(primaryTitle, style: .primary, action: primaryAction)
                    .padding(.top, DGSpacing.sm)
            }
            if let secondaryTitle, let secondaryAction {
                DGButton(secondaryTitle, style: .secondary, action: secondaryAction)
            }
        }
        .padding(DGSpacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Loading (skeleton + 3s 지연 스피너)

public struct DGLoadingState: View {
    public enum Preset { case card, list, dashboard }

    let preset: Preset
    @State private var phase: CGFloat = -1
    @State private var slow = false

    public init(preset: Preset = .card) { self.preset = preset }

    public var body: some View {
        Group {
            if slow {
                VStack(spacing: DGSpacing.md) {
                    ProgressView()
                    Text("오래 걸리고 있어요")
                        .dgText(.subText)
                        .foregroundStyle(DGColor.c500)
                }
            } else {
                VStack(spacing: DGSpacing.md) {
                    ForEach(0..<count, id: \.self) { _ in skeletonCard }
                }
                .padding(DGSpacing.screenPadding)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .task {
            try? await Task.sleep(for: .seconds(3))
            withAnimation { slow = true }
        }
    }

    private var count: Int {
        switch preset {
        case .card: 1
        case .list: 4
        case .dashboard: 3
        }
    }

    private var skeletonCard: some View {
        RoundedRectangle(cornerRadius: DGRadius.card)
            .fill(DGColor.c200)
            .frame(height: preset == .dashboard ? 96 : 64)
            .overlay(
                LinearGradient(
                    colors: [.clear, DGColor.c0.opacity(0.5), .clear],
                    startPoint: .leading, endPoint: .trailing
                )
                .offset(x: phase * 300)
                .clipShape(RoundedRectangle(cornerRadius: DGRadius.card))
            )
            .onAppear {
                withAnimation(.linear(duration: 1.1).repeatForever(autoreverses: false)) {
                    phase = 1.5
                }
            }
    }
}

// MARK: - Error

public struct DGErrorState: View {
    let message: String
    let retry: () -> Void

    public init(message: String = "연결을 확인해 주세요", retry: @escaping () -> Void) {
        self.message = message
        self.retry = retry
    }

    public var body: some View {
        VStack(spacing: DGSpacing.md) {
            Text("⚠️").font(.system(size: 56))
            Text(message)
                .dgText(.bodyText)
                .foregroundStyle(DGColor.c700)
                .multilineTextAlignment(.center)
            DGButton("다시 시도", style: .primary, action: retry)
                .frame(maxWidth: 200)
        }
        .padding(DGSpacing.xl)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

#Preview {
    DGEmptyState(
        title: "팀과 함께 시작해요",
        message: "아직 소속된 팀이 없어요",
        primaryTitle: "팀 만들기", primaryAction: {},
        secondaryTitle: "초대 코드로 참여", secondaryAction: {}
    )
    .background(DGColor.c100)
}
