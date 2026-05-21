//
//  LineupShareContext.swift
//  DugoutLineupFeature
//
//  공유 카드에 표시할 매치 컨텍스트. 호출자(MatchDetailView)가 직접 생성하여
//  LineupView 의 init 으로 전달. 백엔드 응답 매핑이 아니며 DTO 없음.
//

import Foundation

public struct LineupShareContext: Sendable, Equatable {
    /// 팀 이름. 빈 문자열이면 카드 헤더의 팀 row 자체를 skip.
    public let teamName: String

    /// 상대팀 이름. nil 이면 "vs ..." row skip.
    public let opponentName: String?

    /// 경기 날짜.
    public let matchDate: Date

    /// 경기 시간 표시 문자열 (예: "오후 8:00"). TimeOfDay.displayString 결과를 그대로 받음.
    public let matchTimeText: String

    /// 구장 이름. nil 이면 "📍 ..." row skip.
    public let groundName: String?

    public init(
        teamName: String,
        opponentName: String?,
        matchDate: Date,
        matchTimeText: String,
        groundName: String?
    ) {
        self.teamName = teamName
        self.opponentName = opponentName
        self.matchDate = matchDate
        self.matchTimeText = matchTimeText
        self.groundName = groundName
    }
}
