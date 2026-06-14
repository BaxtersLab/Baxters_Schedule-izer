package com.baxter.schedulaizer.transfer

import android.content.Context
import com.baxter.schedulaizer.ui.tryGetMainActivity
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf

object TransferWorkScheduler {

    fun enqueueTransfer(context: Context, attachmentId: Long) {
        val data = workDataOf(TransferWorker.KEY_ATTACHMENT_ID to attachmentId)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<TransferWorker>()
            .setInputData(data)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "transfer_$attachmentId",
                ExistingWorkPolicy.KEEP,
                request
            )
        // Trigger uploading animation if MainActivity is available
        val main = context.tryGetMainActivity()
        main?.runOnUiThread { main.setTransfersUploading(true) }
    }
}
