//
//  APIEndpoint.swift
//  DugoutCoreNetwork
//

import Foundation

/// Dugout 자체 HTTP 메서드 enum. Alamofire 의존성을 외부로 노출하지 않기 위함.
public enum HTTPMethod: String, Sendable {
    case get = "GET"
    case post = "POST"
    case put = "PUT"
    case patch = "PATCH"
    case delete = "DELETE"
}

/// REST API 요청 정의.
public struct APIEndpoint: Sendable {
    public let path: String
    public let method: HTTPMethod
    public let queryItems: [URLQueryItem]
    /// JSON 본문. Encodable 값을 직접 전달하면 APIClient가 내부에서 인코딩한다.
    public let body: AnyEncodable?
    public let requiresAuth: Bool

    public init(
        path: String,
        method: HTTPMethod = .get,
        queryItems: [URLQueryItem] = [],
        body: AnyEncodable? = nil,
        requiresAuth: Bool = true
    ) {
        self.path = path
        self.method = method
        self.queryItems = queryItems
        self.body = body
        self.requiresAuth = requiresAuth
    }

    public static func json<T: Encodable & Sendable>(
        path: String,
        method: HTTPMethod,
        body: T,
        requiresAuth: Bool = true
    ) -> APIEndpoint {
        APIEndpoint(
            path: path,
            method: method,
            body: AnyEncodable(body),
            requiresAuth: requiresAuth
        )
    }
}

/// 타입이 지워진 Encodable 래퍼. Sendable 준수.
public struct AnyEncodable: Encodable, Sendable {
    private let _encode: @Sendable (Encoder) throws -> Void

    public init<T: Encodable & Sendable>(_ value: T) {
        self._encode = { encoder in try value.encode(to: encoder) }
    }

    public func encode(to encoder: Encoder) throws {
        try _encode(encoder)
    }
}
