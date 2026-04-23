//
//  AuthInterceptor.swift
//  Dugout
//

import Foundation
import Alamofire

/// Alamofire RequestInterceptor 구현.
/// - adapt: 요청에 JWT Access Token Bearer 자동 삽입
/// - retry: 401 응답 시 Refresh Token으로 재발급 후 재시도
///
/// 401 재시도 중 다른 요청의 401이 동시다발적으로 발생할 수 있으므로
/// 재발급 작업을 actor로 직렬화한다.
public final class AuthInterceptor: RequestInterceptor, @unchecked Sendable {

    private let tokenStore: TokenStore
    private let refresher: TokenRefresher

    public init(
        tokenStore: TokenStore = .shared,
        refresher: TokenRefresher = TokenRefresher()
    ) {
        self.tokenStore = tokenStore
        self.refresher = refresher
    }

    public func adapt(
        _ urlRequest: URLRequest,
        for session: Session,
        completion: @escaping @Sendable (Result<URLRequest, any Error>) -> Void
    ) {
        var request = urlRequest
        // 헤더 "X-Skip-Auth: 1" 이 있으면 토큰 주입 생략 (로그인/갱신 요청)
        let skipAuth = request.value(forHTTPHeaderField: Self.skipAuthHeader) == "1"
        request.setValue(nil, forHTTPHeaderField: Self.skipAuthHeader)

        if skipAuth {
            completion(.success(request))
            return
        }

        Task {
            if let token = await tokenStore.accessToken {
                request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
            }
            completion(.success(request))
        }
    }

    public func retry(
        _ request: Request,
        for session: Session,
        dueTo error: any Error,
        completion: @escaping @Sendable (RetryResult) -> Void
    ) {
        guard
            let statusCode = request.response?.statusCode,
            statusCode == 401,
            request.retryCount < 1
        else {
            completion(.doNotRetry)
            return
        }

        Task {
            let ok = await refresher.refreshIfNeeded()
            completion(ok ? .retry : .doNotRetryWithError(APIError.unauthorized))
        }
    }

    public static let skipAuthHeader = "X-Skip-Auth"
}

/// Refresh 요청을 직렬화하는 actor.
/// 동시다발적 401 발생 시 한 번만 refresh 하고 나머지는 결과를 공유한다.
public actor TokenRefresher {
    private var inflight: Task<Bool, Never>?
    private let tokenStore: TokenStore
    private let session: Session
    private let baseURL: URL
    private let decoder: JSONDecoder

    public init(
        tokenStore: TokenStore = .shared,
        session: Session = .default,
        baseURL: URL = AppConfig.apiBaseURL
    ) {
        self.tokenStore = tokenStore
        self.session = session
        self.baseURL = baseURL
        self.decoder = .dugoutDefault
    }

    public func refreshIfNeeded() async -> Bool {
        if let inflight {
            return await inflight.value
        }
        let task = Task { await performRefresh() }
        inflight = task
        let result = await task.value
        inflight = nil
        return result
    }

    private func performRefresh() async -> Bool {
        guard let refresh = await tokenStore.refreshToken else { return false }

        struct RefreshBody: Encodable { let refresh_token: String }
        struct RefreshResponse: Decodable, Sendable {
            let access_token: String
            let refresh_token: String
        }

        let url = baseURL.appendingPathComponent("/api/v1/auth/refresh")
        let headers: HTTPHeaders = [
            "Content-Type": "application/json",
            AuthInterceptor.skipAuthHeader: "1",
        ]

        do {
            let response = try await session.request(
                url,
                method: .post,
                parameters: RefreshBody(refresh_token: refresh),
                encoder: JSONParameterEncoder(encoder: .dugoutDefault),
                headers: headers
            )
            .validate()
            .serializingDecodable(RefreshResponse.self, decoder: decoder)
            .value

            await tokenStore.save(
                accessToken: response.access_token,
                refreshToken: response.refresh_token
            )
            return true
        } catch {
            await tokenStore.clear()
            return false
        }
    }
}
