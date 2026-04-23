//
//  APIError.swift
//  DugoutCoreNetwork
//

import Foundation
import Alamofire

/// 백엔드 GlobalExceptionHandler의 ErrorResponse 에 대응.
public struct APIErrorResponse: Sendable, Codable, Error {
    public let code: String
    public let message: String
    public let timestamp: String?
}

public enum APIError: Error, Sendable {
    case invalidURL
    case invalidResponse
    case decoding(String)
    case encoding(String)
    case transport(String)
    case server(APIErrorResponse, statusCode: Int)
    case unauthorized
    case unknown

    public var userMessage: String {
        switch self {
        case .invalidURL: "잘못된 요청 주소입니다"
        case .invalidResponse: "서버 응답이 올바르지 않습니다"
        case .decoding: "응답을 해석하지 못했습니다"
        case .encoding: "요청을 생성하지 못했습니다"
        case .transport: "네트워크 연결을 확인해주세요"
        case .server(let response, _): response.message
        case .unauthorized: "로그인이 필요합니다"
        case .unknown: "알 수 없는 오류가 발생했습니다"
        }
    }

    /// Alamofire의 AFError를 도메인 에러로 변환. (internal — Network 모듈 내부에서만)
    static func from(_ afError: AFError, data: Data?, decoder: JSONDecoder) -> APIError {
        if let statusCode = afError.responseCode {
            if statusCode == 401 {
                return .unauthorized
            }
            if let data,
               let serverError = try? decoder.decode(APIErrorResponse.self, from: data) {
                return .server(serverError, statusCode: statusCode)
            }
            return .server(
                APIErrorResponse(
                    code: "UNKNOWN",
                    message: "서버 오류 (status \(statusCode))",
                    timestamp: nil
                ),
                statusCode: statusCode
            )
        }

        switch afError {
        case .responseSerializationFailed(let reason):
            return .decoding(String(describing: reason))
        case .sessionTaskFailed(let error):
            return .transport(error.localizedDescription)
        case .invalidURL:
            return .invalidURL
        default:
            return .transport(afError.localizedDescription)
        }
    }
}
