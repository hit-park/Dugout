package com.dugout.api.global.fcm

data class FcmBatchResult(
    val successCount: Int,
    val failureCount: Int,
    val invalidTokens: List<String>,
) {
    companion object {
        fun empty() = FcmBatchResult(0, 0, emptyList())
        fun stub() = FcmBatchResult(0, 0, emptyList())
    }
}
