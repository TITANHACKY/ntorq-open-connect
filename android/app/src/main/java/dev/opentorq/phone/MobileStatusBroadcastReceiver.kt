package dev.opentorq.phone

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dev.opentorq.ble.BleService
import dev.opentorq.ble.ClusterWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Sends the mobile status frame (time, battery, missed counts) to the cluster.
 * Called by BleService on connect and by a periodic coroutine every 30 seconds
 * so the cluster clock stays in sync.
 *
 * Also registers a battery change receiver so the cluster battery icon stays accurate.
 */
object MobileStatusSender {

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var periodicJob: Job? = null

    fun start(context: Context) {
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while (true) {
                send(context)
                delay(30_000)
            }
        }
    }

    fun stop() {
        periodicJob?.cancel()
    }

    fun send(context: Context) {
        val ble = BleService.instance ?: return
        val battery = getBatteryPercent(context)
        val frame = ClusterWriter.mobileStatus(
            batteryPercent = battery,
            signalLevel    = 0,   // signal level requires READ_PHONE_STATE — left as 0 for now
            missedCalls    = 0,
            missedSms      = 0,
        )
        ble.writeTx(frame)
    }

    private fun getBatteryPercent(context: Context): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else 50
    }
}
