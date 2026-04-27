//
//  HomeViewModel.swift
//  DugoutHomeFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class HomeViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case loaded([MyTeam])
        case failed(String)
    }

    public enum PendingAction: Sendable {
        case createTeam
        case joinTeam
    }

    public enum PresentedSheet: Identifiable, Sendable {
        case createTeam
        case joinTeam
        case login

        public var id: String {
            switch self {
            case .createTeam: "createTeam"
            case .joinTeam: "joinTeam"
            case .login: "login"
            }
        }
    }

    public private(set) var state: State = .idle
    public var presentedSheet: PresentedSheet?
    public var pendingAction: PendingAction?

    private let repository: any HomeRepository

    public init(repository: any HomeRepository = HomeRepositoryImpl()) {
        self.repository = repository
    }

    public func loadTeams() async {
        state = .loading
        do {
            let teams = try await repository.fetchMyTeams()
            state = .loaded(teams)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("팀 목록을 불러오지 못했습니다")
        }
    }

    /// 팀 만들기 액션 트리거. 비로그인이면 LoginSheet 띄우고 pending에 보관.
    public func tapCreateTeam(isAuthenticated: Bool) {
        if isAuthenticated {
            presentedSheet = .createTeam
        } else {
            pendingAction = .createTeam
            presentedSheet = .login
        }
    }

    /// 팀 가입 액션 트리거. 비로그인이면 LoginSheet 띄우고 pending에 보관.
    public func tapJoinTeam(isAuthenticated: Bool) {
        if isAuthenticated {
            presentedSheet = .joinTeam
        } else {
            pendingAction = .joinTeam
            presentedSheet = .login
        }
    }

    /// 시트가 닫힌 직후 호출. 로그인 시트가 인증 성공으로 닫힌 경우
    /// pending action 시트(.createTeam / .joinTeam)를 이어서 표시한다.
    /// 같은 transaction에서 sheet binding을 nil → 다른 값으로 swap하면
    /// SwiftUI가 dismiss로 인식해 무시하므로 onDismiss 콜백을 사용한다.
    public func onSheetDismissed(isAuthenticated: Bool) {
        guard let action = pendingAction else { return }
        pendingAction = nil
        guard isAuthenticated else { return }

        switch action {
        case .createTeam: presentedSheet = .createTeam
        case .joinTeam: presentedSheet = .joinTeam
        }
    }

    /// 팀 생성/가입 완료 후 호출 — 시트 닫고 목록 새로고침.
    public func onTeamMutated() async {
        presentedSheet = nil
        await loadTeams()
    }
}
