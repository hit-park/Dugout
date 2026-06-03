package com.dugout.api.domain.notification.repository

import com.dugout.api.domain.notification.entity.NotificationPreference
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, Long> {
    fun findByUserId(userId: Long): NotificationPreference?
    fun findByUserIdIn(userIds: Collection<Long>): List<NotificationPreference>
}
