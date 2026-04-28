//
//  AuthRepository.swift
//  DugoutAuthFeature
//

import Foundation

/// Auth Feature의 Domain Repository 프로토콜.
/// Data 레이어에서 구현한다.
public protocol AuthRepository: Sendable {
    /// OAuth 제공자별 로그인.
    func oauthLogin(provider: AuthProvider, accessToken: String) async throws -> User

    /// 로그아웃 (서버 + 로컬 토큰 정리).
    func logout() async throws

    /// 개발 전용 로그인. 백엔드 로컬 프로필에서만 동작.
    func devLogin(nickname: String) async throws -> User

    /// 현재 토큰의 user 정보를 조회. 401 시 APIError.unauthorized.
    func fetchMe() async throws -> User
}
