package com.dugout.api.domain.matching.entity

import com.dugout.api.domain.team.entity.Team
import com.dugout.api.global.common.BaseEntity
import com.dugout.api.global.common.StringListConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "matching_requests")
class MatchingRequest(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,

    @Convert(converter = StringListConverter::class)
    @Column(name = "preferred_dates", length = 500, nullable = false)
    var preferredDates: List<String> = emptyList(),  // ISO 날짜 문자열 리스트

    @Column(name = "preferred_time")
    var preferredTime: LocalTime? = null,

    @Column(name = "preferred_ground_id")
    var preferredGroundId: Long? = null,

    @Column(name = "preferred_region", length = 50)
    var preferredRegion: String? = null,

    @Column(name = "rating_min")
    var ratingMin: Int? = null,

    @Column(name = "rating_max")
    var ratingMax: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "home_away", nullable = false, length = 10)
    var homeAway: HomeAway = HomeAway.ANY,

    @Column(name = "matched_team_id")
    var matchedTeamId: Long? = null,

    @Column(name = "match_date")
    var matchDate: LocalDate? = null,

    @Column(name = "match_time")
    var matchTime: LocalTime? = null,

    @Column(name = "ground_id")
    var groundId: Long? = null,

    @Column(name = "expires_at")
    var expiresAt: LocalDateTime? = null,

    @Column(name = "result_home_score")
    var resultHomeScore: Int? = null,

    @Column(name = "result_away_score")
    var resultAwayScore: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MatchingRequestStatus = MatchingRequestStatus.OPEN,
) : BaseEntity() {

    fun cancel() {
        this.status = MatchingRequestStatus.CANCELLED
    }

    fun applyAcceptedProposal(
        opponentTeamId: Long,
        matchDate: LocalDate?,
        matchTime: LocalTime?,
        groundId: Long?,
    ) {
        this.matchedTeamId = opponentTeamId
        this.matchDate = matchDate
        this.matchTime = matchTime
        this.groundId = groundId
        this.status = MatchingRequestStatus.MATCHED
    }

    fun applyResult(homeScore: Int, awayScore: Int) {
        this.resultHomeScore = homeScore
        this.resultAwayScore = awayScore
        this.status = MatchingRequestStatus.COMPLETED
    }

    fun isOpen(): Boolean = status == MatchingRequestStatus.OPEN
    fun isMatched(): Boolean = status == MatchingRequestStatus.MATCHED

    companion object {
        fun create(
            team: Team,
            preferredDates: List<String>,
            preferredTime: LocalTime? = null,
            preferredGroundId: Long? = null,
            preferredRegion: String? = null,
            ratingMin: Int? = null,
            ratingMax: Int? = null,
            homeAway: HomeAway = HomeAway.ANY,
            expiresAt: LocalDateTime? = null,
        ): MatchingRequest = MatchingRequest(
            team = team,
            preferredDates = preferredDates,
            preferredTime = preferredTime,
            preferredGroundId = preferredGroundId,
            preferredRegion = preferredRegion,
            ratingMin = ratingMin,
            ratingMax = ratingMax,
            homeAway = homeAway,
            expiresAt = expiresAt,
        )
    }
}
