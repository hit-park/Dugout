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
}
