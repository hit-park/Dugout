package com.dugout.api.domain.ground.repository

import com.dugout.api.domain.ground.entity.GroundReview
import org.springframework.data.jpa.repository.JpaRepository

interface GroundReviewRepository : JpaRepository<GroundReview, Long> {
    fun findByGroundIdOrderByCreatedAtDesc(groundId: Long): List<GroundReview>
    fun existsByGroundIdAndUserId(groundId: Long, userId: Long): Boolean

    @org.springframework.data.jpa.repository.Query(
        "SELECT COALESCE(AVG(r.rating), 0.0) FROM GroundReview r WHERE r.ground.id = :groundId",
    )
    fun avgRatingByGroundId(groundId: Long): Double

    fun countByGroundId(groundId: Long): Int
}
