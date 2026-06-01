package com.dugout.api.domain.notification.service

import com.dugout.api.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TokenCleanupService(
    private val userRepository: UserRepository,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun clearInvalidTokens(invalidTokens: List<String>) {
        if (invalidTokens.isEmpty()) return
        val users = userRepository.findAllByFcmTokenIn(invalidTokens)
        users.forEach { it.fcmToken = null }
    }
}
