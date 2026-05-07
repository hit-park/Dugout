package com.dugout.api.domain.ground.repository

import com.dugout.api.domain.ground.entity.Ground
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface GroundRepository : JpaRepository<Ground, Long> {

    /**
     * Haversine 거리(km) 기반 반경 검색.
     *
     * `acos(min(1.0, ...))`로 부동소수 오차에 따른 acos 도메인 초과를 방지한다.
     * 표준 SQL 삼각함수만 사용 — PostgreSQL/H2 모두 동작.
     */
    @Query(
        value = """
            SELECT g FROM Ground g
            WHERE 6371.0 * acos(
                least(1.0,
                    cos(radians(:lat)) * cos(radians(g.latitude))
                        * cos(radians(g.longitude) - radians(:lng))
                        + sin(radians(:lat)) * sin(radians(g.latitude))
                )
            ) <= :radiusKm
            ORDER BY 6371.0 * acos(
                least(1.0,
                    cos(radians(:lat)) * cos(radians(g.latitude))
                        * cos(radians(g.longitude) - radians(:lng))
                        + sin(radians(:lat)) * sin(radians(g.latitude))
                )
            ) ASC
        """,
    )
    fun findWithinRadius(
        @Param("lat") lat: Double,
        @Param("lng") lng: Double,
        @Param("radiusKm") radiusKm: Double,
    ): List<Ground>

    fun findAllByOrderByNameAsc(): List<Ground>
}
