package com.baxter.schedulaizer.transfer

import android.content.Context
import com.baxter.schedulaizer.ui.tryGetMainActivity
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.baxter.schedulaizer.data.repository.AttachmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TransferWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_ATTACHMENT_ID = "attachment_id"
    }

    private val attachmentRepository: AttachmentRepository by lazy {
        val app = applicationContext as com.baxter.schedulaizer.SchedulaizerApp
        app.attachmentRepository
    }

    private fun buildForegroundInfo(
        attachmentId: Long,
        progress: Int
    ): ForegroundInfo {
        val notification = NotificationCompat.Builder(
            applicationContext,
            com.baxter.schedulaizer.SchedulaizerApp.CHANNEL_TRANSFERS
        )
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Sending attachment")
            .setContentText("Progress: $progress%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .build()

        return ForegroundInfo(attachmentId.toInt(), notification)
    }

    private fun buildTapToOpenIntent(attachmentId: Long): PendingIntent {
        val uri = Uri.parse("schedulaizer://transfer/$attachmentId")

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        return PendingIntent.getActivity(
            applicationContext,
            attachmentId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val attachmentId = inputData.getLong(KEY_ATTACHMENT_ID, -1L)
        if (attachmentId < 0) return@withContext Result.failure(workDataOf("error" to "invalid attachment id"))

        try {
            // Notify UI (if present) that upload started
            val mainActivity = applicationContext.tryGetMainActivity()
            mainActivity?.runOnUiThread { mainActivity.setTransfersUploading(true) }

            // Promote to foreground with initial 0% progress
            setForeground(buildForegroundInfo(attachmentId, 0))

            FileTransferBackgroundRunner.run(attachmentId, attachmentRepository) { percent ->
                // Update notification progress
                setForeground(buildForegroundInfo(attachmentId, percent))
            }

            // Success notification with tap-to-open
            val tapIntent = buildTapToOpenIntent(attachmentId)
            val successNotification = NotificationCompat.Builder(
                applicationContext,
                com.baxter.schedulaizer.SchedulaizerApp.CHANNEL_TRANSFERS
            )
                .setSmallIcon(android.R.drawable.stat_sys_upload_done)
                .setContentTitle("Transfer complete")
                .setContentText("Tap to view details")
                .setAutoCancel(true)
                .setContentIntent(tapIntent)
                .build()

            NotificationManagerCompat.from(applicationContext)
                .notify((attachmentId.toInt() + 100000), successNotification)

            // Notify UI of success
            mainActivity?.runOnUiThread { mainActivity.setTransfersSuccess() }

            Result.success()
        } catch (t: Throwable) {
            // Failure notification with retry action
            val tapIntentFail = buildTapToOpenIntent(attachmentId)

            val retryIntent = Intent(applicationContext, RetryTransferReceiver::class.java).apply {
                putExtra("attachment_id", attachmentId)
            }

            val retryPendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                attachmentId.toInt(),
                retryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val failNotification = NotificationCompat.Builder(
                applicationContext,
                com.baxter.schedulaizer.SchedulaizerApp.CHANNEL_TRANSFERS
            )
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle("Transfer failed")
                .setContentText("Tap to view details or retry")
                .setAutoCancel(true)
                .setContentIntent(tapIntentFail)
                .addAction(android.R.drawable.ic_menu_rotate, "Retry", retryPendingIntent)
                .build()

            NotificationManagerCompat.from(applicationContext)
                .notify((attachmentId.toInt() + 200000), failNotification)

            // Notify UI of idle/failure
            val mainActivity = applicationContext.tryGetMainActivity()
            mainActivity?.runOnUiThread { mainActivity.setTransfersIdle() }

            Result.retry()
        }
    }
}
