package com.dugout.api.domain.attendance.service

import com.dugout.api.domain.attendance.dto.AttendanceVoteRequest
import com.dugout.api.domain.attendance.entity.Attendance
import com.dugout.api.domain.attendance.entity.AttendanceStatus
import com.dugout.api.domain.attendance.repository.AttendanceRepository
import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.context.ApplicationEventPublisher
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class AttendanceServiceTest {

    @Mock lateinit var attendanceRepository: AttendanceRepository
    @Mock lateinit var matchRepository: MatchRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var teamMemberRepository: TeamMemberRepository
    @Mock lateinit var eventPublisher: ApplicationEventPublisher

    private lateinit var attendanceService: AttendanceService

    @BeforeEach
    fun setUp() {
        attendanceService = AttendanceService(
            attendanceRepository,
            matchRepository,
            userRepository,
            teamMemberRepository,
            eventPublisher,
        )
    }

    private fun sampleTeam() = Team.create(name = "두갓FC", region = "서울 강남구")
    private fun sampleUser() = User.create(AuthProvider.KAKAO, "kakao-1", "김주장")
    private fun sampleMatch(voteDeadline: LocalDateTime? = null) = Match.create(
        team = sampleTeam(),
        matchDate = LocalDate.of(2099, 5, 1),
        matchTime = LocalTime.of(10, 0),
        voteDeadline = voteDeadline,
    )

    @Test
    fun `출석 투표 - 신규 투표 성공`() {
        val match = sampleMatch()
        val user = sampleUser()
        val member = TeamMember.create(match.team, user, TeamRole.MEMBER)

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(match.team.id, 1L)).thenReturn(member)
        whenever(attendanceRepository.existsByMatchIdAndUserId(match.id, 1L)).thenReturn(false)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(attendanceRepository.save(any<Attendance>())).thenAnswer { it.getArgument<Attendance>(0) }

        val response = attendanceService.vote(
            userId = 1L,
            matchId = match.id,
            request = AttendanceVoteRequest(status = "ATTEND"),
        )

        assertEquals("ATTEND", response.status)
    }

    @Test
    fun `출석 투표 - 조퇴(EARLY_LEAVE) 상태도 허용`() {
        val match = sampleMatch()
        val user = sampleUser()
        val member = TeamMember.create(match.team, user, TeamRole.MEMBER)

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(match.team.id, 1L)).thenReturn(member)
        whenever(attendanceRepository.existsByMatchIdAndUserId(match.id, 1L)).thenReturn(false)
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
        whenever(attendanceRepository.save(any<Attendance>())).thenAnswer { it.getArgument<Attendance>(0) }

        val response = attendanceService.vote(
            userId = 1L,
            matchId = match.id,
            request = AttendanceVoteRequest(status = "EARLY_LEAVE", reason = "5회 종료 후 퇴장"),
        )

        assertEquals("EARLY_LEAVE", response.status)
        assertEquals("5회 종료 후 퇴장", response.reason)
    }

    @Test
    fun `출석 투표 - 팀 멤버가 아니면 NOT_TEAM_MEMBER`() {
        val match = sampleMatch()
        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(match.team.id, 1L)).thenReturn(null)

        val exception = assertThrows<BusinessException> {
            attendanceService.vote(1L, match.id, AttendanceVoteRequest(status = "ATTEND"))
        }
        assertEquals(ErrorCode.NOT_TEAM_MEMBER, exception.errorCode)
    }

    @Test
    fun `출석 투표 - 이미 투표한 경우 ALREADY_VOTED`() {
        val match = sampleMatch()
        val user = sampleUser()
        val member = TeamMember.create(match.team, user, TeamRole.MEMBER)

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(match.team.id, 1L)).thenReturn(member)
        whenever(attendanceRepository.existsByMatchIdAndUserId(match.id, 1L)).thenReturn(true)

        val exception = assertThrows<BusinessException> {
            attendanceService.vote(1L, match.id, AttendanceVoteRequest(status = "ATTEND"))
        }
        assertEquals(ErrorCode.ALREADY_VOTED, exception.errorCode)
    }

    @Test
    fun `출석 투표 - 투표 마감 시간 지나면 VOTE_DEADLINE_PASSED`() {
        val match = sampleMatch(voteDeadline = LocalDateTime.now().minusDays(1))
        val user = sampleUser()
        val member = TeamMember.create(match.team, user, TeamRole.MEMBER)

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(match.team.id, 1L)).thenReturn(member)

        val exception = assertThrows<BusinessException> {
            attendanceService.vote(1L, match.id, AttendanceVoteRequest(status = "ATTEND"))
        }
        assertEquals(ErrorCode.VOTE_DEADLINE_PASSED, exception.errorCode)
    }

    @Test
    fun `출석 투표 - 잘못된 상태값이면 INVALID_ATTENDANCE_STATUS`() {
        // parseStatus는 입력 검증이므로 DB/다른 stub 없이 즉시 예외 발생해야 함
        val exception = assertThrows<BusinessException> {
            attendanceService.vote(1L, 1L, AttendanceVoteRequest(status = "INVALID"))
        }
        assertEquals(ErrorCode.INVALID_ATTENDANCE_STATUS, exception.errorCode)
    }

    @Test
    fun `출석 투표 수정 - 투표 내역이 없으면 VOTE_NOT_FOUND`() {
        val match = sampleMatch()
        val user = sampleUser()
        val member = TeamMember.create(match.team, user, TeamRole.MEMBER)

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(match.team.id, 1L)).thenReturn(member)
        whenever(attendanceRepository.findByMatchIdAndUserId(match.id, 1L)).thenReturn(null)

        val exception = assertThrows<BusinessException> {
            attendanceService.updateVote(1L, match.id, AttendanceVoteRequest(status = "ABSENT"))
        }
        assertEquals(ErrorCode.VOTE_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `출석 투표 수정 - 상태 변경 성공`() {
        val match = sampleMatch()
        val user = sampleUser()
        val member = TeamMember.create(match.team, user, TeamRole.MEMBER)
        val attendance = Attendance.create(match, user, AttendanceStatus.ATTEND)

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(teamMemberRepository.findByTeamIdAndUserId(match.team.id, 1L)).thenReturn(member)
        whenever(attendanceRepository.findByMatchIdAndUserId(match.id, 1L)).thenReturn(attendance)

        val response = attendanceService.updateVote(
            userId = 1L,
            matchId = match.id,
            request = AttendanceVoteRequest(status = "LATE", reason = "교통체증"),
        )

        assertEquals("LATE", response.status)
        assertEquals("교통체증", response.reason)
    }

    @Test
    fun `출석 현황 조회 - 5가지 상태 집계`() {
        val match = sampleMatch()
        val user1 = User.create(AuthProvider.KAKAO, "k1", "주장")
        val user2 = User.create(AuthProvider.KAKAO, "k2", "선수A")
        val user3 = User.create(AuthProvider.KAKAO, "k3", "선수B")
        val user4 = User.create(AuthProvider.KAKAO, "k4", "선수C")

        val votes = listOf(
            Attendance.create(match, user1, AttendanceStatus.ATTEND),
            Attendance.create(match, user2, AttendanceStatus.ABSENT),
            Attendance.create(match, user3, AttendanceStatus.LATE),
            Attendance.create(match, user4, AttendanceStatus.EARLY_LEAVE),
        )

        whenever(matchRepository.findById(match.id)).thenReturn(Optional.of(match))
        whenever(attendanceRepository.findByMatchIdOrderByRespondedAtAsc(match.id)).thenReturn(votes)
        whenever(teamMemberRepository.countByTeamIdAndIsActiveTrue(match.team.id)).thenReturn(10)

        val summary = attendanceService.getAttendanceSummary(match.id)

        assertEquals(10, summary.totalMembers)
        assertEquals(4, summary.respondedCount)
        assertEquals(6, summary.pendingCount)
        assertEquals(1, summary.statusCounts["ATTEND"])
        assertEquals(1, summary.statusCounts["EARLY_LEAVE"])
        assertEquals(0, summary.statusCounts["MAYBE"])
    }
}
