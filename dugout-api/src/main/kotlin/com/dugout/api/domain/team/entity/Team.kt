package com.dugout.api.domain.team.entity

import com.dugout.api.global.common.BaseEntity
import com.dugout.api.global.common.StringListConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "teams")
class Team(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "logo_url", length = 500)
    var logoUrl: String? = null,

    @Column(nullable = false, length = 50)
    var region: String,

    @Column(nullable = false)
    var division: Int = 4,

    @Column(name = "home_ground_id")
    var homeGroundId: Long? = null,

    @Convert(converter = StringListConverter::class)
    @Column(name = "activity_days", length = 200, nullable = false)
    var activityDays: List<String> = emptyList(),

    @Column(name = "activity_time", length = 20)
    var activityTime: String? = null,

    @Column(name = "invite_code", unique = true, length = 20)
    var inviteCode: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "lineup_mode", nullable = false, length = 20)
    var lineupMode: LineupMode = LineupMode.BALANCED,
) : BaseEntity() {

    fun update(
        name: String?,
        logoUrl: String?,
        region: String?,
        division: Int?,
        activityDays: List<String>?,
        activityTime: String?,
        lineupMode: LineupMode?,
    ) {
        name?.let { this.name = it }
        logoUrl?.let { this.logoUrl = it }
        region?.let { this.region = it }
        division?.let { this.division = it }
        activityDays?.let { this.activityDays = it }
        activityTime?.let { this.activityTime = it }
        lineupMode?.let { this.lineupMode = it }
    }

    fun updateInviteCode(code: String) {
        this.inviteCode = code
    }

    companion object {
        fun create(
            name: String,
            region: String,
            division: Int = 4,
            logoUrl: String? = null,
            activityDays: List<String> = emptyList(),
            activityTime: String? = null,
            lineupMode: LineupMode = LineupMode.BALANCED,
        ): Team = Team(
            name = name,
            region = region,
            division = division,
            logoUrl = logoUrl,
            activityDays = activityDays,
            activityTime = activityTime,
            lineupMode = lineupMode,
        )
    }
}
