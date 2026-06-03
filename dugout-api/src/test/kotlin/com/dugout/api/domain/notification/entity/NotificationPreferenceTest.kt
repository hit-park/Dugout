package com.dugout.api.domain.notification.entity

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalTime

class NotificationPreferenceTest {
    private fun pref() = NotificationPreference(
        userId = 1L,
        dndEnabled = true,
        dndStart = LocalTime.of(22, 0),
        dndEnd = LocalTime.of(8, 0),
    )

    @Test
    fun `자정 넘김 23시는 DnD 구간이다`() {
        assertThat(pref().isWithinDnd(LocalTime.of(23, 0))).isTrue()
    }

    @Test
    fun `새벽 2시는 DnD 구간이다`() {
        assertThat(pref().isWithinDnd(LocalTime.of(2, 0))).isTrue()
    }

    @Test
    fun `오전 9시는 DnD 구간이 아니다`() {
        assertThat(pref().isWithinDnd(LocalTime.of(9, 0))).isFalse()
    }

    @Test
    fun `종료시각 8시 정각은 DnD 밖이다`() {
        assertThat(pref().isWithinDnd(LocalTime.of(8, 0))).isFalse()
    }

    @Test
    fun `dnd 비활성화면 항상 false`() {
        val p = NotificationPreference(
            userId = 1L,
            dndEnabled = false,
            dndStart = LocalTime.of(22, 0),
            dndEnd = LocalTime.of(8, 0),
        )
        assertThat(p.isWithinDnd(LocalTime.of(23, 0))).isFalse()
    }
}
