import Foundation

public enum NicknameCheckResult: Sendable {
    case available
    case unavailable(String)
}

public protocol AuthRepository: Sendable {
    func oauthLogin(provider: AuthProvider, accessToken: String) async throws -> User
    func logout() async throws
    func devLogin(nickname: String) async throws -> User
    func fetchMe() async throws -> User

    // MARK: - Onboarding
    func checkNickname(_ nickname: String) async throws -> NicknameCheckResult
    func updateProfile(nickname: String, jerseyNumber: Int?) async throws -> User
    func updatePosition(main: BaseballPosition, subs: [BaseballPosition]) async throws -> User
    func completeOnboarding(step: Int, startMode: OnboardingStartMode?) async throws -> User
}
