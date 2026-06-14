package com.baxter.schedulaizer.voice

sealed class SchedulerIntent {
    data class CreateEvent(val title: String, val startMs: Long, val endMs: Long) : SchedulerIntent()
    data class CreateBill(val name: String, val amountCents: Long, val dueDay: Int) : SchedulerIntent()
    object ListUpcoming : SchedulerIntent()
    data class Unknown(val raw: String) : SchedulerIntent()
}
