package dev.opentorq.ble

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.ParcelUuid
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class BleService : Service() {

    companion object {
        val connectionState: MutableStateFlow<ConnectionState> =
            MutableStateFlow(ConnectionState.Disconnected)
        val telemetry: MutableStateFlow<Telemetry?> = MutableStateFlow(null)

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "opentorq_ble"
        private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        // Singleton reference so other services (PhoneStateService, ClusterNotificationService)
        // can call writeTx() without needing a bound service connection.
        @Volatile var instance: BleService? = null
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var gatt: BluetoothGatt? = null
    private var heartbeatJob: Job? = null

    /**
     * Writes a raw (unencrypted) frame to the TX characteristic.
     * Encrypts automatically via XOR before writing.
     * Safe to call from any thread.
     */
    fun writeTx(rawFrame: ByteArray) {
        val g = gatt ?: return
        val tx = g.getService(NTorqProtocol.SERVICE_UUID)
            ?.getCharacteristic(NTorqProtocol.TX_CHAR_UUID) ?: return
        tx.value = ClusterWriter.encrypt(rawFrame)
        g.writeCharacteristic(tx)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionState.value = ConnectionState.Connected
                gatt.discoverServices()
            } else {
                connectionState.value = ConnectionState.Disconnected
                heartbeatJob?.cancel()
                dev.opentorq.phone.MobileStatusSender.stop()
                // Clear any in-progress nav or call display before dropping the link
                clearClusterDisplays(gatt)
                // Auto-reconnect after 3 seconds
                scope.launch {
                    delay(3000)
                    gatt.connect()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val rx = gatt.getService(NTorqProtocol.SERVICE_UUID)
                ?.getCharacteristic(NTorqProtocol.RX_CHAR_UUID) ?: return

            // Enable notifications on the RX characteristic
            gatt.setCharacteristicNotification(rx, true)
            val cccd = rx.getDescriptor(CCCD_UUID)
            cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(cccd)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            startHeartbeat(gatt)
            sendInitialData()
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            val frame = NTorqProtocol.parseFrame(value) ?: return
            telemetry.value = TelemetryParser.parse(frame)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val name = result.device.name ?: return
            if (!name.startsWith("TVS")) return
            stopScan()
            connectionState.value = ConnectionState.Connecting
            gatt = result.device.connectGatt(this@BleService, false, gattCallback)
        }

        override fun onScanFailed(errorCode: Int) {
            connectionState.value = ConnectionState.Disconnected
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        startForeground(NOTIFICATION_ID, buildNotification())
        scan()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
        dev.opentorq.phone.MobileStatusSender.stop()
        scope.cancel()
        gatt?.close()
        super.onDestroy()
    }

    private fun hasBlePermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) ==
                PackageManager.PERMISSION_GRANTED

    private fun scan() {
        if (!hasBlePermission()) {
            connectionState.value = ConnectionState.Disconnected
            return
        }
        connectionState.value = ConnectionState.Scanning
        val scanner = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter.bluetoothLeScanner ?: return

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // No service UUID filter — bike doesn't advertise service UUID in scan packets.
        // We filter by device name prefix "TVS" in the callback instead.
        scanner.startScan(null, settings, scanCallback)
    }

    private fun stopScan() {
        if (!hasBlePermission()) return
        val scanner = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager)
            .adapter.bluetoothLeScanner
        scanner?.stopScan(scanCallback)
    }

    private fun startHeartbeat(gatt: BluetoothGatt) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (true) {
                delay(5000)
                val tx = gatt.getService(NTorqProtocol.SERVICE_UUID)
                    ?.getCharacteristic(NTorqProtocol.TX_CHAR_UUID) ?: break
                tx.value = NTorqProtocol.HEARTBEAT
                gatt.writeCharacteristic(tx)
            }
        }
    }

    // Writes frames directly to the given GATT object (used during disconnect when
    // writeTx() would already have a null/closing gatt).
    private fun clearClusterDisplays(g: BluetoothGatt) {
        val tx = g.getService(NTorqProtocol.SERVICE_UUID)
            ?.getCharacteristic(NTorqProtocol.TX_CHAR_UUID) ?: return
        listOf(
            ClusterWriter.callerName(""),          // clears incoming call display
            ClusterWriter.navigationStop().first,  // clears nav arrow (control frame)
            ClusterWriter.navigationStop().second, // clears nav text frame
        ).forEach { raw ->
            tx.value = ClusterWriter.encrypt(raw)
            g.writeCharacteristic(tx)
        }
    }

    private fun sendInitialData() {
        scope.launch {
            delay(200)
            writeTx(ClusterWriter.riderName("Rider"))
            delay(100)
            dev.opentorq.phone.MobileStatusSender.send(this@BleService)
            dev.opentorq.phone.MobileStatusSender.start(this@BleService)
        }
    }

    private fun buildNotification(): Notification {
        val channel = NotificationChannel(CHANNEL_ID, "OpenTorq BLE", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OpenTorq")
            .setContentText("Connecting to bike...")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()
    }
}

enum class ConnectionState { Disconnected, Scanning, Connecting, Connected }
