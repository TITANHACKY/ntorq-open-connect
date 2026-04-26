package dev.opentorq.ble

import java.math.BigInteger

// NTorq XT tank capacity in litres — used for DTE estimate
private const val TANK_LITRES = 5.8f

data class Telemetry(
    val speedKmh: Int = 0,
    val rpm: Int = 0,
    val gear: Int = 0,
    val fuelPercent: Int = 0,
    val throttlePercent: Int = 0,
    val engineTempC: Int = 0,
    val odometer: Double = 0.0,
    val rideMode: Int = 0,
    val turnSignal: Int = 0,   // 0=off, 1=left, 2=right, 3=hazard
    val sideStand: Boolean = false,
    val instantMileage: Int = 0, // km/L from byte[8]
) {
    // Estimated distance to empty. Uses instantaneous mileage if non-zero,
    // otherwise falls back to NTorq XT rated economy of 47 km/L.
    val distanceToEmpty: Int get() {
        val economy = if (instantMileage > 0) instantMileage.toFloat() else 47f
        return ((fuelPercent / 100f) * TANK_LITRES * economy).toInt()
    }
}

/**
 * Byte layout for U399 (NTorq XT) 20-byte telemetry frame (post-XOR-decrypt):
 *
 * [0]        Frame header (0x5A)
 * [1]        Opcode
 * [2]        Speed km/h
 * [3][4][5]  Odometer (3-byte big-endian / 10.0)
 * [5]        Gear position (0=N, 1–6)
 * [6]        Fuel level %
 * [7]        Torque / SpeedoSwVersion
 * [8]        Mileage (when speed > 0)
 * [9]        (reserved)
 * [10]       Throttle %
 * [11]       Engine temp raw (value - 40 = °C)
 * [12]       Turn signal lamp status
 * [13]       Engine temp hex-converted
 * [16][17]   Engine RPM (2-byte big-endian)
 * [17]       Ride mode
 * [18]       Checksum
 * [19]       Frame trailer (0xFF)
 *
 * Source: com.tvsm.connect.bluetooth.sendreceive.U399DataParser (decompiled)
 */
object TelemetryParser {

    fun parse(frame: ParsedFrame): Telemetry {
        val p = frame.payload
        if (p.size < 20) return Telemetry()

        val speed           = p[2].toInt() and 0xFF
        val gear            = p[5].toInt() and 0xFF
        val fuel            = p[6].toInt() and 0xFF
        val instantMileage  = p[8].toInt() and 0xFF
        val throttle        = p[10].toInt() and 0xFF
        val engineTemp      = (p[11].toInt() and 0xFF) - 40  // raw value - 40 = °C
        val turnSignal      = p[12].toInt() and 0xFF
        val sideStand       = (p[14].toInt() and 0xFF) != 0
        val rideMode        = p[17].toInt() and 0xFF
        val rpm             = BigInteger(byteArrayOf(p[16], p[17])).toInt()
        val odometer        = BigInteger(byteArrayOf(p[3], p[4], p[5])).toDouble() / 10.0

        return Telemetry(
            speedKmh        = speed,
            rpm             = rpm.coerceAtLeast(0),
            gear            = gear,
            fuelPercent     = fuel,
            throttlePercent = throttle,
            engineTempC     = engineTemp,
            odometer        = odometer,
            rideMode        = rideMode,
            turnSignal      = turnSignal,
            sideStand       = sideStand,
            instantMileage  = instantMileage,
        )
    }
}
