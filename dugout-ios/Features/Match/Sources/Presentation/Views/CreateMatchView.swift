//
//  CreateMatchView.swift
//  DugoutMatchFeature
//
//  M5에서 실제 폼으로 교체.
//

import SwiftUI

public struct CreateMatchView: View {
    let teamId: Int64
    let onCreated: (Match) -> Void

    public init(teamId: Int64, onCreated: @escaping (Match) -> Void) {
        self.teamId = teamId
        self.onCreated = onCreated
    }

    public var body: some View {
        Text("CreateMatchView (M5에서 구현)")
    }
}
