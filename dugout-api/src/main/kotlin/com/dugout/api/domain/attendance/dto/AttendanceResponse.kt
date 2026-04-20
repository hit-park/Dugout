package com.dugout.api.domain.attendance.dto

import com.dugout.api.domain.attendance.entity.Attendance
import com.dugout.api.domain.attendance.entity.AttendanceStatus
import java.time.LocalDateTime

data class AttendanceResponse(
    val id: Long,
    val matchId: Long,
    val userId: Long,
    val nickname: String,
    val status: String,
    val reason: String?,
    val respondedAt: LocalDateTime,
) {
    companion object {
        fun from(attendance: Attendance): AttendanceResponse = AttendanceResponse(
            id = attendance.id,
            matchId = attendance.match.id,
            userId = attendance.user.id,
            nickname = attendance.user.nickname,
            status = attendance.status.name,
            reason = attendance.reason,
            respondedAt = attendance.respondedAt,
        )
    }
}

/**
 * 경기의 출석 투표 현황 요약.
 * 참가/불참/미정/늦참/조퇴 인원 수와 상세 리스트를 함께 반환한다.
 */
data class AttendanceSummaryResponse(
    val matchId: Long,
    val totalMembers: Int,
    val respondedCount: Int,
    val pendingCount: Int,
    val statusCounts: Map<String, Int>,
    val votes: List<AttendanceResponse>,
) {
    companion object {
        fun from(
            matchId: Long,
            totalMembers: Int,
            votes: List<Attendance>,
        ): AttendanceSummaryResponse {
            val responses = votes.map(AttendanceResponse::from)
            val counts = AttendanceStatus.entries.associate { status ->
                status.name to votes.count { it.status == status }
            }
            val respondedCount = votes.size
            return AttendanceSummaryResponse(
                matchId = matchId,
                totalMembers = totalMembers,
                respondedCount = respondedCount,
                pendingCount = (totalMembers - respondedCount).coerceAtLeast(0),
                statusCounts = counts,
                votes = responses,
            )
        }
    }
}
