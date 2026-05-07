package com.dugout.api.domain.mercenary.repository

import com.dugout.api.domain.mercenary.entity.MercenaryApplication
import org.springframework.data.jpa.repository.JpaRepository

interface MercenaryApplicationRepository : JpaRepository<MercenaryApplication, Long> {
    fun findByRequestIdOrderByCreatedAtAsc(requestId: Long): List<MercenaryApplication>
    fun existsByRequestIdAndUserId(requestId: Long, userId: Long): Boolean
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<MercenaryApplication>
}
