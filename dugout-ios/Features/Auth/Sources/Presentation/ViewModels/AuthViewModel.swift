//
//  AuthViewModel.swift
//  DugoutAuthFeature
//

import Foundation
import Observation
import DugoutCoreNetwork

@MainActor
@Observable
public final class AuthViewModel {
    public enum State: Sendable {
        case idle
        case loading
        case authenticated(User)
        case failed(String)
    }

    public private(set) var state: State = .idle
    private let repository: any AuthRepository
    private let tokenStore: TokenStore

    public init(
        repository: any AuthRepository = AuthRepositoryImpl(),
        tokenStore: TokenStore = .shared
    ) {
        self.repository = repository
        self.tokenStore = tokenStore
    }

    /// 앱 시작 시 저장된 토큰이 있는지 확인.
    public func checkAuthStatus() async {
        let authenticated = await tokenStore.isAuthenticated
        if !authenticated {
            state = .idle
        }
    }

    /// Phase 1 더미 로그인: 실제 OAuth 토큰 대신 백엔드가 받을 문자열만 전달.
    public func login(provider: AuthProvider, accessToken: String) async {
        state = .loading
        do {
            let user = try await repository.oauthLogin(provider: provider, accessToken: accessToken)
            state = .authenticated(user)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("로그인 중 오류가 발생했습니다")
        }
    }

    /// 개발 전용 로그인. 백엔드 local 프로필에서만 동작.
    public func devLogin(nickname: String) async {
        state = .loading
        do {
            let user = try await repository.devLogin(nickname: nickname)
            state = .authenticated(user)
        } catch let error as APIError {
            state = .failed(error.userMessage)
        } catch {
            state = .failed("개발 로그인 중 오류가 발생했습니다")
        }
    }

    public func logout() async {
        do {
            try await repository.logout()
        } catch {
            // 서버 에러 무시, 로컬 토큰만 정리 (repository에서 clear 호출됨)
        }
        state = .idle
    }
}
