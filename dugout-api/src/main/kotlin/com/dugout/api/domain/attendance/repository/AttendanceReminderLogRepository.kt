package com.dugout.api.domain.attendance.repository

import com.dugout.api.domain.attendance.entity.AttendanceReminderLog
import com.dugout.api.domain.attendance.entity.ReminderWindow
import org.springframework.data.jpa.repository.JpaRepository

interface AttendanceReminderLogRepository : JpaRepository<AttendanceReminderLog, Long> {
    fun existsByMatchIdAndUserIdAndReminderWindow(
        matchId: Long,
        userId: Long,
        reminderWindow: ReminderWindow,
    ): Boolean
}
