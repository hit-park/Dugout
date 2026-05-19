//
//  AttendanceRequestDTO.swift
//  DugoutMatchFeature
//

import Foundation

struct AttendanceVoteRequestDTO: Encodable, Sendable {
    let status: String      // AttendanceStatus rawValue ("ATTEND" 등)
    let reason: String?

    init(_ request: AttendanceVoteRequest) {
        self.status = request.status.rawValue
        self.reason = request.reason
    }
}
