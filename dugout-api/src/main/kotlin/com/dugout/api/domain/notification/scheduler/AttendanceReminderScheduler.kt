package com.dugout.api.domain.notification.scheduler

import com.dugout.api.domain.attendance.entity.AttendanceReminderLog
import com.dugout.api.domain.attendance.entity.ReminderWindow
import com.dugout.api.domain.attendance.repository.AttendanceReminderLogRepository
import com.dugout.api.domain.attendance.repository.AttendanceRepository
import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.entity.MatchStatus
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.notification.event.NotificationType
import com.dugout.api.domain.notification.repository.NotificationPreferenceRepository
import com.dugout.api.domain.notification.service.TokenCleanupService
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.global.fcm.FcmClient
import com.dugout.api.global.fcm.FcmMessage
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Component
class AttendanceReminderScheduler(
    private val matchRepository: MatchRepository,
    private val attendanceRepository: AttendanceRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val reminderLogRepository: AttendanceReminderLogRepository,
    private val fcmClient: FcmClient,
    private val tokenCleanupService: TokenCleanupService,
    private val preferenceRepository: NotificationPreferenceRepository,
) {
    private val log = LoggerFactory.getLogger(AttendanceReminderScheduler::class.java)

    @Scheduled(cron = "0 0 * * * *", zone = SEOUL_ZONE_ID)
    @Transactional
    fun sendReminders() {
        val now = LocalDateTime.now(SEOUL_ZONE)
        ReminderWindow.entries.forEach { window -> sendForWindow(window, now) }
    }

    companion object {
        private const val SEOUL_ZONE_ID = "Asia/Seoul"
        private val SEOUL_ZONE: ZoneId = ZoneId.of(SEOUL_ZONE_ID)
    }

    private fun sendForWindow(window: ReminderWindow, now: LocalDateTime) {
        val bucketStart = now.plusHours(window.hoursBefore)
        val bucketEnd = bucketStart.plusHours(1)
        val candidates = matchRepository.findByStatusAndMatchDateBetween(
            MatchStatus.SCHEDULED,
            bucketStart.toLocalDate(),
            bucketEnd.toLocalDate(),
        )
        candidates.forEach { match ->
            val start = LocalDateTime.of(match.matchDate, match.matchTime)
            if (start < bucketStart || start >= bucketEnd) return@forEach
            val deadline = match.voteDeadline
            if (deadline != null && now.isAfter(deadline)) return@forEach
            remindNonResponders(match, window)
        }
    }

    private fun remindNonResponders(match: Match, window: ReminderWindow) {
        val respondedIds = attendanceRepository.findRespondedUserIds(match.id).toSet()
        val nonResponders = teamMemberRepository.findByTeamIdAndIsActiveTrue(match.team.id)
            .map { it.user }
            .filter { it.id !in respondedIds }
            .filter {
                !reminderLogRepository.existsByMatchIdAndUserIdAndReminderWindow(match.id, it.id, window)
            }

        val nowTime = LocalDateTime.now(SEOUL_ZONE).toLocalTime()
        val prefs = preferenceRepository.findByUserIdIn(nonResponders.map { it.id })
            .associateBy { it.userId }
        val targets = nonResponders.filter { user ->
            if (user.fcmToken == null) return@filter false
            val pref = prefs[user.id] ?: return@filter true // row 없으면 기본 on
            if (!pref.attendanceReminder) return@filter false
            // DnD 구간이면 이번엔 skip → 로그 미기록 → DnD 종료 후 정시에 재시도
            !pref.isWithinDnd(nowTime)
        }
        if (targets.isEmpty()) return

        val payload = FcmMessage(
            title = "출석 응답을 잊지 않으셨나요?",
            body = matchSummary(match),
            data = mapOf(
                "type" to NotificationType.ATTENDANCE_REMINDER.name,
                "matchId" to match.id.toString(),
                "teamId" to match.team.id.toString(),
            ),
        )
        val tokens = targets.mapNotNull { it.fcmToken }
        val result = fcmClient.sendToTokens(tokens, payload)
        tokenCleanupService.clearInvalidTokens(result.invalidTokens)

        targets
            .filter { it.fcmToken !in result.invalidTokens }
            .forEach { user -> markSent(match.id, user.id, window) }
        log.info("reminder sent: match=${match.id} window=$window targets=${targets.size}")
    }

    private fun markSent(matchId: Long, userId: Long, window: ReminderWindow) {
        reminderLogRepository.save(AttendanceReminderLog(matchId, userId, window))
    }

    private fun matchSummary(match: Match): String {
        val date = match.matchDate
        val dateText = "${date.monthValue}월 ${date.dayOfMonth}일 (${
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        })"
        return buildList {
            add(dateText)
            match.groundName?.let { add(it) }
            match.opponentName?.let { add("vs $it") }
        }.joinToString(" · ")
    }
}
