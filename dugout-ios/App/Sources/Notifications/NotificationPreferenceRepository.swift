//
//  NotificationPreferenceRepository.swift
//  Dugout
//

import DugoutCoreNetwork
import Foundation

struct NotificationPreferenceDTO: Codable, Sendable {
    var matchCreated: Bool
    var lineupConfirmed: Bool
    var attendanceReminder: Bool
    var attendanceChanged: Bool
    var dndEnabled: Bool
    var dndStart: String   // "HH:mm:ss"
    var dndEnd: String
}

protocol NotificationPreferenceRepository: Sendable {
    func fetch() async throws -> NotificationPreferenceDTO
    func update(_ dto: NotificationPreferenceDTO) async throws -> NotificationPreferenceDTO
}

final class NotificationPreferenceRepositoryImpl: NotificationPreferenceRepository {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func fetch() async throws -> NotificationPreferenceDTO {
        let endpoint = APIEndpoint(
            path: "/api/v1/users/me/notification-preferences",
            method: .get
        )
        return try await client.request(endpoint)
    }

    func update(_ dto: NotificationPreferenceDTO) async throws -> NotificationPreferenceDTO {
        let endpoint = APIEndpoint.json(
            path: "/api/v1/users/me/notification-preferences",
            method: .patch,
            body: dto
        )
        return try await client.request(endpoint)
    }
}
