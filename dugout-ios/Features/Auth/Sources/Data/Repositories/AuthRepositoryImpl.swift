//
//  AuthRepositoryImpl.swift
//  DugoutAuthFeature
//

import Foundation
import DugoutCoreNetwork

/// Domain AuthRepository의 구현체.
public struct AuthRepositoryImpl: AuthRepository {
    private let client: APIClient
    private let tokenStore: TokenStore

    public init(
        client: APIClient = .shared,
        tokenStore: TokenStore = .shared
    ) {
        self.client = client
        self.tokenStore = tokenStore
    }

    public func oauthLogin(provider: AuthProvider, accessToken: String) async throws -> User {
        let path = "/api/v1/auth/\(provider.rawValue.lowercased())"
        let body = OAuthLoginRequestDTO(accessToken: accessToken)
        let endpoint = APIEndpoint.json(
            path: path,
            method: .post,
            body: body,
            requiresAuth: false
        )

        let response: AuthResponseDTO = try await client.request(endpoint)
        await tokenStore.save(
            accessToken: response.accessToken,
            refreshToken: response.refreshToken
        )
        return response.user.toDomain()
    }

    public func devLogin(nickname: String) async throws -> User {
        let body = DevLoginRequestDTO(nickname: nickname)
        let endpoint = APIEndpoint.json(
            path: "/api/v1/auth/dev-login",
            method: .post,
            body: body,
            requiresAuth: false
        )

        let response: AuthResponseDTO = try await client.request(endpoint)
        await tokenStore.save(
            accessToken: response.accessToken,
            refreshToken: response.refreshToken
        )
        return response.user.toDomain()
    }

    public func logout() async throws {
        let endpoint = APIEndpoint(path: "/api/v1/auth/logout", method: .delete)
        do {
            try await client.requestVoid(endpoint)
        } catch APIError.unauthorized {
            // 이미 만료된 세션도 정상 로그아웃으로 처리
        }
        await tokenStore.clear()
    }
}
