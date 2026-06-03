package com.dugout.api.domain.notification.dto

import com.dugout.api.domain.notification.entity.NotificationPreference
import java.time.LocalTime

data class NotificationPreferenceResponse(
    val matchCreated: Boolean,
    val lineupConfirmed: Boolean,
    val attendanceReminder: Boolean,
    val attendanceChanged: Boolean,
    val dndEnabled: Boolean,
    val dndStart: LocalTime,
    val dndEnd: LocalTime,
) {
    companion object {
        fun from(p: NotificationPreference) = NotificationPreferenceResponse(
            matchCreated = p.matchCreated,
            lineupConfirmed = p.lineupConfirmed,
            attendanceReminder = p.attendanceReminder,
            attendanceChanged = p.attendanceChanged,
            dndEnabled = p.dndEnabled,
            dndStart = p.dndStart,
            dndEnd = p.dndEnd,
        )
    }
}
