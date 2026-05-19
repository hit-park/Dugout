import Foundation
import DugoutCoreNetwork

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
        let endpoint = APIEndpoint.json(path: path, method: .post, body: body, requiresAuth: false)
        let response: AuthResponseDTO = try await client.request(endpoint)
        await tokenStore.save(accessToken: response.accessToken, refreshToken: response.refreshToken)
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
        await tokenStore.save(accessToken: response.accessToken, refreshToken: response.refreshToken)
        return response.user.toDomain()
    }

    public func logout() async throws {
        let endpoint = APIEndpoint(path: "/api/v1/auth/logout", method: .delete)
        do {
            try await client.requestVoid(endpoint)
        } catch APIError.unauthorized {
            // 만료된 세션도 정상 로그아웃 처리
        }
        await tokenStore.clear()
    }

    public func fetchMe() async throws -> User {
        let endpoint = APIEndpoint(path: "/api/v1/users/me", requiresAuth: true)
        let dto: UserDTO = try await client.request(endpoint)
        return dto.toDomain()
    }

    // MARK: - Onboarding (BACKEND-GAP: 실제 엔드포인트 미구현 → 로컬 mock)

    private static let blockedNicknames: Set<String> = ["관리자", "admin", "dugout", "더그아웃"]

    public func checkNickname(_ nickname: String) async throws -> NicknameCheckResult {
        // BACKEND-GAP: GET /users/check-nickname?q={nickname} 미구현
        // 로컬 검증으로 대체. 백엔드 준비 시 아래 주석 해제 후 mock 블록 삭제
        try await Task.sleep(for: .milliseconds(300))
        let trimmed = nickname.trimmingCharacters(in: .whitespacesAndNewlines)
        if Self.blockedNicknames.contains(trimmed.lowercased()) {
            return .unavailable("사용할 수 없는 닉네임이에요")
        }
        if trimmed.count < 2 {
            return .unavailable("2자 이상 입력해 주세요")
        }
        return .available
    }

    public func updateProfile(nickname: String, jerseyNumber: Int?) async throws -> User {
        // BACKEND-GAP: PATCH /users/me 미구현 → fetchMe 결과에 로컬 오버라이드
        let me = try await fetchMe()
        return User(
            id: me.id,
            email: me.email,
            nickname: nickname,
            profileImgUrl: me.profileImgUrl,
            provider: me.provider,
            jerseyNumber: jerseyNumber,
            mainPosition: me.mainPosition,
            subPositions: me.subPositions,
            onboardingStep: max(me.onboardingStep, 1)
        )
    }

    public func updatePosition(main: BaseballPosition, subs: [BaseballPosition]) async throws -> User {
        // BACKEND-GAP: PATCH /users/me/position 미구현 → 로컬 오버라이드
        let me = try await fetchMe()
        return User(
            id: me.id,
            email: me.email,
            nickname: me.nickname,
            profileImgUrl: me.profileImgUrl,
            provider: me.provider,
            jerseyNumber: me.jerseyNumber,
            mainPosition: main,
            subPositions: subs,
            onboardingStep: max(me.onboardingStep, 2)
        )
    }

    public func completeOnboarding(step: Int, startMode: OnboardingStartMode?) async throws -> User {
        // BACKEND-GAP: PATCH /users/me/onboarding 미구현 → 로컬 오버라이드
        let me = try await fetchMe()
        return User(
            id: me.id,
            email: me.email,
            nickname: me.nickname,
            profileImgUrl: me.profileImgUrl,
            provider: me.provider,
            jerseyNumber: me.jerseyNumber,
            mainPosition: me.mainPosition,
            subPositions: me.subPositions,
            onboardingStep: max(me.onboardingStep, step)
        )
    }
}
