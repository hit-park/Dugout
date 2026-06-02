package com.dugout.api.domain.attendance.entity

import com.dugout.api.global.common.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "attendance_reminder_logs",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["match_id", "user_id", "reminder_window"]),
    ],
)
class AttendanceReminderLog(
    @Column(name = "match_id", nullable = false)
    val matchId: Long,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "reminder_window", nullable = false, length = 10)
    val reminderWindow: ReminderWindow,
) : BaseEntity()
