package com.dugout.api.global.seed

import com.dugout.api.domain.attendance.entity.Attendance
import com.dugout.api.domain.attendance.entity.AttendanceStatus
import com.dugout.api.domain.attendance.repository.AttendanceRepository
import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.match.repository.MatchRepository
import com.dugout.api.domain.team.entity.LineupMode
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
import com.dugout.api.domain.user.entity.AuthProvider
import com.dugout.api.domain.user.entity.User
import com.dugout.api.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 개발 전용 시드 데이터. 로컬 프로필에서만 등록되며
 * `users` 테이블이 비어있을 때만 실행된다.
 *
 * AI 라인업 추천 검증에 필요한 최소 구성:
 *   - 팀 1개 (두갓FC, 4부)
 *   - 사용자 10명 (주장 1 + 매니저 1 + 일반 8)
 *   - 경기 1개 (D+7, 베어스FC, 잠실야구장)
 *   - 출석 10건 (전원 ATTEND)
 *
 * 닉네임은 합성 가상명만 사용 — 실제 PII 형식 절대 금지.
 */
@Component
@Profile("local")
class LocalSeedRunner(
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val matchRepository: MatchRepository,
    private val attendanceRepository: AttendanceRepository,
) {
    private val log = LoggerFactory.getLogger(LocalSeedRunner::class.java)

    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun seed() {
        if (userRepository.count() > 0) {
            log.info("Local seed skipped: users table not empty")
            return
        }

        val users = createUsers()
        val team = createTeam()
        val members = createTeamMembers(team, users)
        val match = createMatch(team)
        val attendances = createAttendances(match, users)

        log.info(
            "Local seed data inserted: {} users, team={}, {} members, match={}, {} attendances",
            users.size,
            team.id,
            members.size,
            match.id,
            attendances.size,
        )
    }

    private fun createUsers(): List<User> {
        val seeds =
            listOf(
                "주장철" to "local-seed-1",
                "매니저민" to "local-seed-2",
                "선수1" to "local-seed-3",
                "선수2" to "local-seed-4",
                "선수3" to "local-seed-5",
                "선수4" to "local-seed-6",
                "선수5" to "local-seed-7",
                "선수6" to "local-seed-8",
                "선수7" to "local-seed-9",
                "선수8" to "local-seed-10",
            )
        return userRepository.saveAll(
            seeds.map { (nickname, providerId) ->
                User.create(
                    provider = AuthProvider.GOOGLE,
                    providerId = providerId,
                    nickname = nickname,
                )
            },
        )
    }

    private fun createTeam(): Team =
        teamRepository.save(
            Team.create(
                name = "두갓FC",
                region = "서울 강남구",
                division = 4,
                lineupMode = LineupMode.BALANCED,
            ),
        )

    private fun createTeamMembers(
        team: Team,
        users: List<User>,
    ): List<TeamMember> {
        val jerseys = listOf(7, 11, 1, 2, 3, 4, 5, 6, 8, 9)
        return teamMemberRepository.saveAll(
            users.mapIndexed { i, user ->
                val role =
                    when (i) {
                        0 -> TeamRole.CAPTAIN
                        1 -> TeamRole.MANAGER
                        else -> TeamRole.MEMBER
                    }
                TeamMember.create(
                    team = team,
                    user = user,
                    role = role,
                    positions = emptyList(),
                    jerseyNumber = jerseys[i],
                )
            },
        )
    }

    private fun createMatch(team: Team): Match {
        val today = LocalDate.now()
        return matchRepository.save(
            Match.create(
                team = team,
                matchDate = today.plusDays(7),
                matchTime = LocalTime.of(20, 0),
                gatherTime = LocalTime.of(19, 30),
                opponentName = "베어스FC",
                groundName = "잠실야구장",
                voteDeadline = LocalDateTime.of(today.plusDays(6), LocalTime.of(22, 0)),
            ),
        )
    }

    private fun createAttendances(
        match: Match,
        users: List<User>,
    ): List<Attendance> =
        attendanceRepository.saveAll(
            users.map { user ->
                Attendance.create(
                    match = match,
                    user = user,
                    status = AttendanceStatus.ATTEND,
                    reason = null,
                )
            },
        )
}
