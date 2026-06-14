package com.baxter.schedulaizer.util

import java.util.Calendar

/**
 * Pure-ish time math for standalone alarms. Everything is expressed against an
 * explicit `fromMs` so the logic is deterministic and unit-testable (the only
 * implicit input is the device's default time zone, via [Calendar]).
 */
object AlarmTimeUtils {
    const val MS_PER_DAY: Long = 86_400_000L

    /**
     * The next epoch-ms at which local [hour]:[minute] occurs strictly after
     * [fromMs]. If that time has already passed today, it rolls to tomorrow.
     */
    fun nextOccurrenceMs(hour: Int, minute: Int, fromMs: Long = System.currentTimeMillis()): Long {
        val c = Calendar.getInstance()
        c.timeInMillis = fromMs
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= fromMs) {
            c.add(Calendar.DAY_OF_MONTH, 1)
        }
        return c.timeInMillis
    }

    fun hourOf(ms: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return c.get(Calendar.HOUR_OF_DAY)
    }

    fun minuteOf(ms: Long): Int {
        val c = Calendar.getInstance()
        c.timeInMillis = ms
        return c.get(Calendar.MINUTE)
    }

    /**
     * Next daily firing after [fromMs] for an alarm whose clock time is taken from
     * [originalMs]. Re-deriving from the wall-clock time (rather than just adding
     * 24h) keeps repeats anchored to HH:mm across DST shifts and missed firings
     * (e.g. the device was powered off when the alarm was due).
     */
    fun nextDailyMs(originalMs: Long, fromMs: Long = System.currentTimeMillis()): Long =
        nextOccurrenceMs(hourOf(originalMs), minuteOf(originalMs), fromMs)
}
