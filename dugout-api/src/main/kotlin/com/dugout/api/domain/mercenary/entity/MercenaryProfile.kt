package com.dugout.api.domain.mercenary.entity

import com.dugout.api.domain.user.entity.User
import com.dugout.api.global.common.BaseEntity
import com.dugout.api.global.common.StringListConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "mercenary_profiles")
class MercenaryProfile(
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Convert(converter = StringListConverter::class)
    @Column(length = 200, nullable = false)
    var regions: List<String> = emptyList(),

    @Convert(converter = StringListConverter::class)
    @Column(name = "available_days", length = 100, nullable = false)
    var availableDays: List<String> = emptyList(),

    @Convert(converter = StringListConverter::class)
    @Column(name = "available_times", length = 100, nullable = false)
    var availableTimes: List<String> = emptyList(),

    @Convert(converter = StringListConverter::class)
    @Column(length = 100, nullable = false)
    var positions: List<String> = emptyList(),

    @Column(name = "desired_fee")
    var desiredFee: Long? = null,

    @Column(nullable = false)
    var rating: Double = 0.0,

    @Column(name = "rating_count", nullable = false)
    var ratingCount: Int = 0,

    @Column(name = "total_games", nullable = false)
    var totalGames: Int = 0,
) : BaseEntity() {

    fun update(
        isActive: Boolean?,
        regions: List<String>?,
        availableDays: List<String>?,
        availableTimes: List<String>?,
        positions: List<String>?,
        desiredFee: Long?,
    ) {
        isActive?.let { this.isActive = it }
        regions?.let { this.regions = it }
        availableDays?.let { this.availableDays = it }
        availableTimes?.let { this.availableTimes = it }
        positions?.let { this.positions = it }
        desiredFee?.let { this.desiredFee = it }
    }

    companion object {
        fun create(user: User): MercenaryProfile = MercenaryProfile(user = user)
    }
}
