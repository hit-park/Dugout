//
//  MyPageView.swift
//  Dugout
//

import SwiftUI
import DugoutAuthFeature
import DugoutDesignSystem

struct MyPageView: View {
    @Bindable var authViewModel: AuthViewModel
    @State private var showLoginSheet = false

    var body: some View {
        NavigationStack {
            Group {
                if authViewModel.isAuthenticated, let user = authViewModel.currentUser {
                    authenticatedContent(user: user)
                } else {
                    guestContent
                }
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(DGColor.background)
            .navigationTitle("마이페이지")
        }
        .sheet(isPresented: $showLoginSheet) {
            LoginSheet(authViewModel: authViewModel)
        }
    }

    @ViewBuilder
    private var guestContent: some View {
        VStack(spacing: DGSpacing.lg) {
            Image(systemName: "person.crop.circle")
                .font(.system(size: 80))
                .foregroundStyle(DGColor.textSecondary)

            Text("로그인이 필요합니다")
                .font(DGFont.title3)

            Text("팀 활동을 시작하려면 로그인하세요")
                .font(DGFont.footnote)
                .foregroundStyle(DGColor.textSecondary)

            DGButton("로그인") {
                showLoginSheet = true
            }
            .padding(.horizontal, DGSpacing.xl)
            .padding(.top, DGSpacing.md)
        }
    }

    @ViewBuilder
    private func authenticatedContent(user: User) -> some View {
        VStack(spacing: DGSpacing.lg) {
            Image(systemName: "person.crop.circle.fill")
                .font(.system(size: 80))
                .foregroundStyle(DGColor.primary)

            VStack(spacing: DGSpacing.xs) {
                Text(user.nickname)
                    .font(DGFont.title2)
                if let email = user.email {
                    Text(email)
                        .font(DGFont.footnote)
                        .foregroundStyle(DGColor.textSecondary)
                }
            }

            DGCard {
                VStack(alignment: .leading, spacing: DGSpacing.sm) {
                    HStack {
                        Text("로그인 방식")
                            .foregroundStyle(DGColor.textSecondary)
                        Spacer()
                        Text(user.provider.rawValue)
                            .font(DGFont.callout)
                    }
                }
                .padding(DGSpacing.sm)
            }
            .padding(.horizontal, DGSpacing.lg)

            Spacer()

            DGButton("로그아웃") {
                Task { await authViewModel.logout() }
            }
            .padding(.horizontal, DGSpacing.xl)
            .padding(.bottom, DGSpacing.xl)
        }
        .padding(.top, DGSpacing.xl)
    }
}
