package com.dugout.api.global.fcm

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

@Configuration
class FcmConfig(
    @Value("\${fcm.enabled}") private val enabled: Boolean,
    @Value("\${fcm.credentials-path}") private val credentialsPath: String,
    @Value("\${fcm.project-id}") private val projectId: String,
) {
    private val log = LoggerFactory.getLogger(FcmConfig::class.java)

    @Bean
    fun firebaseMessaging(): FirebaseMessaging? {
        if (!enabled) {
            log.info("FCM disabled (fcm.enabled=false) — stub client active")
            return null
        }
        require(credentialsPath.isNotBlank()) { "fcm.credentials-path must be set when fcm.enabled=true" }
        require(projectId.isNotBlank()) { "fcm.project-id must be set when fcm.enabled=true" }

        val credentials = GoogleCredentials.fromStream(FileInputStream(credentialsPath))
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setProjectId(projectId)
            .build()
        val app = if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }
        log.info("FCM enabled (project=$projectId)")
        return FirebaseMessaging.getInstance(app)
    }
}
