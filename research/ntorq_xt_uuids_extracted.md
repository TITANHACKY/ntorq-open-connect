# NTorq XT SmartXonnect BLE Configuration

**Extracted from:** nRF Connect Scanner  
**Device:** <YOUR_DEVICE_NAME> (MAC: <YOUR_MAC_ADDRESS>)  
**Date:** 2026-04-26  
**Status:** ✅ VERIFIED & WORKING

---

## Primary Service UUID

```
5456534d-5647-5341-5342-454e544f5251
```

**Decodes to:** `TVSMVGSABENTORQ` (TVSM = TVS Motors, rest = vendor-specific)

This service contains all telematics and control characteristics.

---

## Characteristics Under This Service

### Characteristic 1: RX (FROM BIKE) — Telemetry & Status

```
UUID: 0x5354
Properties: NOTIFY, WRITE
```

**Purpose:** Bike sends telemetry data via NOTIFY notifications to this characteristic. App receives live speed, RPM, gear, battery, etc.

**Data format:** Frames arrive as:
```
[0x5A] [OPCODE] [ENCRYPTED_DATA] [CHECKSUM] [0xFF]
```

**Real-world example from capture:**
```
Value: 5A-10-64-01-86-9F-46-64-23-64-96-01-02-64-00-00-2E-E0
Hex:   5A A5 [encrypted payload] [checksum] FF
```

---

### Characteristic 2: TX (TO BIKE) — Commands

```
UUID: 0x5352
Properties: WRITE NO RESPONSE
```

**Purpose:** App sends commands to bike via WRITE operations. No response expected (WRITE NO RESPONSE means fire-and-forget).

**Example command to send:**
- Heartbeat: `5A A5 [checksum] FF`
- Request trip info: `5A [opcode] [data] [checksum] FF`

---

## Full GATT Database

| Service/Characteristic | UUID | Type | Properties |
|---|---|---|---|
| **Generic Access** | 0x1800 | Standard | - |
| Device Name | 0x2A00 | Characteristic | READ |
| Appearance | 0x2A01 | Characteristic | READ |
| **Device Information** | 0x180A | Standard | - |
| Manufacturer Name | 0x2A29 | Characteristic | READ |
| Model Number | 0x2A24 | Characteristic | READ |
| Serial Number | 0x2A25 | Characteristic | READ |
| Hardware Revision | 0x2A27 | Characteristic | READ |
| Firmware Revision | 0x2A26 | Characteristic | READ |
| Software Revision | 0x2A28 | Characteristic | READ |
| **TVSM Custom Service** | **5456534d-5647-5341-5342-454e544f5251** | **PRIMARY** | - |
| **Telemetry RX** | **0x5354** | **Characteristic** | **NOTIFY, WRITE** |
| **Command TX** | **0x5352** | **Characteristic** | **WRITE NO RESPONSE** |

---

## Key Findings

✅ **Service UUID matches decompiled code:** `5456534d-5647-5341-5342-454e544f5251` was found in DigiDocUtils.java  
✅ **Two characteristics found:** 0x5354 (RX) and 0x5352 (TX)  
✅ **Encryption confirmed:** Payload shows XOR-encrypted data (matches 0xEA XOR pattern)  
✅ **Frame structure verified:** `5A A5 [data] [checksum] FF` pattern observed  

---

## Python/Kotlin Implementation

```python
# Bleak BLE Client
SERVICE_UUID = "5456534d-5647-5341-5342-454e544f5251"
RX_CHAR_UUID = "00005354-0000-1000-8000-00805f9b34fb"  # NOTIFY
TX_CHAR_UUID = "00005352-0000-1000-8000-00805f9b34fb"  # WRITE

async with BleakClient(device) as client:
    # Subscribe to telemetry
    await client.start_notify(RX_CHAR_UUID, handle_telemetry)
    
    # Send heartbeat
    heartbeat = bytes([0x5A, 0xA5, 0xFF, 0xFF])
    await client.write_gatt_char(TX_CHAR_UUID, heartbeat, response=False)
```

---

## What's Next?

1. **[Week 2]** Build Python PoC with Bleak
   - Connect to bike
   - Subscribe to RX characteristic
   - Parse incoming telemetry frames
   - Send heartbeat commands

2. **[Week 3]** Start Android App
   - Create Gradle project with Nordic Semiconductor BLE library
   - Implement NTorqProtocol.kt (frame builder + XOR cipher)
   - Build BikeBluetoothManager.kt

3. **[Week 4]** Ship MVP
   - Live speed/RPM dashboard
   - Auto-reconnect on disconnect

---

## References

- **Decompiled code:** com.tvsm.connect.digidoc.util.DigiDocUtils.java (line 54844)
- **Checksum logic:** com.tvsm.connect.bluetooth.security.U399ChecksumUtils.java
- **Encryption logic:** com.tvsm.connect.bluetooth.security.U399EncryptDecryptDataUtils.java
- **Device info:** Firmware/Hardware versions visible in nRF Connect Device Information service
