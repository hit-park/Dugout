package com.dugout.api.domain.notification.event

data class MatchCreatedEvent(
    val matchId: Long,
    val teamId: Long,
    val createdBy: Long,
)
