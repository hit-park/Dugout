package com.dugout.api.domain.lineup.entity

import com.dugout.api.domain.user.entity.User
import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "lineup_entries")
class LineupEntry(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lineup_id", nullable = false)
    val lineup: Lineup,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(nullable = false, length = 10)
    var position: String,

    @Column(name = "batting_order")
    var battingOrder: Int? = null,

    @Column(name = "is_bench", nullable = false)
    var isBench: Boolean = false,
) : BaseEntity()
