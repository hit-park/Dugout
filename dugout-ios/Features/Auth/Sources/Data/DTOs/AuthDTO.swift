import Foundation

struct AuthResponseDTO: Decodable, Sendable {
    let accessToken: String
    let refreshToken: String
    let user: UserDTO

    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case refreshToken = "refresh_token"
        case user
    }
}

struct UserDTO: Decodable, Sendable {
    let id: Int64
    let email: String?
    let nickname: String
    let profileImgUrl: String?
    let provider: String
    let jerseyNumber: Int?
    let mainPosition: String?
    let subPositions: [String]?
    let onboardingStep: Int?

    enum CodingKeys: String, CodingKey {
        case id, email, nickname, provider
        case profileImgUrl = "profile_img_url"
        case jerseyNumber = "jersey_number"
        case mainPosition = "main_position"
        case subPositions = "sub_positions"
        case onboardingStep = "onboarding_step"
    }

    func toDomain() -> User {
        User(
            id: id,
            email: email,
            nickname: nickname,
            profileImgUrl: profileImgUrl,
            provider: AuthProvider(rawValue: provider) ?? .kakao,
            jerseyNumber: jerseyNumber,
            mainPosition: mainPosition.flatMap { BaseballPosition(rawValue: $0) },
            subPositions: (subPositions ?? []).compactMap { BaseballPosition(rawValue: $0) },
            onboardingStep: onboardingStep ?? 0
        )
    }
}

struct OAuthLoginRequestDTO: Encodable, Sendable {
    let accessToken: String
    enum CodingKeys: String, CodingKey { case accessToken = "access_token" }
}

struct DevLoginRequestDTO: Encodable, Sendable {
    let nickname: String
}

// MARK: - Onboarding DTOs

struct CheckNicknameResponseDTO: Decodable, Sendable {
    let available: Bool
    let message: String?
}

struct UpdateProfileRequestDTO: Encodable, Sendable {
    let nickname: String
    let jerseyNumber: Int?
    enum CodingKeys: String, CodingKey {
        case nickname
        case jerseyNumber = "jersey_number"
    }
}

struct UpdatePositionRequestDTO: Encodable, Sendable {
    let mainPosition: String
    let subPositions: [String]
    enum CodingKeys: String, CodingKey {
        case mainPosition = "main_position"
        case subPositions = "sub_positions"
    }
}

struct CompleteOnboardingRequestDTO: Encodable, Sendable {
    let step: Int
    let startMode: String?
    enum CodingKeys: String, CodingKey {
        case step
        case startMode = "start_mode"
    }
}
