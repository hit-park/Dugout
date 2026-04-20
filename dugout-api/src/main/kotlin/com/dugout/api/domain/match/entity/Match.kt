package com.dugout.api.domain.match.entity

import com.dugout.api.domain.team.entity.Team
import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
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
@Table(name = "matches")
class Match(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,

    @Column(name = "opponent_name", length = 100)
    var opponentName: String? = null,

    @Column(name = "opponent_team_id")
    var opponentTeamId: Long? = null,

    @Column(name = "ground_id")
    var groundId: Long? = null,

    @Column(name = "ground_name", length = 100)
    var groundName: String? = null,

    @Column(name = "match_date", nullable = false)
    var matchDate: LocalDate,

    @Column(name = "gather_time")
    var gatherTime: LocalTime? = null,

    @Column(name = "match_time", nullable = false)
    var matchTime: LocalTime,

    @Column(name = "vote_deadline")
    var voteDeadline: LocalDateTime? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MatchStatus = MatchStatus.SCHEDULED,

    @Column(name = "result_home")
    var resultHome: Int? = null,

    @Column(name = "result_away")
    var resultAway: Int? = null,

    @Column(columnDefinition = "TEXT")
    var memo: String? = null,

    @Column(name = "is_recurring", nullable = false)
    var isRecurring: Boolean = false,

    @Column(name = "recurring_rule", columnDefinition = "TEXT")
    var recurringRule: String? = null,
) : BaseEntity() {

    fun update(
        opponentName: String?,
        opponentTeamId: Long?,
        groundId: Long?,
        groundName: String?,
        matchDate: LocalDate?,
        gatherTime: LocalTime?,
        matchTime: LocalTime?,
        voteDeadline: LocalDateTime?,
        memo: String?,
    ) {
        opponentName?.let { this.opponentName = it }
        opponentTeamId?.let { this.opponentTeamId = it }
        groundId?.let { this.groundId = it }
        groundName?.let { this.groundName = it }
        matchDate?.let { this.matchDate = it }
        gatherTime?.let { this.gatherTime = it }
        matchTime?.let { this.matchTime = it }
        voteDeadline?.let { this.voteDeadline = it }
        memo?.let { this.memo = it }
    }

    fun cancel() {
        this.status = MatchStatus.CANCELLED
    }

    fun isVoteOpen(now: LocalDateTime = LocalDateTime.now()): Boolean {
        val deadline = voteDeadline ?: return status == MatchStatus.SCHEDULED
        return now.isBefore(deadline) && status == MatchStatus.SCHEDULED
    }

    companion object {
        fun create(
            team: Team,
            matchDate: LocalDate,
            matchTime: LocalTime,
            gatherTime: LocalTime? = null,
            opponentName: String? = null,
            opponentTeamId: Long? = null,
            groundId: Long? = null,
            groundName: String? = null,
            voteDeadline: LocalDateTime? = null,
            memo: String? = null,
        ): Match = Match(
            team = team,
            matchDate = matchDate,
            matchTime = matchTime,
            gatherTime = gatherTime,
            opponentName = opponentName,
            opponentTeamId = opponentTeamId,
            groundId = groundId,
            groundName = groundName,
            voteDeadline = voteDeadline,
            memo = memo,
        )
    }
}
