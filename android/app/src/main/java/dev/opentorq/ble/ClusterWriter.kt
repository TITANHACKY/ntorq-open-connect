package dev.opentorq.ble

import java.util.Calendar

/**
 * Builds TX frames (phone → cluster) for the NTorq XT cluster display.
 *
 * All raw frames are XOR-encrypted via NTorqProtocol.xorCrypt() before writing.
 * Frame format: [0x5A|0x5B][DataID][payload...padded to 18 bytes][0xFF] = 20 bytes total.
 *
 * Sources: U399DataSenderToCluster.java, BluetoothUtil.java, BleNavigationSendData.java (decompiled)
 */
object ClusterWriter {

    // ── Frame header bytes ──────────────────────────────────────────────────
    private const val PREFIX_A = 0x5A.toByte()  // vehicle/nav frames
    private const val PREFIX_B = 0x5B.toByte()  // notification/status frames
    private const val SUFFIX   = 0xFF.toByte()

    // ── Data IDs (byte[1]) ──────────────────────────────────────────────────
    private const val ID_MOBILE_STATUS  = 0x4A.toByte()  // time, battery, missed calls
    private const val ID_CALLER_NAME    = 0x43.toByte()  // incoming call display
    private const val ID_SMS_NAME       = 0x53.toByte()  // SMS sender display
    private const val ID_RIDER_NAME     = 0x52.toByte()  // rider name (sent on connect)
    private const val ID_LOCATION       = 0x5C.toByte()  // current location name
    private const val ID_CUSTOM_MSG     = 0x4C.toByte()  // voice assist / custom line 1
    private const val ID_CUSTOM_MSG2    = 0x63.toByte()  // custom line 2
    private const val ID_NAV_CONTROL    = 0x49.toByte()  // navigation data (0x5A header)
    private const val ID_NAV_TEXT       = 0x75.toByte()  // nav instruction text (0x5B header)
    private const val ID_VEHICLE_DATA   = 0xF1.toByte()  // illumination / vehicle settings

    // ── Helpers ─────────────────────────────────────────────────────────────

    /** Builds a 20-byte frame: [prefix][dataId][body padded/trimmed to 17 bytes][0xFF] */
    private fun frame(prefix: Byte, dataId: Byte, body: ByteArray): ByteArray {
        val maxBody = 17
        val padded = ByteArray(maxBody)
        body.copyInto(padded, endIndex = minOf(body.size, maxBody))
        return byteArrayOf(prefix, dataId) + padded + SUFFIX
    }

    /** Encrypts the frame with XOR before it is written to the TX characteristic. */
    fun encrypt(raw: ByteArray): ByteArray = NTorqProtocol.xorCrypt(raw)

    // ── Public frame builders ────────────────────────────────────────────────

    /**
     * Caller name frame — shown on cluster during an incoming call.
     * Max 17 chars; "+91" country code stripped automatically.
     */
    fun callerName(name: String): ByteArray {
        val cleaned = name.removePrefix("+91").take(17)
        return frame(PREFIX_B, ID_CALLER_NAME, cleaned.toByteArray(Charsets.UTF_8))
    }

    /**
     * SMS notification frame — shows sender name/number on cluster.
     * Max 16 chars; "+91" stripped.
     */
    fun smsNotification(sender: String): ByteArray {
        val cleaned = sender.removePrefix("+91").take(16)
        return frame(PREFIX_B, ID_SMS_NAME, cleaned.toByteArray(Charsets.UTF_8))
    }

    /**
     * Rider name frame — sent once on connect.
     * Max 17 chars.
     */
    fun riderName(name: String): ByteArray {
        val trimmed = name.take(17)
        return frame(PREFIX_B, ID_RIDER_NAME, trimmed.toByteArray(Charsets.UTF_8))
    }

    /**
     * Current location name frame — sent after GPS fix on connect.
     * Max 17 chars.
     */
    fun locationName(name: String): ByteArray {
        val trimmed = name.take(17)
        return frame(PREFIX_B, ID_LOCATION, trimmed.toByteArray(Charsets.UTF_8))
    }

    /**
     * Mobile status frame — sends phone time, battery %, missed calls/SMS to cluster.
     * Signal level 0–4, battery 0–9 (bucketed). Sent on connect and periodically.
     */
    fun mobileStatus(
        batteryPercent: Int,
        signalLevel: Int,        // 0–4
        missedCalls: Int,
        missedSms: Int,
        voiceAssistState: Int = 0,
        crashAlert: Boolean = false,
    ): ByteArray {
        val cal = Calendar.getInstance()
        val hour12 = cal.get(Calendar.HOUR).let { if (it == 0) 12 else it }
        val amPm   = cal.get(Calendar.AM_PM)   // 0=AM, 1=PM
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val day    = cal.get(Calendar.DAY_OF_MONTH)
        val month  = cal.get(Calendar.MONTH) + 1
        val year   = cal.get(Calendar.YEAR) % 100

        val batteryBucket = batteryBucket(batteryPercent)
        val combinedByte  = (signalLevel.coerceIn(0, 4) * 10 + batteryBucket).toByte()

        val body = byteArrayOf(
            combinedByte,
            0x78.toByte(),         // default overspeed limit (120 km/h)
            0x00,
            0x02,
            hour12.toByte(),
            minute.toByte(),
            second.toByte(),
            amPm.toByte(),
            missedCalls.coerceIn(0, 255).toByte(),
            0x00,
            day.toByte(), month.toByte(), year.toByte(),
            missedSms.coerceIn(0, 255).toByte(),
            voiceAssistState.toByte(),
            if (crashAlert) 0x01 else 0x00,
            0x00,
        )
        return frame(PREFIX_B, ID_MOBILE_STATUS, body)
    }

    /**
     * Navigation control frame — distance to maneuver, ETA, pictogram, total distance.
     * Pair: first = nav control frame (0x5A), second = instruction text frame (0x5B).
     */
    fun navigationFrames(
        distanceToManeuverM: Int,
        totalDistanceM: Int,
        etaSeconds: Long,
        pictogramId: Int,       // use ClusterPictogram constants
        instruction: String,
    ): Pair<ByteArray, ByteArray> {
        val dist2 = distanceToManeuverM.toUint16BE()
        val time2 = (etaSeconds / 60).toInt().toUint16BE()
        val dist3 = totalDistanceM.toUint24BE()
        val picto = pictogramId.coerceIn(-1, 127).toByte()
        val multiLine: Byte = if (instruction.length > 17) 2 else 1

        val navBody = dist2 + time2 + dist3 + byteArrayOf(picto, multiLine, 0, 0, 0, 0, 0, 0, 0, 0)
        val navFrame = byteArrayOf(PREFIX_A, ID_NAV_CONTROL) + navBody.take(17).toByteArray() + SUFFIX

        val textFrame = frame(PREFIX_B, ID_NAV_TEXT, instruction.take(17).toByteArray(Charsets.UTF_8))
        return navFrame to textFrame
    }

    /**
     * Stop navigation — sends pictogram -1 which clears the nav display.
     */
    fun navigationStop(): Pair<ByteArray, ByteArray> =
        navigationFrames(0, 0, 0, -1, "")

    // ── Internal helpers ────────────────────────────────────────────────────

    /** Maps battery percent to a 0–9 display bucket (matching TVS Connect). */
    private fun batteryBucket(percent: Int): Int = when {
        percent >= 90 -> 9
        percent >= 80 -> 8
        percent >= 70 -> 7
        percent >= 60 -> 6
        percent >= 50 -> 5
        percent >= 40 -> 4
        percent >= 30 -> 3
        percent >= 20 -> 2
        percent >= 10 -> 1
        else          -> 0
    }

    private fun Int.toUint16BE(): ByteArray {
        val v = coerceIn(0, 0xFFFF)
        return byteArrayOf((v shr 8).toByte(), v.toByte())
    }

    private fun Int.toUint24BE(): ByteArray {
        val v = coerceIn(0, 0xFFFFFF)
        return byteArrayOf((v shr 16).toByte(), (v shr 8).toByte(), v.toByte())
    }
}

/** Cluster display pictogram codes (mapped from provider IDs in BleNavigationSendData). */
object ClusterPictogram {
    const val STOP         = -1
    const val STRAIGHT     =  0
    const val SLIGHT_RIGHT =  1
    const val TURN_RIGHT   =  2
    const val SHARP_RIGHT  = 10
    const val UTURN_RIGHT  = 11
    const val TURN_LEFT    = 12
    const val SLIGHT_LEFT  =  9
    const val UTURN_LEFT   = 57
    const val ROUNDABOUT   = 22
    const val EXIT_RIGHT   = 62
    const val EXIT_LEFT    = 63
    const val KEEP_RIGHT   = 14
    const val KEEP_LEFT    = 23
    const val UTURN        = 19
    const val ARRIVED      =  0   // use with distanceToManeuver=0
}
