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

    /// 인증 상태 변화 감지 시 호출. 로그인 직후 pending action을 이어간다.
    public func onAuthChanged(isAuthenticated: Bool) {
        guard isAuthenticated, let action = pendingAction else { return }
        switch action {
        case .createTeam: presentedSheet = .createTeam
        case .joinTeam: presentedSheet = .joinTeam
        }
        pendingAction = nil
    }

    /// 시트가 닫힐 때 호출. 비로그인 상태로 닫혔으면 pending action 정리.
    public func onSheetDismissed(isAuthenticated: Bool) {
        if !isAuthenticated {
            pendingAction = nil
        }
    }

    /// 팀 생성/가입 완료 후 호출 — 시트 닫고 목록 새로고침.
    public func onTeamMutated() async {
        presentedSheet = nil
        await loadTeams()
    }
}
