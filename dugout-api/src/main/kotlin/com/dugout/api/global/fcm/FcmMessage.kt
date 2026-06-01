package com.dugout.api.global.fcm

data class FcmMessage(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap(),
    val sound: String = "default",
    val badge: Int? = 1,
)
