//
//  TokenStore.swift
//  DugoutCoreNetwork
//

import Foundation
import Security

/// JWT access/refresh 토큰 저장소.
/// Phase 1: 메모리 캐시 + UserDefaults 폴백.
/// Phase 2: Keychain으로 대체 (TODO).
///
/// actor로 구현해 Swift 6 Strict Concurrency 준수.
public actor TokenStore {
    public static let shared = TokenStore()

    private let accessKey = "dugout.auth.accessToken"
    private let refreshKey = "dugout.auth.refreshToken"

    private var cachedAccess: String?
    private var cachedRefresh: String?

    public var accessToken: String? {
        if cachedAccess == nil {
            cachedAccess = UserDefaults.standard.string(forKey: accessKey)
        }
        return cachedAccess
    }

    public var refreshToken: String? {
        if cachedRefresh == nil {
            cachedRefresh = UserDefaults.standard.string(forKey: refreshKey)
        }
        return cachedRefresh
    }

    public func save(accessToken: String, refreshToken: String) {
        self.cachedAccess = accessToken
        self.cachedRefresh = refreshToken
        UserDefaults.standard.set(accessToken, forKey: accessKey)
        UserDefaults.standard.set(refreshToken, forKey: refreshKey)
    }

    public func clear() {
        cachedAccess = nil
        cachedRefresh = nil
        UserDefaults.standard.removeObject(forKey: accessKey)
        UserDefaults.standard.removeObject(forKey: refreshKey)
    }

    public var isAuthenticated: Bool {
        accessToken != nil
    }
}
