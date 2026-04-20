package com.dugout.api.domain.attendance.entity

import com.dugout.api.domain.match.entity.Match
import com.dugout.api.domain.user.entity.User
import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(
    name = "attendances",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["match_id", "user_id"]),
    ],
)
class Attendance(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    val match: Match,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AttendanceStatus,

    @Column(length = 200)
    var reason: String? = null,

    @Column(name = "responded_at", nullable = false)
    var respondedAt: LocalDateTime = LocalDateTime.now(),

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_predicted", length = 20)
    var aiPredicted: AttendanceStatus? = null,

    @Column(name = "ai_confidence", precision = 3, scale = 2)
    var aiConfidence: BigDecimal? = null,
) : BaseEntity() {

    fun updateVote(newStatus: AttendanceStatus, newReason: String?) {
        this.status = newStatus
        this.reason = newReason
        this.respondedAt = LocalDateTime.now()
    }

    companion object {
        fun create(
            match: Match,
            user: User,
            status: AttendanceStatus,
            reason: String? = null,
        ): Attendance = Attendance(
            match = match,
            user = user,
            status = status,
            reason = reason,
        )
    }
}
