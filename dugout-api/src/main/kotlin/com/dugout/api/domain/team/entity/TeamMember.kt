package com.dugout.api.domain.team.entity

import com.dugout.api.domain.user.entity.User
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
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "team_members",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["team_id", "user_id"]),
    ],
)
class TeamMember(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    val team: Team,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: TeamRole = TeamRole.MEMBER,

    @Column(name = "jersey_number")
    var jerseyNumber: Int? = null,

    @Convert(converter = StringListConverter::class)
    @Column(length = 100, nullable = false)
    var positions: List<String> = emptyList(),

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "joined_at", nullable = false, updatable = false)
    val joinedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity() {

    fun updateInfo(jerseyNumber: Int?, positions: List<String>?) {
        jerseyNumber?.let { this.jerseyNumber = it }
        positions?.let { this.positions = it }
    }

    fun updateRole(newRole: TeamRole) {
        this.role = newRole
    }

    fun deactivate() {
        this.isActive = false
    }

    companion object {
        fun create(
            team: Team,
            user: User,
            role: TeamRole = TeamRole.MEMBER,
            positions: List<String> = emptyList(),
            jerseyNumber: Int? = null,
        ): TeamMember = TeamMember(
            team = team,
            user = user,
            role = role,
            positions = positions,
            jerseyNumber = jerseyNumber,
        )
    }
}
