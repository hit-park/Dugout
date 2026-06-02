//
//  DeepLinkInbox.swift
//  Dugout
//
//  푸시 탭 → 라우팅을 잇는 브리지. actor 코디네이터가 적재하고
//  MainTabView 가 소비한다. cold-start(앱 종료 중 푸시 탭)도 pending 보관으로 처리.
//

import Foundation

@MainActor
@Observable
final class DeepLinkInbox {
    static let shared = DeepLinkInbox()
    var pending: PushRoute?

    private init() {}

    func submit(_ route: PushRoute) {
        pending = route
    }

    func consume() -> PushRoute? {
        defer { pending = nil }
        return pending
    }
}
