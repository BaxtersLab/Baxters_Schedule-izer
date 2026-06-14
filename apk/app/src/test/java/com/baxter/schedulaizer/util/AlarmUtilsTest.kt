package com.baxter.schedulaizer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/** Pure logic for standalone alarms: time rollover + tone-source resolution. */
class AlarmUtilsTest {

    private fun cal(year: Int, month0: Int, day: Int, hour: Int, min: Int): Calendar {
        val c = Calendar.getInstance()
        c.set(year, month0, day, hour, min, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }

    @Test
    fun nextOccurrence_laterToday_staysToday() {
        val from = cal(2026, Calendar.JUNE, 14, 8, 0).timeInMillis
        val r = AlarmTimeUtils.nextOccurrenceMs(9, 30, from)
        val c = Calendar.getInstance().apply { timeInMillis = r }
        assertEquals(9, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, c.get(Calendar.MINUTE))
        assertEquals(14, c.get(Calendar.DAY_OF_MONTH))
        assertTrue(r > from)
    }

    @Test
    fun nextOccurrence_earlierTime_rollsToTomorrow() {
        val from = cal(2026, Calendar.JUNE, 14, 8, 0).timeInMillis
        val r = AlarmTimeUtils.nextOccurrenceMs(7, 0, from)
        val c = Calendar.getInstance().apply { timeInMillis = r }
        assertEquals(7, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, c.get(Calendar.MINUTE))
        assertEquals(15, c.get(Calendar.DAY_OF_MONTH))
        assertTrue(r > from)
    }

    @Test
    fun nextOccurrence_sameMinute_rollsForwardStrictly() {
        val from = cal(2026, Calendar.JUNE, 14, 8, 0).timeInMillis
        val r = AlarmTimeUtils.nextOccurrenceMs(8, 0, from)
        assertTrue("must be strictly in the future", r > from)
        val c = Calendar.getInstance().apply { timeInMillis = r }
        assertEquals(15, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun nextDaily_preservesClockTime_andAdvances() {
        val original = cal(2026, Calendar.JUNE, 1, 6, 45).timeInMillis
        val from = cal(2026, Calendar.JUNE, 14, 10, 0).timeInMillis
        val r = AlarmTimeUtils.nextDailyMs(original, from)
        val c = Calendar.getInstance().apply { timeInMillis = r }
        assertEquals(6, c.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, c.get(Calendar.MINUTE))
        assertTrue(r > from)
        // 6:45 already passed on the 14th at 10:00, so the next firing is the 15th.
        assertEquals(15, c.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun chooseTone_perAlertWins() {
        assertEquals("a", AlarmSoundPlayer.chooseToneUri("a", "b"))
    }

    @Test
    fun chooseTone_blankPerAlert_fallsToGlobal() {
        assertEquals("b", AlarmSoundPlayer.chooseToneUri("   ", "b"))
        assertEquals("b", AlarmSoundPlayer.chooseToneUri(null, "b"))
    }

    @Test
    fun chooseTone_bothBlank_null() {
        assertNull(AlarmSoundPlayer.chooseToneUri(null, null))
        assertNull(AlarmSoundPlayer.chooseToneUri("", "   "))
    }
}
