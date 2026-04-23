//
//  JSONCoder.swift
//  Dugout
//

import Foundation

/// 백엔드 Jackson SNAKE_CASE 설정과 일관된 JSON 인코더/디코더.
/// 모델에서 CodingKeys를 명시적으로 선언하므로 기본 전략은 useDefaultKeys.
/// Date는 ISO8601 문자열로 주고받는다.
extension JSONDecoder {
    static var dugoutDefault: JSONDecoder {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601WithFractionalSeconds
        return decoder
    }
}

extension JSONEncoder {
    static var dugoutDefault: JSONEncoder {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601WithFractionalSeconds
        return encoder
    }
}

private extension JSONDecoder.DateDecodingStrategy {
    /// 2026-04-21T10:15:30 / 2026-04-21T10:15:30.123 / 2026-04-21T10:15:30Z 모두 허용.
    static var iso8601WithFractionalSeconds: JSONDecoder.DateDecodingStrategy {
        .custom { decoder in
            let container = try decoder.singleValueContainer()
            let raw = try container.decode(String.self)
            if let date = DateFormatters.iso8601.date(from: raw) {
                return date
            }
            if let date = DateFormatters.iso8601Fractional.date(from: raw) {
                return date
            }
            if let date = DateFormatters.localDateTime.date(from: raw) {
                return date
            }
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "지원하지 않는 날짜 형식: \(raw)"
            )
        }
    }
}

private extension JSONEncoder.DateEncodingStrategy {
    static var iso8601WithFractionalSeconds: JSONEncoder.DateEncodingStrategy {
        .custom { date, encoder in
            var container = encoder.singleValueContainer()
            try container.encode(DateFormatters.iso8601.string(from: date))
        }
    }
}

enum DateFormatters {
    // ISO8601DateFormatter / DateFormatter 는 공식적으로 thread-safe로 문서화되어 있어
    // read-only 사용은 안전하다. Swift 6 Sendable 체크를 통과하기 위해 nonisolated(unsafe) 사용.
    nonisolated(unsafe) static let iso8601: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime]
        return f
    }()

    nonisolated(unsafe) static let iso8601Fractional: ISO8601DateFormatter = {
        let f = ISO8601DateFormatter()
        f.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return f
    }()

    /// 백엔드가 "2026-04-21T10:15:30" 형태(타임존 없이)로 주는 경우 대응.
    static let localDateTime: DateFormatter = {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.timeZone = TimeZone(identifier: "Asia/Seoul")
        f.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        return f
    }()
}
