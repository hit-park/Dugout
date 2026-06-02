package com.dugout.api.domain.notification.entity

import com.dugout.api.domain.notification.event.NotificationType
import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "notification_preferences")
class NotificationPreference(
    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,

    @Column(name = "match_created", nullable = false)
    var matchCreated: Boolean = true,

    @Column(name = "lineup_confirmed", nullable = false)
    var lineupConfirmed: Boolean = true,

    @Column(name = "attendance_reminder", nullable = false)
    var attendanceReminder: Boolean = true,

    @Column(name = "attendance_changed", nullable = false)
    var attendanceChanged: Boolean = true,

    @Column(name = "dnd_enabled", nullable = false)
    var dndEnabled: Boolean = true,

    @Column(name = "dnd_start", nullable = false)
    var dndStart: LocalTime = LocalTime.of(22, 0),

    @Column(name = "dnd_end", nullable = false)
    var dndEnd: LocalTime = LocalTime.of(8, 0),
) : BaseEntity() {

    fun isWithinDnd(now: LocalTime): Boolean {
        if (!dndEnabled) return false
        return if (dndStart <= dndEnd) {
            now >= dndStart && now < dndEnd
        } else {
            now >= dndStart || now < dndEnd
        }
    }

    fun isEnabled(type: NotificationType): Boolean = when (type) {
        NotificationType.MATCH_CREATED -> matchCreated
        NotificationType.LINEUP_CONFIRMED -> lineupConfirmed
        NotificationType.ATTENDANCE_REMINDER -> attendanceReminder
        NotificationType.ATTENDANCE_CHANGED -> attendanceChanged
    }

    companion object {
        fun default(userId: Long) = NotificationPreference(userId = userId)
    }
}
