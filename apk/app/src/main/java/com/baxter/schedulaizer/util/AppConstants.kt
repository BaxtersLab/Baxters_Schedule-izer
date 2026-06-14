package com.baxter.schedulaizer.util

object AppConstants {
    // Notification channels
    const val CHANNEL_ALERTS = "schedualizer_alerts"
    const val CHANNEL_SERVICE = "schedualizer_service"
    const val CHANNEL_TRANSFER = "schedualizer_transfer"

    // Intent actions
    const val ACTION_FIRE_ALERT = "com.baxter.schedulaizer.FIRE_ALERT"
    const val ACTION_SNOOZE_ALERT = "com.baxter.schedulaizer.SNOOZE_ALERT"
    const val ACTION_DISMISS_ALERT = "com.baxter.schedulaizer.DISMISS_ALERT"

    // Intent extras
    const val EXTRA_ALERT_ID = "alert_id"
    const val EXTRA_EVENT_ID = "event_id"
    const val EXTRA_BILL_ID = "bill_id"
    const val EXTRA_ALERT_TITLE = "alert_title"
    const val EXTRA_ALERT_BODY = "alert_body"

    // SharedPreferences keys and defaults
    const val PREFS_NAME = "schedualizer_prefs"
    const val PREF_HOMELAB_HOST = "homelab_host"
    const val PREF_HOMELAB_PORT = "homelab_port"
    const val PREF_BUDGET_BLASTER_ENDPOINT = "budget_blaster_endpoint"

    const val DEFAULT_HOMELAB_HOST = "192.168.1.100"
    const val DEFAULT_HOMELAB_PORT = 7891
    // Budget Blaster's receipt endpoint (POST /receipt on its reconcile server,
    // default port 8082). Budget Blaster must be configured to bind to the LAN
    // (server.bind_addr = 0.0.0.0:8082) for this to be reachable from the phone.
    const val DEFAULT_BUDGET_BLASTER_ENDPOINT = "http://192.168.1.100:8082/receipt"

    // Upload queue
    const val UPLOAD_WORKER_TAG = "file_upload_worker"
    const val MAX_UPLOAD_RETRY = 3
}
