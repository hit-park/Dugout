package com.dugout.api.domain.mercenary.repository

import com.dugout.api.domain.mercenary.entity.MercenaryProfile
import org.springframework.data.jpa.repository.JpaRepository

interface MercenaryProfileRepository : JpaRepository<MercenaryProfile, Long> {
    fun findByUserId(userId: Long): MercenaryProfile?
    fun findAllByIsActiveTrue(): List<MercenaryProfile>
}
