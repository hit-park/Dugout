//
//  NotificationRepository.swift
//  Dugout
//

import DugoutCoreNetwork
import Foundation

protocol NotificationRepository: Sendable {
    func patchFcmToken(_ token: String?) async throws
}

final class NotificationRepositoryImpl: NotificationRepository {
    private let client: APIClient

    init(client: APIClient = .shared) {
        self.client = client
    }

    func patchFcmToken(_ token: String?) async throws {
        struct Body: Encodable, Sendable { let token: String? }
        struct Response: Decodable, Sendable { let ok: Bool }
        let endpoint = APIEndpoint.json(
            path: "/api/v1/users/me/fcm-token",
            method: .patch,
            body: Body(token: token)
        )
        let _: Response = try await client.request(endpoint)
    }
}
