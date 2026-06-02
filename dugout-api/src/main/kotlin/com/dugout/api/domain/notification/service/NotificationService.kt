package com.dugout.api.domain.notification.service

import com.dugout.api.domain.attendance.entity.AttendanceStatus
import com.dugout.api.domain.attendance.entity.isMeaningfulAttendanceChange
import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.notification.event.AttendanceChangedEvent
import com.dugout.api.domain.notification.event.LineupConfirmedEvent
import com.dugout.api.domain.notification.event.MatchCreatedEvent
import com.dugout.api.domain.notification.event.NotificationType
import com.dugout.api.domain.notification.repository.NotificationPreferenceRepository
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import com.dugout.api.global.fcm.FcmClient
import com.dugout.api.global.fcm.FcmMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.format.TextStyle
import java.util.Locale

@Service
class NotificationService(
    private val userRepository: UserRepository,
    private val matchRepository: MatchRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val fcmClient: FcmClient,
    private val tokenCleanupService: TokenCleanupService,
    private val preferenceRepository: NotificationPreferenceRepository,
) {
    @Transactional
    fun updateFcmToken(userId: Long, token: String?) {
        val normalized = token?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized != null && !isValidFcmTokenShape(normalized)) {
            throw BusinessException(ErrorCode.NOTIFICATION_TOKEN_INVALID)
        }
        val user = userRepository.findById(userId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        user.fcmToken = normalized
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onLineupConfirmed(event: LineupConfirmedEvent) {
        val members = teamMemberRepository.findByTeamIdAndIsActiveTrue(event.teamId)
        val candidates = members
            .map { it.user }
            .filter { it.id != event.confirmedBy }
        val targetUsers = filterByPreference(candidates, NotificationType.LINEUP_CONFIRMED)
        val tokens = targetUsers.mapNotNull { it.fcmToken }
        if (tokens.isEmpty()) return

        val match = matchRepository.findById(event.matchId).orElse(null) ?: return
        val payload = buildLineupConfirmedMessage(match, event.lineupId)
        val result = fcmClient.sendToTokens(tokens, payload)
        tokenCleanupService.clearInvalidTokens(result.invalidTokens)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onMatchCreated(event: MatchCreatedEvent) {
        val members = teamMemberRepository.findByTeamIdAndIsActiveTrue(event.teamId)
        val candidates = members.map { it.user }.filter { it.id != event.createdBy }
        val targetUsers = filterByPreference(candidates, NotificationType.MATCH_CREATED)
        val tokens = targetUsers.mapNotNull { it.fcmToken }
        if (tokens.isEmpty()) return

        val match = matchRepository.findById(event.matchId).orElse(null) ?: return
        val payload = FcmMessage(
            title = "새 경기 일정이 등록됐어요",
            body = matchSummary(match),
            data = notificationData(NotificationType.MATCH_CREATED, match.id, match.team.id),
        )
        val result = fcmClient.sendToTokens(tokens, payload)
        tokenCleanupService.clearInvalidTokens(result.invalidTokens)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onAttendanceChanged(event: AttendanceChangedEvent) {
        if (!isMeaningfulAttendanceChange(event.previous, event.new)) return

        val captain = teamMemberRepository.findByTeamIdAndIsActiveTrue(event.teamId)
            .firstOrNull { it.role == TeamRole.CAPTAIN } ?: return
        if (captain.user.id == event.actorUserId) return
        if (filterByPreference(listOf(captain.user), NotificationType.ATTENDANCE_CHANGED).isEmpty()) return
        val token = captain.user.fcmToken ?: return

        val actor = userRepository.findById(event.actorUserId).orElse(null) ?: return
        val match = matchRepository.findById(event.matchId).orElse(null) ?: return
        val payload = FcmMessage(
            title = "출석 응답이 변경됐어요",
            body = "${actor.nickname}님 · ${attendanceLabel(event.new)} · ${matchSummary(match)}",
            data = notificationData(NotificationType.ATTENDANCE_CHANGED, match.id, match.team.id),
        )
        val result = fcmClient.sendToTokens(listOf(token), payload)
        tokenCleanupService.clearInvalidTokens(result.invalidTokens)
    }

    private fun buildLineupConfirmedMessage(match: Match, lineupId: Long): FcmMessage {
        val date = match.matchDate
        val dateText = "${date.monthValue}월 ${date.dayOfMonth}일 (${
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        })"
        val parts = buildList {
            add(dateText)
            match.groundName?.let { add(it) }
            match.opponentName?.let { add("vs $it") }
        }
        return FcmMessage(
            title = "라인업이 확정됐어요",
            body = parts.joinToString(" · "),
            data = notificationData(
                type = NotificationType.LINEUP_CONFIRMED,
                matchId = match.id,
                teamId = match.team.id,
                lineupId = lineupId,
            ),
        )
    }

    private fun isValidFcmTokenShape(token: String): Boolean {
        return token.length in 100..300 && token.all {
            it.isLetterOrDigit() || it == '-' || it == '_' || it == ':'
        }
    }

    private fun notificationData(
        type: NotificationType,
        matchId: Long,
        teamId: Long,
        lineupId: Long? = null,
    ): Map<String, String> = buildMap {
        put("type", type.name)
        put("matchId", matchId.toString())
        put("teamId", teamId.toString())
        lineupId?.let { put("lineupId", it.toString()) }
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

    private fun attendanceLabel(status: AttendanceStatus): String = when (status) {
        AttendanceStatus.ATTEND -> "참석"
        AttendanceStatus.ABSENT -> "불참"
        AttendanceStatus.MAYBE -> "미정"
        AttendanceStatus.LATE -> "늦참"
        AttendanceStatus.EARLY_LEAVE -> "조퇴"
    }

    private fun filterByPreference(users: List<User>, type: NotificationType): List<User> {
        if (users.isEmpty()) return users
        val prefs = preferenceRepository.findByUserIdIn(users.map { it.id })
            .associateBy { it.userId }
        // row 없으면 기본 on (opt-out 모델)
        return users.filter { user -> prefs[user.id]?.isEnabled(type) ?: true }
    }
}
