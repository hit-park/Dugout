//
//  APIClient.swift
//  Dugout
//

import Foundation
import Alamofire

/// Alamofire 기반 REST API 클라이언트.
/// - Swift 6 Strict Concurrency 준수
/// - AuthInterceptor가 JWT 삽입 + 401 자동 리프레시 처리
public final class APIClient: @unchecked Sendable {
    public static let shared = APIClient()

    private let session: Session
    private let baseURL: URL
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    public init(
        baseURL: URL = AppConfig.apiBaseURL,
        interceptor: (any RequestInterceptor)? = AuthInterceptor()
    ) {
        self.baseURL = baseURL
        self.decoder = .dugoutDefault
        self.encoder = .dugoutDefault

        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 15
        configuration.timeoutIntervalForResource = 30

        self.session = Session(
            configuration: configuration,
            interceptor: interceptor
        )
    }

    public func request<T: Decodable & Sendable>(
        _ endpoint: APIEndpoint
    ) async throws -> T {
        let dataTask = makeDataRequest(endpoint)
            .validate(statusCode: 200..<300)
            .serializingDecodable(T.self, decoder: decoder)

        let response = await dataTask.response
        switch response.result {
        case .success(let value):
            return value
        case .failure(let afError):
            throw APIError.from(afError, data: response.data, decoder: decoder)
        }
    }

    public func requestVoid(_ endpoint: APIEndpoint) async throws {
        let dataTask = makeDataRequest(endpoint)
            .validate(statusCode: 200..<300)
            .serializingData(emptyResponseCodes: [200, 204])

        let response = await dataTask.response
        if case let .failure(afError) = response.result {
            throw APIError.from(afError, data: response.data, decoder: decoder)
        }
    }

    // MARK: - Private

    private func makeDataRequest(_ endpoint: APIEndpoint) -> DataRequest {
        let url = buildURL(for: endpoint)
        var headers: HTTPHeaders = [
            "Accept": "application/json",
        ]
        if !endpoint.requiresAuth {
            headers.add(name: AuthInterceptor.skipAuthHeader, value: "1")
        }

        let afMethod = Alamofire.HTTPMethod(rawValue: endpoint.method.rawValue)

        if let body = endpoint.body {
            return session.request(
                url,
                method: afMethod,
                parameters: body,
                encoder: JSONParameterEncoder(encoder: encoder),
                headers: headers
            )
        } else {
            return session.request(
                url,
                method: afMethod,
                headers: headers
            )
        }
    }

    private func buildURL(for endpoint: APIEndpoint) -> URL {
        let base = baseURL.appendingPathComponent(endpoint.path)
        guard !endpoint.queryItems.isEmpty else { return base }

        var components = URLComponents(url: base, resolvingAgainstBaseURL: false)
        components?.queryItems = endpoint.queryItems
        return components?.url ?? base
    }
}
