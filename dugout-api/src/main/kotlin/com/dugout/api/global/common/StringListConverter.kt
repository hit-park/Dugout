package com.dugout.api.global.common

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

/**
 * List<String> ↔ 콤마 구분 문자열 JPA 컨버터.
 * PostgreSQL 배열 타입 대신 VARCHAR로 저장해 H2 테스트 호환성 확보.
 * - null/빈 리스트 → 빈 문자열 ""
 * - "a,b,c" → ["a", "b", "c"]
 */
@Converter
class StringListConverter : AttributeConverter<List<String>, String> {

    override fun convertToDatabaseColumn(attribute: List<String>?): String =
        attribute?.joinToString(",") ?: ""

    override fun convertToEntityAttribute(dbData: String?): List<String> =
        if (dbData.isNullOrBlank()) emptyList()
        else dbData.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
