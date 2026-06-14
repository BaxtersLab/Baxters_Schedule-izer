package com.baxter.schedulaizer.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntentParserTest {

    @Test
    fun listQueries_classified_as_listUpcoming() {
        assertTrue(IntentParser.parse("what's due") is SchedulerIntent.ListUpcoming)
        assertTrue(IntentParser.parse("What do I have") is SchedulerIntent.ListUpcoming)
        assertTrue(IntentParser.parse("anything upcoming?") is SchedulerIntent.ListUpcoming)
    }

    @Test
    fun bill_with_amount_and_due_day() {
        val intent = IntentParser.parse("add bill Rent 1200 on the 1st")
        assertTrue(intent is SchedulerIntent.CreateBill)
        intent as SchedulerIntent.CreateBill
        assertEquals("Rent", intent.name)
        assertEquals(120000L, intent.amountCents)
        assertEquals(1, intent.dueDay)
    }

    @Test
    fun bill_with_dollar_sign_and_cents() {
        val intent = IntentParser.parse("create bill Netflix \$15.99 day 15")
        assertTrue(intent is SchedulerIntent.CreateBill)
        intent as SchedulerIntent.CreateBill
        assertEquals("Netflix", intent.name)
        assertEquals(1599L, intent.amountCents)
        assertEquals(15, intent.dueDay)
    }

    @Test
    fun bill_without_due_day_defaults_to_first() {
        val intent = IntentParser.parse("add bill Gym 25") as SchedulerIntent.CreateBill
        assertEquals("Gym", intent.name)
        assertEquals(2500L, intent.amountCents)
        assertEquals(1, intent.dueDay)
    }

    @Test
    fun bill_without_amount_is_unknown() {
        assertTrue(IntentParser.parse("add bill Rent on the 1st") is SchedulerIntent.Unknown)
    }

    @Test
    fun event_with_day_and_time_extracts_title() {
        val intent = IntentParser.parse("add event Dentist tomorrow at 2pm")
        assertTrue(intent is SchedulerIntent.CreateEvent)
        intent as SchedulerIntent.CreateEvent
        assertEquals("Dentist", intent.title)
        assertTrue(intent.endMs > intent.startMs)
    }

    @Test
    fun event_with_schedule_prefix() {
        val intent = IntentParser.parse("schedule Standup at 9:30am") as SchedulerIntent.CreateEvent
        assertEquals("Standup", intent.title)
    }

    @Test
    fun unrecognized_is_unknown() {
        assertTrue(IntentParser.parse("hello there") is SchedulerIntent.Unknown)
        assertTrue(IntentParser.parse("") is SchedulerIntent.Unknown)
    }

    @Test
    fun dollars_to_cents_parsing() {
        assertEquals(120000L, IntentParser.dollarsToCents("1200"))
        assertEquals(1599L, IntentParser.dollarsToCents("15.99"))
        assertEquals(120050L, IntentParser.dollarsToCents("1,200.50"))
        assertNull(IntentParser.dollarsToCents("abc"))
    }
}
