package dev.opentorq.ble

import java.util.UUID

object NTorqProtocol {

    val SERVICE_UUID: UUID = UUID.fromString("5456534d-5647-5341-5342-454e544f5251")
    val RX_CHAR_UUID: UUID = UUID.fromString("00005354-0000-1000-8000-00805f9b34fb") // NOTIFY
    val TX_CHAR_UUID: UUID = UUID.fromString("00005352-0000-1000-8000-00805f9b34fb") // WRITE

    private const val XOR_KEY = 0xEA

    fun xorCrypt(data: ByteArray): ByteArray =
        ByteArray(data.size) { i -> (data[i].toInt() xor XOR_KEY).toByte() }

    fun checksum(bytes: ByteArray): Byte {
        val sum = bytes.sumOf { it.toInt() and 0xFF }
        return (255 - (sum % 256)).toByte()
    }

    fun buildFrame(opcode: Byte, payload: ByteArray = byteArrayOf()): ByteArray {
        val header = byteArrayOf(0x5A.toByte(), opcode)
        val encrypted = xorCrypt(payload)
        val forChecksum = header + encrypted
        return forChecksum + byteArrayOf(checksum(forChecksum), 0xFF.toByte())
    }

    fun parseFrame(data: ByteArray): ParsedFrame? {
        if (data.size < 4) return null
        if (data.first() != 0x5A.toByte() || data.last() != 0xFF.toByte()) return null

        val opcode = data[1]
        val encryptedPayload = data.sliceArray(2 until data.size - 2)
        val receivedChecksum = data[data.size - 2]
        val expectedChecksum = checksum(data.sliceArray(0..1) + encryptedPayload)

        if (receivedChecksum != expectedChecksum) return null

        return ParsedFrame(
            opcode = opcode.toInt() and 0xFF,
            payload = xorCrypt(encryptedPayload),
            raw = data,
        )
    }

    // Prebuilt heartbeat frame — sent periodically to keep connection alive
    val HEARTBEAT: ByteArray = buildFrame(opcode = 0xA5.toByte())
}

data class ParsedFrame(
    val opcode: Int,
    val payload: ByteArray,
    val raw: ByteArray,
)
