package com.dugout.api.domain.notification.event

import com.dugout.api.domain.attendance.entity.AttendanceStatus

data class AttendanceChangedEvent(
    val matchId: Long,
    val teamId: Long,
    val actorUserId: Long,
    val previous: AttendanceStatus,
    val new: AttendanceStatus,
)
