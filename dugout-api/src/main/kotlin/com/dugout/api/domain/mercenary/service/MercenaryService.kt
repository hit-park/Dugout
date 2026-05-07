package com.dugout.api.domain.mercenary.service

import com.dugout.api.domain.mercenary.dto.ApplyMercenaryRequest
import com.dugout.api.domain.mercenary.dto.CreateMercenaryRequestPayload
import com.dugout.api.domain.mercenary.dto.MercenaryApplicationResponse
import com.dugout.api.domain.mercenary.dto.MercenaryProfileResponse
import com.dugout.api.domain.mercenary.dto.MercenaryRequestResponse
import com.dugout.api.domain.mercenary.dto.ProfileUpsertRequest
import com.dugout.api.domain.mercenary.entity.MercenaryApplication
import com.dugout.api.domain.mercenary.entity.MercenaryApplicationStatus
import com.dugout.api.domain.mercenary.entity.MercenaryProfile
import com.dugout.api.domain.mercenary.entity.MercenaryRequest
import com.dugout.api.domain.mercenary.entity.MercenaryRequestStatus
import com.dugout.api.domain.mercenary.repository.MercenaryApplicationRepository
import com.dugout.api.domain.mercenary.repository.MercenaryProfileRepository
import com.dugout.api.domain.mercenary.repository.MercenaryRequestRepository
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MercenaryService(
    private val profileRepository: MercenaryProfileRepository,
    private val requestRepository: MercenaryRequestRepository,
    private val applicationRepository: MercenaryApplicationRepository,
    private val userRepository: UserRepository,
    private val teamRepository: TeamRepository,
    private val teamMemberRepository: TeamMemberRepository,
) {

    @Transactional
    fun upsertProfile(userId: Long, request: ProfileUpsertRequest): MercenaryProfileResponse {
        val existing = profileRepository.findByUserId(userId)
        val profile = existing
            ?: run {
                val user = userRepository.findById(userId)
                    .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }
                profileRepository.save(MercenaryProfile.create(user))
            }
        profile.update(
            isActive = request.isActive,
            regions = request.regions,
            availableDays = request.availableDays,
            availableTimes = request.availableTimes,
            positions = request.positions,
            desiredFee = request.desiredFee,
        )
        return MercenaryProfileResponse.from(profile)
    }

    fun getMyProfile(userId: Long): MercenaryProfileResponse {
        val profile = profileRepository.findByUserId(userId)
            ?: throw BusinessException(ErrorCode.MERCENARY_PROFILE_NOT_FOUND)
        return MercenaryProfileResponse.from(profile)
    }

    @Transactional
    fun createRequest(
        userId: Long,
        teamId: Long,
        payload: CreateMercenaryRequestPayload,
    ): MercenaryRequestResponse {
        requireTeamManagement(teamId, userId)
        val team = teamRepository.findById(teamId)
            .orElseThrow { BusinessException(ErrorCode.TEAM_NOT_FOUND) }

        val request = requestRepository.save(
            MercenaryRequest.create(
                team = team,
                matchId = payload.matchId,
                neededPositions = payload.neededPositions,
                neededCount = payload.neededCount,
                skillMin = payload.skillMin,
                skillMax = payload.skillMax,
                fee = payload.fee,
                regions = payload.regions,
                memo = payload.memo,
            ),
        )
        return MercenaryRequestResponse.from(request)
    }

    fun listOpenRequests(): List<MercenaryRequestResponse> =
        requestRepository.findByStatusOrderByCreatedAtDesc(MercenaryRequestStatus.OPEN)
            .map(MercenaryRequestResponse::from)

    fun getRequest(requestId: Long): MercenaryRequestResponse =
        MercenaryRequestResponse.from(findRequest(requestId))

    @Transactional
    fun closeRequest(userId: Long, requestId: Long): MercenaryRequestResponse {
        val request = findRequest(requestId)
        requireTeamManagement(request.team.id, userId)
        if (request.isOpen()) request.close()
        return MercenaryRequestResponse.from(request)
    }

    @Transactional
    fun apply(
        userId: Long,
        requestId: Long,
        body: ApplyMercenaryRequest,
    ): MercenaryApplicationResponse {
        val request = findRequest(requestId)
        if (!request.isOpen()) {
            throw BusinessException(ErrorCode.MERCENARY_REQUEST_NOT_OPEN)
        }
        if (applicationRepository.existsByRequestIdAndUserId(requestId, userId)) {
            throw BusinessException(ErrorCode.ALREADY_APPLIED_MERCENARY)
        }

        val profile = profileRepository.findByUserId(userId)
            ?: throw BusinessException(ErrorCode.MERCENARY_PROFILE_NOT_FOUND)
        if (!profile.isActive) {
            throw BusinessException(ErrorCode.INACTIVE_MERCENARY_PROFILE)
        }

        val application = applicationRepository.save(
            MercenaryApplication.create(
                request = request,
                user = profile.user,
                position = body.position,
                memo = body.memo,
            ),
        )
        return MercenaryApplicationResponse.from(application)
    }

    fun listApplications(userId: Long, requestId: Long): List<MercenaryApplicationResponse> {
        val request = findRequest(requestId)
        requireTeamManagement(request.team.id, userId)
        return applicationRepository.findByRequestIdOrderByCreatedAtAsc(requestId)
            .map(MercenaryApplicationResponse::from)
    }

    @Transactional
    fun acceptApplication(
        userId: Long,
        requestId: Long,
        targetUserId: Long,
    ): MercenaryApplicationResponse {
        val (_, application) = loadForDecision(userId, requestId, targetUserId)
        application.accept()
        return MercenaryApplicationResponse.from(application)
    }

    @Transactional
    fun rejectApplication(
        userId: Long,
        requestId: Long,
        targetUserId: Long,
    ): MercenaryApplicationResponse {
        val (_, application) = loadForDecision(userId, requestId, targetUserId)
        application.reject()
        return MercenaryApplicationResponse.from(application)
    }

    /**
     * Phase 1 추천 stub: dugout-ai 미가용 시 백엔드에서 단순 필터링.
     *  - 활성 프로필
     *  - region 또는 position 교집합이 있는 후보
     *  - rating 내림차순 정렬
     * Phase 2에서 dugout-ai 호출로 교체 (가중 스코어 + 기여도 분석).
     */
    fun recommendCandidates(requestId: Long): List<MercenaryProfileResponse> {
        val request = findRequest(requestId)
        val activeProfiles = profileRepository.findAllByIsActiveTrue()

        val matchedRegions = request.regions.toSet()
        val matchedPositions = request.neededPositions.toSet()

        return activeProfiles
            .filter { profile ->
                val regionMatch = matchedRegions.isEmpty() ||
                    profile.regions.any { it in matchedRegions }
                val positionMatch = matchedPositions.isEmpty() ||
                    profile.positions.any { it in matchedPositions }
                regionMatch && positionMatch
            }
            .sortedByDescending { it.rating }
            .map(MercenaryProfileResponse::from)
    }

    private fun loadForDecision(
        actorUserId: Long,
        requestId: Long,
        targetUserId: Long,
    ): Pair<MercenaryRequest, MercenaryApplication> {
        val request = findRequest(requestId)
        requireTeamManagement(request.team.id, actorUserId)

        val application = applicationRepository.findByRequestIdOrderByCreatedAtAsc(requestId)
            .firstOrNull { it.user.id == targetUserId }
            ?: throw BusinessException(ErrorCode.MERCENARY_APPLICATION_NOT_FOUND)
        return request to application
    }

    private fun findRequest(requestId: Long): MercenaryRequest =
        requestRepository.findById(requestId)
            .orElseThrow { BusinessException(ErrorCode.MERCENARY_REQUEST_NOT_FOUND) }

    private fun requireTeamManagement(teamId: Long, userId: Long) {
        val member = teamMemberRepository.findByTeamIdAndUserId(teamId, userId)
            ?: throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        if (!member.isActive) {
            throw BusinessException(ErrorCode.NOT_TEAM_MEMBER)
        }
        if (member.role !in MANAGEMENT_ROLES) {
            throw BusinessException(ErrorCode.TEAM_ROLE_NOT_ALLOWED)
        }
    }

    companion object {
        private val MANAGEMENT_ROLES = listOf(TeamRole.CAPTAIN, TeamRole.MANAGER)
    }
}
