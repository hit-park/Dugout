package com.dugout.api.domain.ground.service

import com.dugout.api.domain.ground.dto.CreateGroundRequest
import com.dugout.api.domain.ground.dto.CreateGroundReviewRequest
import com.dugout.api.domain.ground.dto.GroundResponse
import com.dugout.api.domain.ground.dto.GroundReviewResponse
import com.dugout.api.domain.ground.entity.Ground
import com.dugout.api.domain.ground.entity.GroundReview
import com.dugout.api.domain.ground.repository.GroundRepository
import com.dugout.api.domain.ground.repository.GroundReviewRepository
import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GroundService(
    private val groundRepository: GroundRepository,
    private val groundReviewRepository: GroundReviewRepository,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun createGround(request: CreateGroundRequest): GroundResponse {
        val ground = groundRepository.save(
            Ground.create(
                name = request.name,
                address = request.address,
                latitude = request.latitude,
                longitude = request.longitude,
                fieldType = request.fieldType,
                phone = request.phone,
                hasLights = request.hasLights,
                hasScoreboard = request.hasScoreboard,
                hasDugout = request.hasDugout,
                capacity = request.capacity,
                priceInfo = request.priceInfo,
                photos = request.photos,
            ),
        )
        return GroundResponse.from(ground)
    }

    fun searchGrounds(lat: Double?, lng: Double?, radiusKm: Double?): List<GroundResponse> {
        if (lat == null && lng == null && radiusKm == null) {
            return groundRepository.findAllByOrderByNameAsc().map(GroundResponse::from)
        }
        if (lat == null || lng == null || radiusKm == null) {
            throw BusinessException(ErrorCode.INVALID_LOCATION_QUERY)
        }
        if (radiusKm <= 0.0) {
            throw BusinessException(ErrorCode.INVALID_LOCATION_QUERY)
        }
        return groundRepository.findWithinRadius(lat, lng, radiusKm).map(GroundResponse::from)
    }

    fun getGround(groundId: Long): GroundResponse =
        GroundResponse.from(findGround(groundId))

    @Transactional
    fun createReview(
        userId: Long,
        groundId: Long,
        request: CreateGroundReviewRequest,
    ): GroundReviewResponse {
        if (request.rating !in 1..5) {
            throw BusinessException(ErrorCode.INVALID_GROUND_RATING)
        }

        val ground = findGround(groundId)

        if (groundReviewRepository.existsByGroundIdAndUserId(groundId, userId)) {
            throw BusinessException(ErrorCode.DUPLICATE_GROUND_REVIEW)
        }

        val user = userRepository.findById(userId)
            .orElseThrow { BusinessException(ErrorCode.USER_NOT_FOUND) }

        val review = groundReviewRepository.save(
            GroundReview.create(
                ground = ground,
                user = user,
                rating = request.rating,
                content = request.content,
                photos = request.photos,
            ),
        )

        refreshAggregate(ground)
        return GroundReviewResponse.from(review)
    }

    fun getReviews(groundId: Long): List<GroundReviewResponse> {
        if (!groundRepository.existsById(groundId)) {
            throw BusinessException(ErrorCode.GROUND_NOT_FOUND)
        }
        return groundReviewRepository.findByGroundIdOrderByCreatedAtDesc(groundId)
            .map(GroundReviewResponse::from)
    }

    private fun refreshAggregate(ground: Ground) {
        val avg = groundReviewRepository.avgRatingByGroundId(ground.id)
        val count = groundReviewRepository.countByGroundId(ground.id)
        ground.applyReviewAggregate(avg, count)
    }

    private fun findGround(groundId: Long): Ground =
        groundRepository.findById(groundId)
            .orElseThrow { BusinessException(ErrorCode.GROUND_NOT_FOUND) }
}
