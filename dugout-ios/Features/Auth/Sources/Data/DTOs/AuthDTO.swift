//
//  AuthDTO.swift
//  DugoutAuthFeature
//

import Foundation

/// 서버 JSON 스키마에 직접 매핑되는 DTO. Domain Entity와 분리.
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

    enum CodingKeys: String, CodingKey {
        case id
        case email
        case nickname
        case profileImgUrl = "profile_img_url"
        case provider
    }

    func toDomain() -> User {
        User(
            id: id,
            email: email,
            nickname: nickname,
            profileImgUrl: profileImgUrl,
            provider: AuthProvider(rawValue: provider) ?? .kakao
        )
    }
}

struct OAuthLoginRequestDTO: Encodable, Sendable {
    let accessToken: String

    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
    }
}

struct DevLoginRequestDTO: Encodable, Sendable {
    let nickname: String
}
