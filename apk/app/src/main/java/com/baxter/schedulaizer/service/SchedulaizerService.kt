package com.baxter.schedulaizer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.baxter.schedulaizer.util.AppConstants
import com.baxter.schedulaizer.SchedulaizerApp

class SchedulaizerService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildForegroundNotification())
        // Tunnel init (RemoteDexter .so) will be handled here when available.
    }

    private fun buildForegroundNotification() =
        NotificationCompat.Builder(this, AppConstants.CHANNEL_SERVICE)
            .setContentTitle("Schedualizer Service")
            .setContentText("Service running")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service is started by system or user interaction. Keep running.
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        // Shutdown tunnel if initialized
    }
}
