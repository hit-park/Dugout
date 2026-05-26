package com.dugout.api.domain.notification.service

import com.dugout.api.domain.user.repository.UserRepository
import com.dugout.api.global.error.BusinessException
import com.dugout.api.global.error.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun updateFcmToken(userId: Long, token: String?) {
        val normalized = token?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized != null && !isValidFcmTokenShape(normalized)) {
            throw BusinessException(ErrorCode.NOTIFICATION_TOKEN_INVALID)
        }
        val user = userRepository.findById(userId).orElseThrow {
            BusinessException(ErrorCode.USER_NOT_FOUND)
        }
        user.fcmToken = normalized
    }

    private fun isValidFcmTokenShape(token: String): Boolean {
        return token.length in 100..300 && token.all {
            it.isLetterOrDigit() || it == '-' || it == '_' || it == ':'
        }
    }
}
