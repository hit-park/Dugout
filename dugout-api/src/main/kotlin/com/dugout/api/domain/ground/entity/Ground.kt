package com.dugout.api.domain.ground.entity

import com.dugout.api.global.common.BaseEntity
import com.dugout.api.global.common.StringListConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "grounds")
class Ground(
    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 200)
    var address: String,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    @Column(length = 20)
    var phone: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false, length = 20)
    var fieldType: FieldType,

    @Column(name = "has_lights", nullable = false)
    var hasLights: Boolean = false,

    @Column(name = "has_scoreboard", nullable = false)
    var hasScoreboard: Boolean = false,

    @Column(name = "has_dugout", nullable = false)
    var hasDugout: Boolean = false,

    var capacity: Int? = null,

    @Column(name = "price_info", columnDefinition = "TEXT")
    var priceInfo: String? = null,

    @Convert(converter = StringListConverter::class)
    @Column(length = 1000, nullable = false)
    var photos: List<String> = emptyList(),

    @Column(name = "avg_rating", nullable = false)
    var avgRating: Double = 0.0,

    @Column(name = "review_count", nullable = false)
    var reviewCount: Int = 0,
) : BaseEntity() {

    fun applyReviewAggregate(newAvgRating: Double, newCount: Int) {
        this.avgRating = newAvgRating
        this.reviewCount = newCount
    }

    companion object {
        fun create(
            name: String,
            address: String,
            latitude: Double,
            longitude: Double,
            fieldType: FieldType,
            phone: String? = null,
            hasLights: Boolean = false,
            hasScoreboard: Boolean = false,
            hasDugout: Boolean = false,
            capacity: Int? = null,
            priceInfo: String? = null,
            photos: List<String> = emptyList(),
        ): Ground = Ground(
            name = name,
            address = address,
            latitude = latitude,
            longitude = longitude,
            phone = phone,
            fieldType = fieldType,
            hasLights = hasLights,
            hasScoreboard = hasScoreboard,
            hasDugout = hasDugout,
            capacity = capacity,
            priceInfo = priceInfo,
            photos = photos,
        )
    }
}
