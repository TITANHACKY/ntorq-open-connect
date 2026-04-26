package dev.opentorq.phone

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import dev.opentorq.ble.BleService
import dev.opentorq.ble.ClusterWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground-less background service that monitors phone call state and pushes
 * caller name to the NTorq XT cluster display.
 *
 * Sends the caller name frame repeatedly (as TVS Connect does) to ensure the
 * cluster receives it even under BLE congestion.
 */
class PhoneStateService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var telephonyManager: TelephonyManager? = null
    private var callerSendJob: Job? = null

    private val phoneStateListener = object : PhoneStateListener() {
        @Deprecated("Deprecated in API 31 but needed for minSdk 26 support")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    val display = phoneNumber?.removePrefix("+91")?.take(17) ?: "Unknown"
                    sendCallerName(display)
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    callerSendJob?.cancel()
                    // Clear the display by sending empty caller frame
                    BleService.instance?.writeTx(ClusterWriter.callerName(""))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        @Suppress("DEPRECATION")
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        @Suppress("DEPRECATION")
        telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE)
        scope.cancel()
        super.onDestroy()
    }

    // Sends caller name 8 times with 100ms gap — same pattern as TVS Connect source.
    private fun sendCallerName(name: String) {
        callerSendJob?.cancel()
        callerSendJob = scope.launch {
            val frame = ClusterWriter.callerName(name)
            repeat(8) {
                BleService.instance?.writeTx(frame)
                delay(100)
            }
        }
    }
}
