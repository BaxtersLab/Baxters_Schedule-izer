package com.baxter.schedulaizer

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room.Room
import com.baxter.schedulaizer.util.AppConstants
import com.baxter.schedulaizer.data.db.SchedulaizerDatabase
import com.baxter.schedulaizer.data.repository.EventRepository
import com.baxter.schedulaizer.data.repository.BillRepository
import com.baxter.schedulaizer.data.repository.AttachmentRepository
import android.util.Log
import com.baxter.schedulaizer.transfer.RemoteDexterLoader


class SchedulaizerApp : Application() {

    // Lazy-initialized database and repositories. Database class is defined in
    // Module D and will be available before final builds.
    val database: SchedulaizerDatabase by lazy {
        SchedulaizerDatabase.getInstance(this)
    }

    lateinit var eventRepository: EventRepository
    lateinit var billRepository: BillRepository
    lateinit var attachmentRepository: AttachmentRepository

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        createTransferChannel()
        enableNativeLibraryIfAllowed()
        // Warm up text-to-speech so reminder alerts and the agent can speak.
        try { com.baxter.schedulaizer.util.TtsSpeaker.init(this) } catch (_: Throwable) {}
        // Initialize repositories now that application context is available
        val db = database
        eventRepository = EventRepository(db.eventDao(), db.alertDao())
        billRepository = BillRepository(db.billDao(), db.alertDao())
        attachmentRepository = AttachmentRepository(db.attachmentDao())
        // Start DB-backed transfer state aggregator to reflect background transfers in UI
        try {
            com.baxter.schedulaizer.transfer.TransferStateAggregator.start(this, attachmentRepository)
        } catch (_: Throwable) {}
        // Wire repository and context provider into FileTransferManager
        try {
            com.baxter.schedulaizer.transfer.FileTransferManager.attachmentRepository = attachmentRepository
            com.baxter.schedulaizer.transfer.FileTransferManager.appContextProvider = { this }
        } catch (_: Throwable) {}
    }

    private fun enableNativeLibraryIfAllowed() {
        try {
            // Check explicit BuildConfig flag if present (use reflection to avoid failures)
            var enabledFlag = false
            try {
                val bcClass = try { Class.forName("com.baxter.schedulaizer.BuildConfig") } catch (_: Throwable) { null }
                if (bcClass != null) {
                    val f = bcClass.getDeclaredField("ENABLE_JNI")
                    f.isAccessible = true
                    enabledFlag = f.getBoolean(null)
                }
            } catch (_: Throwable) {
                // no explicit flag present; default to false
            }

            if (!enabledFlag) return

            // Avoid running in unit-test environment (e.g., Robolectric)
            if (isRunningUnitTests()) return

            // Check device ABI compatibility against known ABIs we ship
            val supportedAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")
            val deviceOk = Build.SUPPORTED_ABIS.any { supportedAbis.contains(it) }
            if (!deviceOk) return

            // All checks passed — load library in a controlled manner
            RemoteDexterLoader.loadNativeLibrary()
        } catch (t: Throwable) {
            Log.w("SchedulaizerApp", "enableNativeLibraryIfAllowed failed", t)
        }
    }

    private fun isRunningUnitTests(): Boolean {
        return try {
            // Detect Robolectric or other test runners by checking for their classes
            try {
                Class.forName("org.robolectric.Robolectric")
                return true
            } catch (_: ClassNotFoundException) {
                // not robolectric
            }
            // Additional check: common system property set in JVM test runners
            val cp = System.getProperty("java.class.path") ?: return false
            cp.contains("robolectric") || cp.contains("junit")
        } catch (_: Throwable) {
            false
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // The alerts channel is HIGH-importance (heads-up) but SILENT: AlertReceiver
            // plays the chosen alarm tone itself on the alarm stream, so letting the
            // channel also ring would double up the sound.
            val alerts = NotificationChannel(
                AppConstants.CHANNEL_ALERTS, "Reminders & Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                enableVibration(true)
            }
            val channels = listOf(
                alerts,
                NotificationChannel(AppConstants.CHANNEL_SERVICE, "Schedualizer Service", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel(AppConstants.CHANNEL_TRANSFER, "File Transfers", NotificationManager.IMPORTANCE_DEFAULT)
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannels(channels)
        }
    }

    private fun createTransferChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_TRANSFERS,
                "Background Transfers",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Shows progress of background file transfers"
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_TRANSFERS = "transfer_channel"
        lateinit var instance: SchedulaizerApp
            private set

        fun get(context: Context): SchedulaizerApp = context.applicationContext as SchedulaizerApp
    }
}
