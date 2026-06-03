package com.dugout.api.domain.record.entity

enum class BattingResult {
    SINGLE, DOUBLE, TRIPLE, HOME_RUN,   // 안타류
    WALK, HIT_BY_PITCH,                 // 출루 (타수 제외)
    SACRIFICE_FLY,                      // 희생플라이
    STRIKEOUT, IN_PLAY_OUT,             // 아웃
    REACHED_ON_ERROR,                   // 실책출루 (OBP/SLG 계산 시 아웃 취급)
}
