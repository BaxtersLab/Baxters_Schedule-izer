package com.baxter.schedulaizer.voice

import java.util.Calendar
import java.util.Locale
import com.baxter.schedulaizer.util.DateUtils

/**
 * Small rule-based parser turning a typed or spoken utterance into a [SchedulerIntent].
 * Deliberately forgiving rather than clever: it recognises a few useful command shapes
 * and the agent always reads back what it understood so the user can correct mistakes.
 *
 * Recognised shapes:
 *  - "what's due" / "upcoming" / "what do I have"        -> ListUpcoming
 *  - "add bill Rent 1200 on the 1st"                     -> CreateBill
 *  - "add event Dentist tomorrow at 2pm"                 -> CreateEvent
 */
object IntentParser {

    private val BILL_PREFIXES = listOf("create bill", "add bill", "new bill")
    private val EVENT_PREFIXES = listOf("create event", "add event", "new event", "schedule")

    fun parse(utterance: String): SchedulerIntent {
        val raw = utterance.trim()
        if (raw.isEmpty()) return SchedulerIntent.Unknown(raw)
        val t = raw.lowercase(Locale.getDefault())
        return when {
            isListQuery(t) -> SchedulerIntent.ListUpcoming
            BILL_PREFIXES.any { t.startsWith(it) } -> parseBill(raw) ?: SchedulerIntent.Unknown(raw)
            EVENT_PREFIXES.any { t.startsWith(it) } -> parseEvent(raw) ?: SchedulerIntent.Unknown(raw)
            else -> SchedulerIntent.Unknown(raw)
        }
    }

    internal fun isListQuery(t: String): Boolean {
        val q = t.trim()
        return q.contains("what's due") || q.contains("whats due") || q.contains("what is due") ||
            q.contains("upcoming") || q.contains("coming up") || q.contains("what's coming") ||
            q.contains("what do i have") || q.contains("agenda") || q == "due"
    }

    // ---- Bills -------------------------------------------------------------
    // "add bill Rent 1200 on the 1st" / "create bill Netflix $15.99 day 15"
    internal fun parseBill(raw: String): SchedulerIntent.CreateBill? {
        val body = stripPrefix(raw, BILL_PREFIXES) ?: return null

        // Pull out an explicit due-day phrase first so it isn't mistaken for the amount.
        val dueMatch = DUE_DAY_REGEX.find(body)
        val dueDay = dueMatch?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 31) ?: 1
        val withoutDue = if (dueMatch != null) body.replaceRange(dueMatch.range, " ") else body

        val amountMatch = AMOUNT_REGEX.find(withoutDue) ?: return null
        val amountCents = dollarsToCents(amountMatch.groupValues[1]) ?: return null
        val withoutAmount = withoutDue.replaceRange(amountMatch.range, " ")

        val name = cleanName(stripFillerLeadIns(withoutAmount))
        if (name.isBlank()) return null
        return SchedulerIntent.CreateBill(name, amountCents, dueDay)
    }

    // ---- Events ------------------------------------------------------------
    // "add event Dentist tomorrow at 2pm" / "schedule Standup at 9:30am" / "add event Trip on 2026-07-04"
    internal fun parseEvent(raw: String): SchedulerIntent.CreateEvent? {
        val body = stripPrefix(raw, EVENT_PREFIXES) ?: return null

        var remaining = body
        val cal = Calendar.getInstance()
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        // Date: ISO "yyyy-MM-dd", or the words "today"/"tomorrow".
        var hasDate = false
        val isoMatch = ISO_DATE_REGEX.find(remaining)
        if (isoMatch != null) {
            cal.set(Calendar.YEAR, isoMatch.groupValues[1].toInt())
            cal.set(Calendar.MONTH, isoMatch.groupValues[2].toInt() - 1)
            cal.set(Calendar.DAY_OF_MONTH, isoMatch.groupValues[3].toInt())
            remaining = remaining.replaceRange(isoMatch.range, " ")
            hasDate = true
        } else if (DAY_TOMORROW.containsMatchIn(remaining)) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
            remaining = DAY_TOMORROW.replace(remaining, " ")
            hasDate = true
        } else if (DAY_TODAY.containsMatchIn(remaining)) {
            remaining = DAY_TODAY.replace(remaining, " ")
            hasDate = true
        }

        // Time: "at 2pm" / "9:30am" / "at 14:00".
        var hasTime = false
        val time = extractTime(remaining)
        if (time != null) {
            cal.set(Calendar.HOUR_OF_DAY, time.hour24)
            cal.set(Calendar.MINUTE, time.minute)
            remaining = remaining.replaceRange(time.range, " ")
            hasTime = true
        }

        // Defaults: no time -> top of the next hour; if the result is in the past and
        // no explicit date was given, roll forward to tomorrow.
        if (!hasTime) {
            cal.add(Calendar.HOUR_OF_DAY, 1)
            cal.set(Calendar.MINUTE, 0)
        }
        if (!hasDate && cal.timeInMillis < System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val title = cleanName(stripFillerLeadIns(remaining))
        if (title.isBlank()) return null
        val startMs = cal.timeInMillis
        return SchedulerIntent.CreateEvent(title, startMs, startMs + DateUtils.MS_PER_HOUR)
    }

    // ---- helpers -----------------------------------------------------------

    private data class TimeHit(val hour24: Int, val minute: Int, val range: IntRange)

    private fun extractTime(s: String): TimeHit? {
        TIME_AT.find(s)?.let { m ->
            val min = m.groupValues.getOrNull(2)?.toIntOrNull() ?: 0
            val ap = m.groupValues.getOrNull(3)?.lowercase(Locale.getDefault()) ?: ""
            val hr = to24Hour(m.groupValues[1].toInt(), ap)
            if (hr != null && min in 0..59) return TimeHit(hr, min, m.range)
        }
        TIME_COLON.find(s)?.let { m ->
            val min = m.groupValues[2].toIntOrNull() ?: 0
            val ap = m.groupValues.getOrNull(3)?.lowercase(Locale.getDefault()) ?: ""
            val hr = to24Hour(m.groupValues[1].toInt(), ap)
            if (hr != null && min in 0..59) return TimeHit(hr, min, m.range)
        }
        TIME_AMPM.find(s)?.let { m ->
            val hr = to24Hour(m.groupValues[1].toInt(), m.groupValues[2].lowercase(Locale.getDefault()))
            if (hr != null) return TimeHit(hr, 0, m.range)
        }
        return null
    }

    private fun to24Hour(hour: Int, ampm: String): Int? = when (ampm) {
        "am" -> if (hour == 12) 0 else hour.takeIf { it in 1..12 }
        "pm" -> if (hour == 12) 12 else (hour + 12).takeIf { hour in 1..11 }
        else -> hour.takeIf { it in 0..23 } // 24-hour form
    }

    private fun stripPrefix(raw: String, prefixes: List<String>): String? {
        val lower = raw.lowercase(Locale.getDefault())
        for (p in prefixes) {
            if (lower.startsWith(p)) return raw.substring(p.length).trim()
        }
        return null
    }

    internal fun dollarsToCents(s: String): Long? {
        val value = s.replace(",", "").trim().toDoubleOrNull() ?: return null
        if (value < 0) return null
        return Math.round(value * 100.0)
    }

    /** Trim leading filler words like "for", "called", "named", "on", "at". */
    private fun stripFillerLeadIns(s: String): String {
        var r = s.trim()
        val fillers = listOf("for ", "called ", "named ", "titled ", "on ", "at ", "the ")
        var changed = true
        while (changed) {
            changed = false
            val lower = r.lowercase(Locale.getDefault())
            for (f in fillers) {
                if (lower.startsWith(f)) {
                    r = r.substring(f.length).trim()
                    changed = true
                    break
                }
            }
        }
        return r
    }

    private fun cleanName(s: String): String =
        s.replace(Regex("\\s+"), " ").trim().trim('-', ',', '.', ':').trim()

    private val DUE_DAY_REGEX = Regex(
        "\\b(?:on the|on day|day|due(?: on)?(?: the)?)\\s+(\\d{1,2})(?:st|nd|rd|th)?",
        RegexOption.IGNORE_CASE
    )
    private val AMOUNT_REGEX = Regex("\\$?\\s?(\\d{1,9}(?:[.,]\\d{1,2})?)")
    private val ISO_DATE_REGEX = Regex("(\\d{4})-(\\d{2})-(\\d{2})")
    private val DAY_TOMORROW = Regex("\\btomorrow\\b", RegexOption.IGNORE_CASE)
    private val DAY_TODAY = Regex("\\btoday\\b", RegexOption.IGNORE_CASE)
    private val TIME_AT = Regex("\\bat\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE)
    private val TIME_COLON = Regex("\\b(\\d{1,2}):(\\d{2})\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE)
    private val TIME_AMPM = Regex("\\b(\\d{1,2})\\s*(am|pm)\\b", RegexOption.IGNORE_CASE)
}
