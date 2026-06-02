//
//  PushPermissionCoordinator.swift
//  Dugout
//

import FirebaseMessaging
import Foundation
import UIKit
import UserNotifications

actor PushPermissionCoordinator: NSObject {
    static let shared = PushPermissionCoordinator()

    private let repository: any NotificationRepository
    private var lastSyncedToken: String?

    override init() {
        self.repository = NotificationRepositoryImpl()
        super.init()
    }

    func requestAuthorization() async -> Bool {
        let granted = (try? await UNUserNotificationCenter.current()
            .requestAuthorization(options: [.alert, .sound, .badge])) ?? false
        if granted {
            await MainActor.run {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
        return granted
    }

    func registerIfAuthorized() async {
        let settings = await UNUserNotificationCenter.current().notificationSettings()
        if settings.authorizationStatus == .authorized {
            await MainActor.run {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
    }

    func handleNewToken(_ token: String?) async {
        let normalized = token?.trimmingCharacters(in: .whitespacesAndNewlines)
        guard normalized != lastSyncedToken else { return }
        do {
            try await repository.patchFcmToken(normalized)
            lastSyncedToken = normalized
        } catch {
            print("FCM token sync failed: \(error.localizedDescription)")
        }
    }

    func clearToken() async {
        do {
            try await repository.patchFcmToken(nil)
            lastSyncedToken = nil
        } catch {
            print("FCM token clear failed: \(error.localizedDescription)")
        }
        await MainActor.run {
            Messaging.messaging().deleteToken { _ in }
        }
    }
}

extension PushPermissionCoordinator: MessagingDelegate {
    nonisolated func messaging(
        _ messaging: Messaging,
        didReceiveRegistrationToken fcmToken: String?
    ) {
        Task { await self.handleNewToken(fcmToken) }
    }
}

extension PushPermissionCoordinator: UNUserNotificationCenterDelegate {
    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .sound]
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let userInfo = response.notification.request.content.userInfo
        guard let route = PushRoute(userInfo: userInfo) else { return }
        await DeepLinkInbox.shared.submit(route)
    }
}
