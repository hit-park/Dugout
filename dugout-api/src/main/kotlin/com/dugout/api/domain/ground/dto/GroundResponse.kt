package com.dugout.api.domain.ground.dto

import com.dugout.api.domain.ground.entity.Ground
import com.dugout.api.domain.ground.entity.GroundReview
import java.time.LocalDateTime

data class GroundResponse(
    val id: Long,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val phone: String?,
    val fieldType: String,
    val hasLights: Boolean,
    val hasScoreboard: Boolean,
    val hasDugout: Boolean,
    val capacity: Int?,
    val priceInfo: String?,
    val photos: List<String>,
    val avgRating: Double,
    val reviewCount: Int,
) {
    companion object {
        fun from(ground: Ground): GroundResponse = GroundResponse(
            id = ground.id,
            name = ground.name,
            address = ground.address,
            latitude = ground.latitude,
            longitude = ground.longitude,
            phone = ground.phone,
            fieldType = ground.fieldType.name,
            hasLights = ground.hasLights,
            hasScoreboard = ground.hasScoreboard,
            hasDugout = ground.hasDugout,
            capacity = ground.capacity,
            priceInfo = ground.priceInfo,
            photos = ground.photos,
            avgRating = ground.avgRating,
            reviewCount = ground.reviewCount,
        )
    }
}

data class GroundReviewResponse(
    val id: Long,
    val groundId: Long,
    val userId: Long,
    val userNickname: String,
    val rating: Int,
    val content: String?,
    val photos: List<String>,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(review: GroundReview): GroundReviewResponse = GroundReviewResponse(
            id = review.id,
            groundId = review.ground.id,
            userId = review.user.id,
            userNickname = review.user.nickname,
            rating = review.rating,
            content = review.content,
            photos = review.photos,
            createdAt = review.createdAt,
        )
    }
}
