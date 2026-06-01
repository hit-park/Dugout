package com.dugout.api.domain.notification.event

data class LineupConfirmedEvent(
    val lineupId: Long,
    val matchId: Long,
    val teamId: Long,
    val confirmedBy: Long,
)
