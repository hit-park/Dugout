package com.dugout.api.domain.attendance.entity

enum class ReminderWindow(val hoursBefore: Long) {
    H48(48),
    H24(24),
}
