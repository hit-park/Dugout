//
//  PushPermissionGate.swift
//  Dugout
//

import SwiftUI
import UserNotifications

struct PushPermissionGate<Content: View>: View {
    @ViewBuilder let content: () -> Content
    @State private var showPriming = false

    var body: some View {
        content()
            .fullScreenCover(isPresented: $showPriming) {
                PushPermissionPrimingView(
                    onAllow: {
                        _ = await PushPermissionCoordinator.shared.requestAuthorization()
                        showPriming = false
                    },
                    onSkip: { showPriming = false }
                )
            }
            .task {
                let settings = await UNUserNotificationCenter.current().notificationSettings()
                switch settings.authorizationStatus {
                case .notDetermined:
                    showPriming = true
                case .authorized, .provisional, .ephemeral:
                    await PushPermissionCoordinator.shared.registerIfAuthorized()
                default:
                    break
                }
            }
    }
}
