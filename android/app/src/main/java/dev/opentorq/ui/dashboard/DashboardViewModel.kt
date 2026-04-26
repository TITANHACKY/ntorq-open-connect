package dev.opentorq.ui.dashboard

import android.app.Application
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.opentorq.ble.BleService
import dev.opentorq.ble.ConnectionState
import dev.opentorq.ble.Telemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val app: Application,
) : AndroidViewModel(app) {

    val connectionState: StateFlow<ConnectionState> = BleService.connectionState
    val telemetry: StateFlow<Telemetry?> = BleService.telemetry

    val bluetoothError = MutableStateFlow<String?>(null)

    fun startService() {
        val adapter = (app.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (!adapter.isEnabled) {
            bluetoothError.value = "Bluetooth is off. Please turn on Bluetooth and try again."
            return
        }
        bluetoothError.value = null
        app.startForegroundService(Intent(app, BleService::class.java))
    }

    fun stopService() {
        app.stopService(Intent(app, BleService::class.java))
        BleService.connectionState.value = ConnectionState.Disconnected
    }
}
