package com.dugout.api.domain.ground.entity

import com.dugout.api.domain.user.entity.User
import com.dugout.api.global.common.BaseEntity
import com.dugout.api.global.common.StringListConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "ground_reviews",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["ground_id", "user_id"]),
    ],
)
class GroundReview(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ground_id", nullable = false)
    val ground: Ground,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false)
    var rating: Int,

    @Column(columnDefinition = "TEXT")
    var content: String? = null,

    @Convert(converter = StringListConverter::class)
    @Column(length = 1000, nullable = false)
    var photos: List<String> = emptyList(),
) : BaseEntity() {

    fun update(rating: Int?, content: String?, photos: List<String>?) {
        rating?.let { this.rating = it }
        content?.let { this.content = it }
        photos?.let { this.photos = it }
    }

    companion object {
        fun create(
            ground: Ground,
            user: User,
            rating: Int,
            content: String? = null,
            photos: List<String> = emptyList(),
        ): GroundReview = GroundReview(
            ground = ground,
            user = user,
            rating = rating,
            content = content,
            photos = photos,
        )
    }
}
