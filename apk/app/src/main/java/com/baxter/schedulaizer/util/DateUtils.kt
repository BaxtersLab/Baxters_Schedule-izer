package com.baxter.schedulaizer.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    const val MS_PER_DAY: Long = 86_400_000L
    const val MS_PER_HOUR: Long = 3_600_000L

    fun nowMs(): Long = System.currentTimeMillis()

    private fun calendarTodayMidnight(): Calendar {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c
    }

    fun todayStartMs(): Long = calendarTodayMidnight().timeInMillis

    fun todayEndMs(): Long = todayStartMs() + MS_PER_DAY - 1

    fun startOfWeekMs(): Long {
        val c = calendarTodayMidnight()
        // ISO week: Monday = first day
        c.firstDayOfWeek = Calendar.MONDAY
        while (c.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            c.add(Calendar.DATE, -1)
        }
        return c.timeInMillis
    }

    fun endOfWeekMs(): Long = startOfWeekMs() + (7 * MS_PER_DAY) - 1

    fun startOfMonthMs(): Long {
        val c = calendarTodayMidnight()
        c.set(Calendar.DAY_OF_MONTH, 1)
        return c.timeInMillis
    }

    fun endOfMonthMs(): Long {
        val c = calendarTodayMidnight()
        c.set(Calendar.DAY_OF_MONTH, 1)
        c.add(Calendar.MONTH, 1)
        c.add(Calendar.MILLISECOND, -1)
        return c.timeInMillis
    }

    fun formatDate(ms: Long): String {
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return fmt.format(Date(ms))
    }

    fun formatDateTime(ms: Long): String {
        val fmt = SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault())
        return fmt.format(Date(ms))
    }

    fun formatTime(ms: Long): String {
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        return fmt.format(Date(ms))
    }

    fun formatShortDate(ms: Long): String {
        val fmt = SimpleDateFormat("M/d", Locale.getDefault())
        return fmt.format(Date(ms))
    }

    fun nextDueDateMs(dueDayOfMonth: Int, intervalMonths: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance()
        target.set(Calendar.HOUR_OF_DAY, 0)
        target.set(Calendar.MINUTE, 0)
        target.set(Calendar.SECOND, 0)
        target.set(Calendar.MILLISECOND, 0)

        // set day-of-month safely
        val maxDay = target.getActualMaximum(Calendar.DAY_OF_MONTH)
        val day = if (dueDayOfMonth > maxDay) maxDay else dueDayOfMonth
        target.set(Calendar.DAY_OF_MONTH, day)

        if (target.timeInMillis >= now.timeInMillis) {
            return target.timeInMillis
        }

        // advance by intervalMonths until it's in the future
        while (target.timeInMillis < now.timeInMillis) {
            target.add(Calendar.MONTH, intervalMonths)
            val maxDay2 = target.getActualMaximum(Calendar.DAY_OF_MONTH)
            val useDay = if (dueDayOfMonth > maxDay2) maxDay2 else dueDayOfMonth
            target.set(Calendar.DAY_OF_MONTH, useDay)
            target.set(Calendar.HOUR_OF_DAY, 0)
            target.set(Calendar.MINUTE, 0)
            target.set(Calendar.SECOND, 0)
            target.set(Calendar.MILLISECOND, 0)
        }
        return target.timeInMillis
    }

    fun daysUntil(targetMs: Long): Int {
        val diff = (targetMs - nowMs())
        return (diff / MS_PER_DAY).toInt()
    }

    fun isOverdue(dueDateMs: Long): Boolean = dueDateMs < todayStartMs()

    fun isDueThisWeek(dueDateMs: Long): Boolean = dueDateMs in todayStartMs()..endOfWeekMs()

    fun isDueThisMonth(dueDateMs: Long): Boolean = dueDateMs in todayStartMs()..endOfMonthMs()

    fun epochFromParts(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.YEAR, year)
        c.set(Calendar.MONTH, month - 1)
        c.set(Calendar.DAY_OF_MONTH, day)
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, minute)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    fun parseIso(isoString: String): Long? {
        return try {
            val s = when {
                isoString.length <= 10 -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(isoString)
                else -> SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).parse(isoString)
            }
            s?.time
        } catch (e: ParseException) {
            null
        }
    }
}
