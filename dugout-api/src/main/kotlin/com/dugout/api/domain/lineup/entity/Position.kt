package com.dugout.api.domain.lineup.entity

/**
 * 야구 포지션 코드 (CLAUDE.md "포지션 코드" 표 기준).
 * 라인업 검증·AI 추천 호출에서 모두 이 set을 단일 출처로 사용.
 */
object Position {
    val FIELD_POSITIONS: List<String> = listOf("P", "C", "1B", "2B", "3B", "SS", "LF", "CF", "RF")
    val ALL: Set<String> = (FIELD_POSITIONS + "DH").toSet()

    fun isValid(code: String): Boolean = code in ALL
}
