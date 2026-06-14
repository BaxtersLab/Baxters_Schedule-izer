package com.baxter.schedulaizer.transfer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class RetryTransferReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val attachmentId = intent.getLongExtra("attachment_id", -1L)
        if (attachmentId < 0) return

        // Enqueue the transfer via the scheduler
        TransferWorkScheduler.enqueueTransfer(context.applicationContext, attachmentId)
    }
}
