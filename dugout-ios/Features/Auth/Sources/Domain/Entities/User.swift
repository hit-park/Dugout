//
//  User.swift
//  DugoutAuthFeature
//

import Foundation

/// 인증 도메인의 사용자 Entity.
/// Feature 독립성을 위해 각 Feature가 자체 User Entity를 가질 수 있다.
public struct User: Sendable, Equatable, Identifiable, Hashable {
    public let id: Int64
    public let email: String?
    public let nickname: String
    public let profileImgUrl: String?
    public let provider: AuthProvider

    public init(
        id: Int64,
        email: String?,
        nickname: String,
        profileImgUrl: String?,
        provider: AuthProvider
    ) {
        self.id = id
        self.email = email
        self.nickname = nickname
        self.profileImgUrl = profileImgUrl
        self.provider = provider
    }
}

public enum AuthProvider: String, Sendable, Hashable, CaseIterable {
    case kakao = "KAKAO"
    case naver = "NAVER"
    case google = "GOOGLE"
    case apple = "APPLE"
    /// 개발용 가짜 provider. UI에는 노출되지 않음.
    case dev = "DEV"

    /// 실제 로그인 버튼으로 노출할 OAuth 제공자 목록 (dev 제외).
    public static var oauthProviders: [AuthProvider] {
        [.kakao, .naver, .google, .apple]
    }
}

public extension AuthProvider {
    /// UI에 노출할 사용자 친화적 표기.
    /// rawValue("KAKAO", "DEV" 등)는 API 식별자이므로 UI에 직접 사용하지 않는다.
    var displayName: String {
        switch self {
        case .kakao: "카카오"
        case .naver: "네이버"
        case .google: "Google"
        case .apple: "Apple"
        case .dev: "개발 모드"
        }
    }
}
