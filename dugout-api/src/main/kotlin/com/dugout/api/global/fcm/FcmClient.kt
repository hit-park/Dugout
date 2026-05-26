package com.dugout.api.global.fcm

import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class FcmClient(
    private val firebaseMessaging: FirebaseMessaging?,
) {
    private val log = LoggerFactory.getLogger(FcmClient::class.java)

    fun sendToTokens(tokens: List<String>, message: FcmMessage): FcmBatchResult {
        if (tokens.isEmpty()) return FcmBatchResult.empty()
        val messaging = firebaseMessaging ?: run {
            log.debug("FCM stub: would send '${message.title}' to ${tokens.size} tokens")
            return FcmBatchResult.stub()
        }

        val multicast = MulticastMessage.builder()
            .setNotification(
                Notification.builder()
                    .setTitle(message.title)
                    .setBody(message.body)
                    .build()
            )
            .putAllData(message.data)
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setSound(message.sound)
                            .setBadge(message.badge ?: 0)
                            .build()
                    )
                    .build()
            )
            .addAllTokens(tokens)
            .build()

        return try {
            val response = messaging.sendEachForMulticast(multicast)
            val invalid = mutableListOf<String>()
            response.responses.forEachIndexed { index, resp ->
                if (!resp.isSuccessful) {
                    val code = resp.exception?.messagingErrorCode?.name
                    log.warn("FCM send failed for token ${tokens[index].take(8)}... : $code")
                    if (code == "UNREGISTERED" || code == "INVALID_ARGUMENT") {
                        invalid += tokens[index]
                    }
                }
            }
            FcmBatchResult(
                successCount = response.successCount,
                failureCount = response.failureCount,
                invalidTokens = invalid,
            )
        } catch (e: Exception) {
            log.error("FCM multicast send error: ${e.message}", e)
            FcmBatchResult.empty()
        }
    }
}
