package com.baxter.schedulaizer.voice

import android.content.Context
import com.baxter.schedulaizer.SchedulaizerApp
import com.baxter.schedulaizer.data.db.entity.BillEntity
import com.baxter.schedulaizer.data.db.entity.EventEntity
import com.baxter.schedulaizer.util.DateUtils
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Turns a [SchedulerIntent] into an action against the repositories and returns a
 * short, natural-language reply suitable for both display and text-to-speech.
 */
class AgentDispatcher(context: Context) {
    private val app = SchedulaizerApp.get(context)

    suspend fun dispatch(intent: SchedulerIntent): String = when (intent) {
        is SchedulerIntent.CreateEvent -> {
            val id = app.eventRepository.save(
                EventEntity(title = intent.title, startMs = intent.startMs, endMs = intent.endMs)
            )
            if (id > 0) "Added event \"${intent.title}\" on ${DateUtils.formatDateTime(intent.startMs)}."
            else "Sorry, I couldn't save that event."
        }
        is SchedulerIntent.CreateBill -> {
            app.billRepository.save(
                BillEntity(name = intent.name, amountCents = intent.amountCents, dueDayOfMonth = intent.dueDay)
            )
            "Added bill \"${intent.name}\", ${formatMoney(intent.amountCents)}, due on the ${ordinal(intent.dueDay)}."
        }
        is SchedulerIntent.ListUpcoming -> buildUpcomingSummary()
        is SchedulerIntent.Unknown -> unknownHelp(intent.raw)
    }

    private suspend fun buildUpcomingSummary(): String {
        val events = app.eventRepository.upcomingEvents.first().sortedBy { it.startMs }
        val from = DateUtils.todayStartMs()
        val to = from + 30 * DateUtils.MS_PER_DAY
        val bills = app.billRepository.getBillsDueInRange(from, to).first()
            .filter { !it.isPaid }
            .sortedBy { it.nextDueMs }

        if (events.isEmpty() && bills.isEmpty()) return "You're all clear — nothing upcoming."

        val parts = mutableListOf<String>()
        if (events.isNotEmpty()) {
            val e = events.first()
            parts.add("${events.size} upcoming ${plural(events.size, "event")}, next is ${e.title} on ${DateUtils.formatDateTime(e.startMs)}")
        }
        if (bills.isNotEmpty()) {
            val b = bills.first()
            parts.add("${bills.size} ${plural(bills.size, "bill")} due, next is ${b.name} for ${formatMoney(b.amountCents)} on ${DateUtils.formatDate(b.nextDueMs)}")
        }
        return "You have " + parts.joinToString("; ") + "."
    }

    private fun unknownHelp(raw: String): String {
        val lead = if (raw.isBlank()) "I didn't catch that." else "Sorry, I didn't understand \"$raw\"."
        return "$lead Try \"what's due\", \"add bill Rent 1200 on the 1st\", or \"add event Dentist tomorrow at 2pm\"."
    }

    private fun formatMoney(cents: Long): String =
        "$" + String.format(Locale.US, "%,.2f", cents / 100.0)

    private fun plural(n: Int, word: String): String = if (n == 1) word else "${word}s"

    private fun ordinal(n: Int): String {
        val suffix = if (n in 11..13) {
            "th"
        } else when (n % 10) {
            1 -> "st"
            2 -> "nd"
            3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }
}
