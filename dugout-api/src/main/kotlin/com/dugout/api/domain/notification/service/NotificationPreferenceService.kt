package com.dugout.api.domain.notification.service

import com.dugout.api.domain.notification.dto.NotificationPreferenceRequest
import com.dugout.api.domain.notification.dto.NotificationPreferenceResponse
import com.dugout.api.domain.notification.entity.NotificationPreference
import com.dugout.api.domain.notification.repository.NotificationPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationPreferenceService(
    private val repository: NotificationPreferenceRepository,
) {
    @Transactional
    fun getOrCreate(userId: Long): NotificationPreferenceResponse {
        val pref = repository.findByUserId(userId)
            ?: repository.save(NotificationPreference.default(userId))
        return NotificationPreferenceResponse.from(pref)
    }

    @Transactional
    fun update(userId: Long, request: NotificationPreferenceRequest): NotificationPreferenceResponse {
        val pref = repository.findByUserId(userId)
            ?: repository.save(NotificationPreference.default(userId))
        request.matchCreated?.let { pref.matchCreated = it }
        request.lineupConfirmed?.let { pref.lineupConfirmed = it }
        request.attendanceReminder?.let { pref.attendanceReminder = it }
        request.attendanceChanged?.let { pref.attendanceChanged = it }
        request.dndEnabled?.let { pref.dndEnabled = it }
        request.dndStart?.let { pref.dndStart = it }
        request.dndEnd?.let { pref.dndEnd = it }
        return NotificationPreferenceResponse.from(pref)
    }
}
