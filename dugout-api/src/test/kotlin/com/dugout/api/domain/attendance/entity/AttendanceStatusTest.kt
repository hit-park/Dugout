package com.dugout.api.domain.attendance.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AttendanceStatusTest {
    @Test
    fun `참석 to 불참 은 의미있는 변경이다`() {
        assertThat(isMeaningfulAttendanceChange(AttendanceStatus.ATTEND, AttendanceStatus.ABSENT)).isTrue()
    }

    @Test
    fun `불참 to 참석 은 의미있는 변경이다`() {
        assertThat(isMeaningfulAttendanceChange(AttendanceStatus.ABSENT, AttendanceStatus.ATTEND)).isTrue()
    }

    @Test
    fun `참석 to 늦참 은 가용성 유지라 의미없다`() {
        assertThat(isMeaningfulAttendanceChange(AttendanceStatus.ATTEND, AttendanceStatus.LATE)).isFalse()
    }

    @Test
    fun `미정 to 불참 은 둘 다 불가용이라 의미없다`() {
        assertThat(isMeaningfulAttendanceChange(AttendanceStatus.MAYBE, AttendanceStatus.ABSENT)).isFalse()
    }

    @Test
    fun `참석 to 미정 은 의미있는 변경이다`() {
        assertThat(isMeaningfulAttendanceChange(AttendanceStatus.ATTEND, AttendanceStatus.MAYBE)).isTrue()
    }
}
