package com.dugout.api.domain.notification.service

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.notification.event.LineupConfirmedEvent
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import com.dugout.api.global.fcm.FcmClient
import com.dugout.api.global.fcm.FcmMessage
import org.springframework.stereotype.Service
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

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onLineupConfirmed(event: LineupConfirmedEvent) {
        val members = teamMemberRepository.findByTeamIdAndIsActiveTrue(event.teamId)
        val targetUsers = members
            .map { it.user }
            .filter { it.id != event.confirmedBy }
        val tokens = targetUsers.mapNotNull { it.fcmToken }
        if (tokens.isEmpty()) return

        val match = matchRepository.findById(event.matchId).orElse(null) ?: return
        val payload = buildLineupConfirmedMessage(match, event.lineupId)
        val result = fcmClient.sendToTokens(tokens, payload)
        cleanUpInvalidTokens(targetUsers, result.invalidTokens)
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
            data = mapOf(
                "type" to "LINEUP_CONFIRMED",
                "matchId" to match.id.toString(),
                "teamId" to match.team.id.toString(),
                "lineupId" to lineupId.toString(),
            ),
        )
    }

    @Transactional
    fun cleanUpInvalidTokens(users: List<User>, invalidTokens: List<String>) {
        if (invalidTokens.isEmpty()) return
        users.filter { it.fcmToken in invalidTokens }.forEach { it.fcmToken = null }
    }

    private fun isValidFcmTokenShape(token: String): Boolean {
        return token.length in 100..300 && token.all {
            it.isLetterOrDigit() || it == '-' || it == '_' || it == ':'
        }
    }
}
