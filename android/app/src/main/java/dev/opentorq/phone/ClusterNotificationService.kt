package dev.opentorq.phone

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.opentorq.ble.BleService
import dev.opentorq.ble.ClusterWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Notification listener that intercepts SMS and messaging app notifications
 * and forwards the sender name/number to the NTorq XT cluster display.
 *
 * Requires the user to grant notification access in Settings → Notification Access.
 * The app guides the user to do this in MainActivity.
 *
 * Supported apps (matching TVS Connect behaviour):
 *   - SMS (com.android.mms, com.google.android.apps.messaging)
 *   - WhatsApp (com.whatsapp)
 *   - Telegram (org.telegram.messenger)
 */
class ClusterNotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())

    companion object {
        private val SUPPORTED_PACKAGES = setOf(
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in SUPPORTED_PACKAGES) return
        if (BleService.instance == null) return

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: return
        // Strip group name suffix "WhatsApp" etc. — keep only the contact name
        val sender = title.substringBefore(":").trim().take(16)
        if (sender.isBlank()) return

        sendSmsNotification(sender)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    // Sends SMS sender frame 8 times with 100ms gap — same pattern as TVS Connect.
    private fun sendSmsNotification(sender: String) {
        scope.launch {
            val frame = ClusterWriter.smsNotification(sender)
            repeat(8) {
                BleService.instance?.writeTx(frame)
                delay(100)
            }
        }
    }
}
