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
}
