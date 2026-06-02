package com.dugout.api.domain.attendance.service

import com.dugout.api.domain.attendance.dto.AttendanceResponse
import com.dugout.api.domain.attendance.dto.AttendanceSummaryResponse
import com.dugout.api.domain.attendance.dto.AttendanceVoteRequest
import com.dugout.api.domain.attendance.entity.Attendance
import com.dugout.api.domain.attendance.entity.AttendanceStatus
import com.dugout.api.domain.attendance.repository.AttendanceRepository
import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.entity.MatchStatus
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.notification.event.AttendanceChangedEvent
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class AttendanceService(
    private val attendanceRepository: AttendanceRepository,
    private val matchRepository: MatchRepository,
    private val userRepository: UserRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val eventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun vote(userId: Long, matchId: Long, request: AttendanceVoteRequest): AttendanceResponse {
        val status = parseStatus(request.status)
        val match = findMatch(matchId)
        requireTeamMember(match, userId)
        requireVoteOpen(match)

        if (attendanceRepository.existsByMatchIdAndUserId(matchId, userId)) {
            throw BusinessException(ErrorCode.ALREADY_VOTED)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        val saved = attendanceRepository.save(
            Attendance.create(match = match, user = user, status = status, reason = request.reason),
        )

        return AttendanceResponse.from(saved)
    }

    @Transactional
    fun updateVote(userId: Long, matchId: Long, request: AttendanceVoteRequest): AttendanceResponse {
        val status = parseStatus(request.status)
        val match = findMatch(matchId)
        requireTeamMember(match, userId)
        requireVoteOpen(match)

        val attendance = attendanceRepository.findByMatchIdAndUserId(matchId, userId)
            ?: throw BusinessException(ErrorCode.VOTE_NOT_FOUND)

        val previous = attendance.status
        attendance.updateVote(status, request.reason)

        eventPublisher.publishEvent(
            AttendanceChangedEvent(
                matchId = matchId,
                teamId = match.team.id,
                actorUserId = userId,
                previous = previous,
                new = status,
            ),
        )
        return AttendanceResponse.from(attendance)
    }

    fun getAttendanceSummary(matchId: Long): AttendanceSummaryResponse {
        val match = findMatch(matchId)
        val votes = attendanceRepository.findByMatchIdOrderByRespondedAtAsc(matchId)
        val totalMembers = teamMemberRepository.countByTeamIdAndIsActiveTrue(match.team.id)

        return AttendanceSummaryResponse.from(matchId, totalMembers, votes)
    }

    private fun findMatch(matchId: Long): Match =
        matchRepository.findById(matchId)
            .orElseThrow { BusinessException(ErrorCode.MATCH_NOT_FOUND) }

    private fun requireTeamMember(match: Match, userId: Long) {
        val member = teamMemberRepository.findByTeamIdAndUserId(match.team.id, userId)
            ?: throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)

        if (!member.isActive) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }
    }

    private fun requireVoteOpen(match: Match) {
        if (match.status == MatchStatus.CANCELLED) {
            throw BusinessException(ErrorCode.MATCH_ALREADY_CANCELLED)
        }

        val deadline = match.voteDeadline
        if (deadline != null && LocalDateTime.now().isAfter(deadline)) {
            throw BusinessException(ErrorCode.VOTE_DEADLINE_PASSED)
        }
    }

    private fun parseStatus(value: String): AttendanceStatus = try {
        AttendanceStatus.valueOf(value.uppercase())
    } catch (e: IllegalArgumentException) {
        throw BusinessException(ErrorCode.INVALID_ATTENDANCE_STATUS)
    }
}
