package com.dugout.api.domain.notification.dto

import java.time.LocalTime

data class NotificationPreferenceRequest(
    val matchCreated: Boolean? = null,
    val lineupConfirmed: Boolean? = null,
    val attendanceReminder: Boolean? = null,
    val attendanceChanged: Boolean? = null,
    val dndEnabled: Boolean? = null,
    val dndStart: LocalTime? = null,
    val dndEnd: LocalTime? = null,
)
