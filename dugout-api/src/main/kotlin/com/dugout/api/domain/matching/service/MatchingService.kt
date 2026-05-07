package com.dugout.api.domain.matching.service

import com.dugout.api.domain.matching.dto.CreateMatchingRequestPayload
import com.dugout.api.domain.matching.dto.MatchResultPayload
import com.dugout.api.domain.matching.dto.MatchingProposalResponse
import com.dugout.api.domain.matching.dto.MatchingRequestResponse
import com.dugout.api.domain.matching.dto.ProposeMatchPayload
import com.dugout.api.domain.matching.dto.TeamRatingResponse
import com.dugout.api.domain.matching.entity.MatchingProposal
import com.dugout.api.domain.matching.entity.MatchingProposalStatus
import com.dugout.api.domain.matching.entity.MatchingRequest
import com.dugout.api.domain.matching.entity.MatchingRequestStatus
import com.dugout.api.domain.matching.entity.TeamRating
import com.dugout.api.domain.matching.repository.MatchingProposalRepository
import com.dugout.api.domain.matching.repository.MatchingRequestRepository
import com.dugout.api.domain.matching.repository.TeamRatingRepository
import com.dugout.api.domain.team.entity.Team
import com.dugout.api.domain.team.entity.TeamRole
import com.dugout.api.domain.team.repository.TeamMemberRepository
import com.dugout.api.domain.team.repository.TeamRepository
import com.dugout.api.global.ai.AiMatchingScoreRequest
import com.dugout.api.global.ai.DugoutAiClient
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.abs

@Service
@Transactional(readOnly = true)
class MatchingService(
    private val requestRepository: MatchingRequestRepository,
    private val proposalRepository: MatchingProposalRepository,
    private val teamRatingRepository: TeamRatingRepository,
    private val teamRepository: TeamRepository,
    private val teamMemberRepository: TeamMemberRepository,
    private val dugoutAiClient: DugoutAiClient,
) {

    @Transactional
    fun createRequest(
        userId: Long,
        teamId: Long,
        payload: CreateMatchingRequestPayload,
    ): MatchingRequestResponse {
        requireTeamManagement(teamId, userId)
        val team = teamRepository.findById(teamId)
            .orElseThrow { BusinessException(ErrorCode.TEAM_NOT_FOUND) }

        ensureRating(team)

        val request = requestRepository.save(
            MatchingRequest.create(
                team = team,
                preferredDates = payload.preferredDates,
                preferredTime = payload.preferredTime,
                preferredGroundId = payload.preferredGroundId,
                preferredRegion = payload.preferredRegion,
                ratingMin = payload.ratingMin,
                ratingMax = payload.ratingMax,
                homeAway = payload.homeAway,
                expiresAt = payload.expiresAt,
            ),
        )
        return MatchingRequestResponse.from(request)
    }

    fun listOpenRequests(): List<MatchingRequestResponse> =
        requestRepository.findByStatusOrderByCreatedAtDesc(MatchingRequestStatus.OPEN)
            .map(MatchingRequestResponse::from)

    fun getRequest(requestId: Long): MatchingRequestResponse =
        MatchingRequestResponse.from(findRequest(requestId))

    @Transactional
    fun cancelRequest(userId: Long, requestId: Long): MatchingRequestResponse {
        val request = findRequest(requestId)
        requireTeamManagement(request.team.id, userId)
        if (request.isOpen()) request.cancel()
        return MatchingRequestResponse.from(request)
    }

    @Transactional
    fun proposeMatch(
        userId: Long,
        requestId: Long,
        proposingTeamId: Long,
        payload: ProposeMatchPayload,
    ): MatchingProposalResponse {
        requireTeamManagement(proposingTeamId, userId)
        val request = findRequest(requestId)
        if (!request.isOpen()) {
            throw BusinessException(ErrorCode.MATCHING_REQUEST_NOT_OPEN)
        }
        if (request.team.id == proposingTeamId) {
            throw BusinessException(ErrorCode.SELF_MATCHING_NOT_ALLOWED)
        }
        if (proposalRepository.existsByRequestIdAndProposedTeamId(requestId, proposingTeamId)) {
            throw BusinessException(ErrorCode.DUPLICATE_MATCHING_PROPOSAL)
        }

        val proposingTeam = teamRepository.findById(proposingTeamId)
            .orElseThrow { BusinessException(ErrorCode.TEAM_NOT_FOUND) }
        ensureRating(proposingTeam)

        val proposal = proposalRepository.save(
            MatchingProposal.create(
                request = request,
                proposedTeam = proposingTeam,
                proposedBy = userId,
                proposedDate = payload.proposedDate,
                proposedTime = payload.proposedTime,
                proposedGroundId = payload.proposedGroundId,
                memo = payload.memo,
                matchScore = simpleScore(request.team, proposingTeam),
            ),
        )
        return MatchingProposalResponse.from(proposal)
    }

    fun listProposals(userId: Long, requestId: Long): List<MatchingProposalResponse> {
        val request = findRequest(requestId)
        requireTeamManagement(request.team.id, userId)
        return proposalRepository.findByRequestIdOrderByCreatedAtAsc(requestId)
            .map(MatchingProposalResponse::from)
    }

    @Transactional
    fun acceptProposal(userId: Long, proposalId: Long): MatchingProposalResponse {
        val proposal = findProposal(proposalId)
        val request = proposal.request
        requireTeamManagement(request.team.id, userId)
        if (!proposal.isPending()) {
            throw BusinessException(ErrorCode.MATCHING_PROPOSAL_NOT_PENDING)
        }
        if (!request.isOpen()) {
            throw BusinessException(ErrorCode.MATCHING_REQUEST_NOT_OPEN)
        }

        proposal.accept()
        request.applyAcceptedProposal(
            opponentTeamId = proposal.proposedTeam.id,
            matchDate = proposal.proposedDate,
            matchTime = proposal.proposedTime,
            groundId = proposal.proposedGroundId,
        )

        // 다른 PENDING 제안은 자동 거절 (참조 비교: BaseEntity id 충돌 회피)
        proposalRepository.findByRequestIdAndStatus(request.id, MatchingProposalStatus.PENDING)
            .filter { it !== proposal }
            .forEach { it.reject() }

        return MatchingProposalResponse.from(proposal)
    }

    @Transactional
    fun rejectProposal(userId: Long, proposalId: Long): MatchingProposalResponse {
        val proposal = findProposal(proposalId)
        requireTeamManagement(proposal.request.team.id, userId)
        if (!proposal.isPending()) {
            throw BusinessException(ErrorCode.MATCHING_PROPOSAL_NOT_PENDING)
        }
        proposal.reject()
        return MatchingProposalResponse.from(proposal)
    }

    @Transactional
    fun applyResult(
        userId: Long,
        requestId: Long,
        payload: MatchResultPayload,
    ): MatchingRequestResponse {
        val request = findRequest(requestId)
        requireTeamManagement(request.team.id, userId)
        if (!request.isMatched()) {
            throw BusinessException(ErrorCode.MATCHING_REQUEST_NOT_OPEN)
        }
        if (payload.homeScore < 0 || payload.awayScore < 0) {
            throw BusinessException(ErrorCode.INVALID_MATCH_RESULT)
        }

        val opponentTeamId = request.matchedTeamId
            ?: throw BusinessException(ErrorCode.INVALID_MATCH_RESULT)

        val homeRating = teamRatingRepository.findByTeamId(request.team.id)
            ?: throw BusinessException(ErrorCode.TEAM_RATING_NOT_FOUND)
        val awayRating = teamRatingRepository.findByTeamId(opponentTeamId)
            ?: throw BusinessException(ErrorCode.TEAM_RATING_NOT_FOUND)

        val homeResult = when {
            payload.homeScore > payload.awayScore -> 1.0
            payload.homeScore < payload.awayScore -> 0.0
            else -> 0.5
        }
        val homeOpponentElo = awayRating.eloRating
        val awayOpponentElo = homeRating.eloRating
        homeRating.applyResult(homeOpponentElo, homeResult)
        awayRating.applyResult(awayOpponentElo, 1.0 - homeResult)

        request.applyResult(payload.homeScore, payload.awayScore)
        return MatchingRequestResponse.from(request)
    }

    /**
     * 단순 ELO 차이 기반 정렬 (가중 스코어는 proposal 생성 시점에 dugout-ai로 산출).
     * 요청팀 경영진(CAPTAIN/MANAGER)만 호출 가능 — 다른 팀이 후보 풀 미리보기 차단.
     */
    fun recommendOpponents(userId: Long, requestId: Long): List<TeamRatingResponse> {
        val request = findRequest(requestId)
        requireTeamManagement(request.team.id, userId)
        val myRating = teamRatingRepository.findByTeamId(request.team.id)
            ?: throw BusinessException(ErrorCode.TEAM_RATING_NOT_FOUND)

        val all = teamRatingRepository.findAll()
        return all
            .asSequence()
            .filter { it.team.id != request.team.id }
            .filter { rating ->
                val withinMin = request.ratingMin?.let { rating.eloRating >= it } ?: true
                val withinMax = request.ratingMax?.let { rating.eloRating <= it } ?: true
                withinMin && withinMax
            }
            .sortedBy { abs(it.eloRating - myRating.eloRating) }
            .toList()
            .map(TeamRatingResponse::from)
    }

    fun getRating(teamId: Long): TeamRatingResponse {
        val rating = teamRatingRepository.findByTeamId(teamId)
            ?: throw BusinessException(ErrorCode.TEAM_RATING_NOT_FOUND)
        return TeamRatingResponse.from(rating)
    }

    @Transactional
    fun ensureRating(team: Team): TeamRating =
        teamRatingRepository.findByTeamId(team.id)
            ?: teamRatingRepository.save(TeamRating.ofTeam(team))

    /**
     * dugout-ai의 가중 스코어(40/25/20/15)를 호출.
     * 백엔드가 distance/time_overlap을 정확히 모를 때는 보수적 기본값(0/1.0) 사용.
     * AI 미가용 시 ELO 차이만으로 폴백.
     */
    private fun simpleScore(team: Team, opponent: Team): Double {
        val a = teamRatingRepository.findByTeamId(team.id) ?: return 50.0
        val b = teamRatingRepository.findByTeamId(opponent.id) ?: return 50.0
        return try {
            dugoutAiClient.computeMatchingScore(
                AiMatchingScoreRequest(
                    homeElo = a.eloRating,
                    awayElo = b.eloRating,
                    distanceKm = 0.0,
                    timeOverlapRatio = 1.0,
                    awayMannerScore = b.mannerScore,
                ),
            ).totalScore
        } catch (e: BusinessException) {
            val diff = abs(a.eloRating - b.eloRating).toDouble()
            (100.0 - (diff / 4.0)).coerceIn(0.0, 100.0)
        }
    }

    private fun findRequest(requestId: Long): MatchingRequest =
        requestRepository.findById(requestId)
            .orElseThrow { BusinessException(ErrorCode.MATCHING_REQUEST_NOT_FOUND) }

    private fun findProposal(proposalId: Long): MatchingProposal =
        proposalRepository.findById(proposalId)
            .orElseThrow { BusinessException(ErrorCode.MATCHING_PROPOSAL_NOT_FOUND) }

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
