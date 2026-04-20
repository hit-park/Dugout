package com.dugout.api.domain.attendance.entity

/**
 * 출석 투표 상태 5종.
 * - ATTEND: 참가 ✅
 * - ABSENT: 불참 ❌
 * - MAYBE: 미정 ❓
 * - LATE: 늦참 ⏰ (경기 시작 후 합류)
 * - EARLY_LEAVE: 조퇴 🚪 (경기 시작은 참석하나 중간 이탈)
 */
enum class AttendanceStatus {
    ATTEND,
    ABSENT,
    MAYBE,
    LATE,
    EARLY_LEAVE,
}
