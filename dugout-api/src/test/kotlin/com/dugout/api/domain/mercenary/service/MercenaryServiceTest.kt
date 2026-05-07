package com.dugout.api.domain.mercenary.service

import com.dugout.api.domain.mercenary.dto.ApplyMercenaryRequest
import com.dugout.api.domain.mercenary.dto.CreateMercenaryRequestPayload
import com.dugout.api.domain.mercenary.dto.ProfileUpsertRequest
import com.dugout.api.domain.mercenary.entity.MercenaryApplication
import com.dugout.api.domain.mercenary.entity.MercenaryProfile
import com.dugout.api.domain.mercenary.entity.MercenaryRequest
import com.dugout.api.domain.mercenary.repository.MercenaryApplicationRepository
import com.dugout.api.domain.mercenary.repository.MercenaryProfileRepository
import com.dugout.api.domain.mercenary.repository.MercenaryRequestRepository
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamMember
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
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
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class MercenaryServiceTest {

    @Mock lateinit var profileRepository: MercenaryProfileRepository
    @Mock lateinit var requestRepository: MercenaryRequestRepository
    @Mock lateinit var applicationRepository: MercenaryApplicationRepository
    @Mock lateinit var userRepository: UserRepository
    @Mock lateinit var teamRepository: TeamRepository
    @Mock lateinit var teamMemberRepository: TeamMemberRepository

    private lateinit var service: MercenaryService

    @BeforeEach
    fun setUp() {
        service = MercenaryService(
            profileRepository, requestRepository, applicationRepository,
            userRepository, teamRepository, teamMemberRepository,
        )
    }

    private fun sampleTeam() = Team.create(name = "두갓FC", region = "서울 강남구")
    private fun sampleUser(nickname: String = "용병") = User.create(AuthProvider.KAKAO, "kakao-1", nickname)

    @Test
    fun `프로필 신규 생성 - 없으면 새로 만들고 필드 적용`() {
        val user = sampleUser("새용병")
        whenever(profileRepository.findByUserId(user.id)).thenReturn(null)
        whenever(userRepository.findById(user.id)).thenReturn(Optional.of(user))
        whenever(profileRepository.save(any<MercenaryProfile>())).thenAnswer {
            it.getArgument<MercenaryProfile>(0)
        }

        val response = service.upsertProfile(
            user.id,
            ProfileUpsertRequest(
                isActive = true,
                regions = listOf("서울 강남"),
                positions = listOf("P", "OF"),
                desiredFee = 30_000L,
            ),
        )

        assertEquals(true, response.isActive)
        assertEquals(listOf("서울 강남"), response.regions)
        assertEquals(30_000L, response.desiredFee)
    }

    @Test
    fun `용병 모집 생성 - MEMBER는 TEAM_ROLE_NOT_ALLOWED`() {
        val team = sampleTeam()
        val member = TeamMember.create(team, sampleUser(), TeamRole.MEMBER)
        whenever(teamMemberRepository.findByTeamIdAndUserId(team.id, 1L)).thenReturn(member)

        val exception = assertThrows<BusinessException> {
            service.createRequest(
                1L, team.id,
                CreateMercenaryRequestPayload(
                    matchId = 100L,
                    neededPositions = listOf("OF"),
                    neededCount = 1,
                ),
            )
        }
        assertEquals(ErrorCode.TEAM_ROLE_NOT_ALLOWED, exception.errorCode)
    }

    @Test
    fun `지원 - OPEN 아닌 모집이면 MERCENARY_REQUEST_NOT_OPEN`() {
        val team = sampleTeam()
        val request = MercenaryRequest.create(
            team = team,
            matchId = 1L,
            neededPositions = listOf("P"),
            neededCount = 1,
        )
        request.close()
        whenever(requestRepository.findById(request.id)).thenReturn(Optional.of(request))

        val exception = assertThrows<BusinessException> {
            service.apply(1L, request.id, ApplyMercenaryRequest(position = "P"))
        }
        assertEquals(ErrorCode.MERCENARY_REQUEST_NOT_OPEN, exception.errorCode)
    }

    @Test
    fun `지원 - 동일 사용자 중복 시 ALREADY_APPLIED_MERCENARY`() {
        val team = sampleTeam()
        val request = MercenaryRequest.create(
            team = team,
            matchId = 1L,
            neededPositions = listOf("P"),
            neededCount = 1,
        )
        whenever(requestRepository.findById(request.id)).thenReturn(Optional.of(request))
        whenever(applicationRepository.existsByRequestIdAndUserId(request.id, 1L)).thenReturn(true)

        val exception = assertThrows<BusinessException> {
            service.apply(1L, request.id, ApplyMercenaryRequest(position = "P"))
        }
        assertEquals(ErrorCode.ALREADY_APPLIED_MERCENARY, exception.errorCode)
    }

    @Test
    fun `지원 - 비활성 프로필이면 INACTIVE_MERCENARY_PROFILE`() {
        val team = sampleTeam()
        val user = sampleUser()
        val request = MercenaryRequest.create(
            team = team,
            matchId = 1L,
            neededPositions = listOf("P"),
            neededCount = 1,
        )
        val profile = MercenaryProfile.create(user).apply {
            update(isActive = false, null, null, null, null, null)
        }

        whenever(requestRepository.findById(request.id)).thenReturn(Optional.of(request))
        whenever(applicationRepository.existsByRequestIdAndUserId(request.id, user.id)).thenReturn(false)
        whenever(profileRepository.findByUserId(user.id)).thenReturn(profile)

        val exception = assertThrows<BusinessException> {
            service.apply(user.id, request.id, ApplyMercenaryRequest(position = "P"))
        }
        assertEquals(ErrorCode.INACTIVE_MERCENARY_PROFILE, exception.errorCode)
    }

    @Test
    fun `지원 - 정상 PENDING 상태로 생성`() {
        val team = sampleTeam()
        val user = sampleUser("지원자")
        val request = MercenaryRequest.create(
            team = team,
            matchId = 1L,
            neededPositions = listOf("P"),
            neededCount = 1,
        )
        val profile = MercenaryProfile.create(user)

        whenever(requestRepository.findById(request.id)).thenReturn(Optional.of(request))
        whenever(applicationRepository.existsByRequestIdAndUserId(request.id, user.id)).thenReturn(false)
        whenever(profileRepository.findByUserId(user.id)).thenReturn(profile)
        whenever(applicationRepository.save(any<MercenaryApplication>())).thenAnswer {
            it.getArgument<MercenaryApplication>(0)
        }

        val response = service.apply(user.id, request.id, ApplyMercenaryRequest("P", memo = "선발 가능"))

        assertEquals("PENDING", response.status)
        assertEquals("P", response.position)
    }

    @Test
    fun `추천 stub - region·position 매칭하는 활성 프로필만 평점순 반환`() {
        val team = sampleTeam()
        val request = MercenaryRequest.create(
            team = team,
            matchId = 1L,
            neededPositions = listOf("P"),
            neededCount = 1,
            regions = listOf("서울 강남"),
        )
        val matched = MercenaryProfile.create(sampleUser("매치O")).apply {
            update(true, listOf("서울 강남"), null, null, listOf("P"), null)
            // rating 보정은 동일 mock에서 추가 처리 없이 0.0
        }
        val unmatchedRegion = MercenaryProfile.create(sampleUser("매치X")).apply {
            update(true, listOf("부산"), null, null, listOf("P"), null)
        }

        whenever(requestRepository.findById(request.id)).thenReturn(Optional.of(request))
        whenever(profileRepository.findAllByIsActiveTrue()).thenReturn(listOf(matched, unmatchedRegion))

        val recommended = service.recommendCandidates(request.id)

        assertEquals(1, recommended.size)
        assertEquals("매치O", recommended[0].nickname)
    }
}
