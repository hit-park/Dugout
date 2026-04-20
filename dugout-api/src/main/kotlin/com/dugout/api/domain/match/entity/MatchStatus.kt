package com.dugout.api.domain.match.entity

enum class MatchStatus {
    SCHEDULED,    // 예정
    CONFIRMED,    // 확정 (출석 투표 마감)
    IN_PROGRESS,  // 진행 중
    COMPLETED,    // 종료
    CANCELLED,    // 취소
}
