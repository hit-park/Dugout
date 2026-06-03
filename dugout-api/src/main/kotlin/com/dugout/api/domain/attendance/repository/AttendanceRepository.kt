package com.dugout.api.domain.attendance.repository

import com.dugout.api.domain.attendance.entity.Attendance
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface AttendanceRepository : JpaRepository<Attendance, Long> {
    fun findByMatchIdAndUserId(matchId: Long, userId: Long): Attendance?
    fun findByMatchIdOrderByRespondedAtAsc(matchId: Long): List<Attendance>
    fun existsByMatchIdAndUserId(matchId: Long, userId: Long): Boolean

    @Query("SELECT a.user.id FROM Attendance a WHERE a.match.id = :matchId")
    fun findRespondedUserIds(matchId: Long): List<Long>
}
