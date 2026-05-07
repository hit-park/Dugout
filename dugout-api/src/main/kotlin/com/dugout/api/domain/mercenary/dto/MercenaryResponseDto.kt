package com.dugout.api.domain.mercenary.dto

import com.dugout.api.domain.mercenary.entity.MercenaryApplication
import com.dugout.api.domain.mercenary.entity.MercenaryProfile
import com.dugout.api.domain.mercenary.entity.MercenaryRequest
import java.time.LocalDateTime

data class MercenaryProfileResponse(
    val id: Long,
    val userId: Long,
    val nickname: String,
    val isActive: Boolean,
    val regions: List<String>,
    val availableDays: List<String>,
    val availableTimes: List<String>,
    val positions: List<String>,
    val desiredFee: Long?,
    val rating: Double,
    val ratingCount: Int,
    val totalGames: Int,
) {
    companion object {
        fun from(profile: MercenaryProfile): MercenaryProfileResponse = MercenaryProfileResponse(
            id = profile.id,
            userId = profile.user.id,
            nickname = profile.user.nickname,
            isActive = profile.isActive,
            regions = profile.regions,
            availableDays = profile.availableDays,
            availableTimes = profile.availableTimes,
            positions = profile.positions,
            desiredFee = profile.desiredFee,
            rating = profile.rating,
            ratingCount = profile.ratingCount,
            totalGames = profile.totalGames,
        )
    }
}

data class MercenaryRequestResponse(
    val id: Long,
    val teamId: Long,
    val teamName: String,
    val matchId: Long,
    val neededPositions: List<String>,
    val neededCount: Int,
    val skillMin: Int?,
    val skillMax: Int?,
    val fee: Long?,
    val regions: List<String>,
    val memo: String?,
    val status: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(request: MercenaryRequest): MercenaryRequestResponse = MercenaryRequestResponse(
            id = request.id,
            teamId = request.team.id,
            teamName = request.team.name,
            matchId = request.matchId,
            neededPositions = request.neededPositions,
            neededCount = request.neededCount,
            skillMin = request.skillMin,
            skillMax = request.skillMax,
            fee = request.fee,
            regions = request.regions,
            memo = request.memo,
            status = request.status.name,
            createdAt = request.createdAt,
        )
    }
}

data class MercenaryApplicationResponse(
    val id: Long,
    val requestId: Long,
    val userId: Long,
    val nickname: String,
    val position: String,
    val memo: String?,
    val status: String,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(application: MercenaryApplication): MercenaryApplicationResponse = MercenaryApplicationResponse(
            id = application.id,
            requestId = application.request.id,
            userId = application.user.id,
            nickname = application.user.nickname,
            position = application.position,
            memo = application.memo,
            status = application.status.name,
            createdAt = application.createdAt,
        )
    }
}
